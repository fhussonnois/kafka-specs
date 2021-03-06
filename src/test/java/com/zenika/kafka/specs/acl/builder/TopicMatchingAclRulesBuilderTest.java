/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zenika.kafka.specs.acl.builder;

import com.zenika.kafka.specs.acl.AclGroupPolicy;
import com.zenika.kafka.specs.acl.AclGroupPolicyBuilder;
import com.zenika.kafka.specs.acl.AclOperationPolicy;
import com.zenika.kafka.specs.acl.AclRule;
import com.zenika.kafka.specs.acl.AclUserPolicy;
import com.zenika.kafka.specs.acl.AclUserPolicyBuilder;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class TopicMatchingAclRulesBuilderTest {


    static final List<AclGroupPolicy> EMPTY_GROUP = Collections.emptyList();

    static final String TOPIC_REGEX         = "/topic-.*/";
    static final String TOPIC_TEST_A        = "topic-test-a";
    static final String TOPIC_TEST_B        = "topic-test-b";
    static final String USER_TYPE           = "User:";
    static final String SIMPLE_USER         = "SimpleUser";
    static final String WILDCARD            = "*";
    static final String TOPIC_WITH_WILDCARD = "topic-*";
    static final String SIMPLE_GROUP        = "SimpleGroup";

    private TopicMatchingAclRulesBuilder builder;

    private Supplier<Collection<TopicListing>> getSupplier() {
        return () -> Arrays.asList(
                new TopicListing(TOPIC_TEST_A, false),
                new TopicListing(TOPIC_TEST_B, false)
        );
    }

    @BeforeEach
    public void setUp() {
        this.builder = new TopicMatchingAclRulesBuilder();
        this.builder.setListTopics(CompletableFuture.supplyAsync(getSupplier()));
    }

    @Test
    public void shouldBuildAclRulesGivenUserWithRegexPermissionAndNoGroup() {

        AclUserPolicy user = AclUserPolicyBuilder.newBuilder()
                .principal(USER_TYPE + SIMPLE_USER)
                .addPermission(TOPIC_REGEX, PatternType.MATCH, ResourceType.TOPIC, Collections.singleton(new AclOperationPolicy(AclOperation.CREATE)))
                .build();
        Collection<AclRule> rules = this.builder.toAclRules(EMPTY_GROUP, user);

        assertEquals(2, rules.size());

        String[] topics = new String[]{TOPIC_TEST_A, TOPIC_TEST_B};
        int i = 0;
        for (AclRule rule : rules) {
            assertEquals(WILDCARD, rule.host());
            assertEquals(AclOperation.CREATE, rule.operation());
            assertEquals(USER_TYPE +  SIMPLE_USER, rule.principal());
            assertEquals(SIMPLE_USER, rule.principalName());
            assertEquals(topics[i], rule.resourcePattern());
            assertEquals(ResourceType.TOPIC, rule.resourceType());
            assertEquals(PatternType.LITERAL, rule.patternType());
            i++;
        }
    }

    @Test
    public void shouldBuildAclRulesGivenUserAndGroupWithRegexPermission() {

        AclGroupPolicy group = AclGroupPolicyBuilder.newBuilder()
                .withName(SIMPLE_GROUP)
                .allow(new AclOperationPolicy(AclOperation.CREATE))
                .withPattern(TOPIC_REGEX)
                .withPatternType(PatternType.MATCH)
                .onResourceType(ResourceType.TOPIC)
                .build();

        AclUserPolicy user = AclUserPolicyBuilder.newBuilder()
                .principal(USER_TYPE + SIMPLE_USER)
                .groups(Collections.singleton(SIMPLE_GROUP))
                .build();
        Collection<AclRule> rules = this.builder.toAclRules(Collections.singleton(group), user);

        assertEquals(2, rules.size());

        String[] topics = new String[]{TOPIC_TEST_A, TOPIC_TEST_B};
        int i = 0;
        for (AclRule rule : rules) {
            assertEquals(WILDCARD, rule.host());
            assertEquals(AclOperation.CREATE, rule.operation());
            assertEquals(USER_TYPE +  SIMPLE_USER, rule.principal());
            assertEquals(SIMPLE_USER, rule.principalName());
            assertEquals(topics[i], rule.resourcePattern());
            assertEquals(ResourceType.TOPIC, rule.resourceType());
            assertEquals(PatternType.LITERAL, rule.patternType());
            i++;
        }
    }
}