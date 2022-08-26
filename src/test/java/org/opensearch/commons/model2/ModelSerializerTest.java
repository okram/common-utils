/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.commons.model2;


import org.junit.Test;
import org.opensearch.common.io.stream.BytesStreamInput;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.commons.model2.model.Action;
import org.opensearch.commons.model2.model.Query;
import org.opensearch.commons.model2.model.Script;
import org.opensearch.commons.model2.model.Throttle;

import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class ModelSerializerTest {

    @Test
    public void testWriter() throws Exception {
        final Query query1 = new Query("one", "two", "three", List.of("a", "b", "c"));
        final Query query2 = ModelTest.serializeDeserialize(query1);
        assertEquals("one", query2.id);
        assertEquals("two", query2.name);
        assertEquals("three", query2.query);
        assertEquals(List.of("a", "b", "c"), query2.tags);
        assertEquals(query1, query2);
    }

    @Test
    public void testEnumAccess() throws Exception {
        final Throttle throttle1 = new Throttle(10, ChronoUnit.SECONDS);
        final Throttle throttle2 = ModelTest.serializeDeserialize(throttle1);
        assertEquals(throttle1, throttle2);
        assertEquals(10, throttle2.value);
        assertEquals(ChronoUnit.SECONDS, throttle2.unit);
    }

    @Test
    public void testNestedModels1() throws Exception {
        final Action.ExecutionScope.PerScope scope1 = new Action.ExecutionScope.PerScope(List.of("a", "b", "c"));
        final Action.ExecutionScope.PerScope scope2 = ModelTest.serializeDeserialize(scope1);
        assertEquals(scope1, scope2);
        assertEquals(scope1.actionable_alerts, scope2.actionable_alerts);
        assertEquals(scope1.actionable_alerts, scope2.actionable_alerts);
    }

    @Test
    public void testNestedModels2() throws Exception {
        final Action.ExecutionScope scope1 = new Action.ExecutionScope(new Action.ExecutionScope.PerScope(List.of("a", "b", "c")));
        final Action.ExecutionScope scope2 = ModelTest.serializeDeserialize(scope1);
        assertEquals(scope1, scope2);
        assertEquals(scope1.per_alert, scope2.per_alert);
        assertEquals(scope1.per_alert.actionable_alerts, scope2.per_alert.actionable_alerts);
    }

    @Test
    public void testNestedModels3() throws Exception {
        final Action.ExecutionPolicy scope1 = new Action.ExecutionPolicy(new Action.ExecutionScope(new Action.ExecutionScope.PerScope(List.of("e", "f", "g"))));
        final Action.ExecutionPolicy scope2 = ModelTest.serializeDeserialize(scope1);
        assertEquals(scope1, scope2);
        assertEquals(scope1.action_execution_scope, scope2.action_execution_scope);
        assertEquals(scope1.action_execution_scope.per_alert, scope2.action_execution_scope.per_alert);
    }

    @Test
    public void testNestedModels4() throws Exception {
        final Action scope1 = new Action("123", "an_action", "a_destination", new Script("here", "a"), new Script("here2", "b"), false, null, new Action.ExecutionPolicy(new Action.ExecutionScope(new Action.ExecutionScope.PerScope(List.of("e", "f", "g")))));
        final Action scope2 = ModelTest.serializeDeserialize(scope1);
        assertEquals(scope1, scope2);
        assertEquals(scope1.action_execution_policy, scope2.action_execution_policy);
        assertEquals(scope1.action_execution_policy.action_execution_scope, scope2.action_execution_policy.action_execution_scope);
        assertEquals(scope1.action_execution_policy.action_execution_scope.per_alert, scope2.action_execution_policy.action_execution_scope.per_alert);
    }

    @Test
    public void testXBuilder() throws Exception {
        Query query = new Query("one", "two", "three", List.of("a", "b", "c"));
        final BytesStreamOutput output = new BytesStreamOutput();
        final XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON, output);
        final BytesStreamInput input = new BytesStreamInput(output.bytes().toBytesRef().bytes);
        //
    }
}