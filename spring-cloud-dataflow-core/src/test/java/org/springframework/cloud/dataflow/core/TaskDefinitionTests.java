/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Risberg
 * @author Glenn Renfro
 */
public class TaskDefinitionTests {

	@Test
	public void testDefinition() {
		TaskDefinition definition = new TaskDefinition("test", "timestamp");
		assertEquals("test", definition.getName());
		assertEquals("timestamp", definition.getDslText());
		assertEquals("timestamp", definition.getRegisteredAppName());
		assertEquals(1, definition.getProperties().size());
		assertEquals("test", definition.getProperties().get("spring.cloud.task.name"));

		TaskDefinition composedDef = new TaskDefinition("composed", "foo && bar");
		assertEquals("composed", composedDef.getName());
		assertEquals("foo && bar", composedDef.getDslText());
		assertEquals("composed", composedDef.getRegisteredAppName());
		assertEquals(1, composedDef.getProperties().size());
		assertEquals("composed", composedDef.getProperties().get("spring.cloud.task.name"));
	}

	@Test
	public void testPackageProtectedConstructor() {
		TaskDefinition definition = new TaskDefinition("timestamp", "label",
				Collections.singletonMap("spring.cloud.task.name", "label"));
		assertEquals("label", definition.getName());
		assertEquals("timestamp", definition.getRegisteredAppName());
		assertEquals(1, definition.getProperties().size());
		assertEquals("label", definition.getProperties().get("spring.cloud.task.name"));
	}

	@Test
	public void testBuilder() {
		TaskDefinition definition = new TaskDefinition.TaskDefinitionBuilder()
				.from(new TaskDefinition("test", "timestamp"))
				.build();
		assertEquals("test", definition.getName());
		assertEquals("timestamp", definition.getRegisteredAppName());
		assertEquals(1, definition.getProperties().size());
		assertEquals("test", definition.getProperties().get("spring.cloud.task.name"));
	}

	@Test
	public void testEquality() {
		TaskDefinition definitionOne = new TaskDefinition("test", "timestamp");
		TaskDefinition definitionTwo = new TaskDefinition("test", "timestamp");

		assertTrue(definitionOne.equals(definitionTwo), "TaskDefinitions were expected to be equal.");
		assertTrue(definitionOne.equals(definitionOne), "TaskDefinitions were expected to be equal.");

	}

	@Test
	public void testInequality() {
		TaskDefinition definitionOne = new TaskDefinition("test", "timestamp");
		TaskDefinition definitionFoo = new TaskDefinition("test", "foo");

		assertFalse(definitionOne.equals(definitionFoo), "TaskDefinitions were not expected to be equal.");
		assertFalse(definitionOne.equals(null), "TaskDefinitions were not expected to be equal.");
		assertFalse(definitionOne.equals("HI"), "TaskDefinitions were not expected to be equal.");
	}
	@Test
	public void testHashCode() {
		TaskDefinition definitionOne = new TaskDefinition("test", "timestamp");
		TaskDefinition definitionTwo = new TaskDefinition("test", "timestamp");
		TaskDefinition definitionFoo = new TaskDefinition("test", "foo");

		assertTrue(definitionOne.hashCode() == definitionTwo.hashCode(), "TaskDefinitions' hashcodes were expected to be equal.");
		assertFalse(definitionOne.hashCode() == definitionFoo.hashCode(), "TaskDefinitions' hashcodes were not expected to be equal.");
	}

	@Test
	public void testDefinitionWithArguments() {
		TaskDefinition definition = new TaskDefinition("test", "timestamp --timestamp.format=yyyy");
		assertEquals("test", definition.getName());
		assertEquals("timestamp --timestamp.format=yyyy", definition.getDslText());
		assertEquals("timestamp", definition.getRegisteredAppName());
		assertEquals(2, definition.getProperties().size());
		assertEquals("test", definition.getProperties().get("spring.cloud.task.name"));
		assertEquals("yyyy", definition.getProperties().get("timestamp.format"));
	}

	@Test
	public void testBuilderSetProperties() {
		Map<String,String> properties = new HashMap<>();
		properties.put("foo", "bar");
		TaskDefinition definition = new TaskDefinition.TaskDefinitionBuilder()
				.from(new TaskDefinition("test", "timestamp"))
				.setProperties(properties)
				.build();
		assertEquals("test", definition.getName());
		assertEquals("timestamp", definition.getRegisteredAppName());
		assertEquals(1, definition.getProperties().size());
		assertEquals("bar", definition.getProperties().get("foo"));
	}

	}
