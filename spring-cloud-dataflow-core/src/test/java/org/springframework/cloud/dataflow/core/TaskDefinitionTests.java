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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thomas Risberg
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
class TaskDefinitionTests {

	@Test
	void definition() {
		TaskDefinition definition = new TaskDefinition("test", "timestamp");
		assertThat(definition.getName()).isEqualTo("test");
		assertThat(definition.getDslText()).isEqualTo("timestamp");
		assertThat(definition.getRegisteredAppName()).isEqualTo("timestamp");
		assertThat(definition.getProperties()).hasSize(1);
		assertThat(definition.getProperties()).containsEntry("spring.cloud.task.name", "test");

		TaskDefinition composedDef = new TaskDefinition("composed", "foo && bar");
		assertThat(composedDef.getName()).isEqualTo("composed");
		assertThat(composedDef.getDslText()).isEqualTo("foo && bar");
		assertThat(composedDef.getRegisteredAppName()).isEqualTo("composed");
		assertThat(composedDef.getProperties()).hasSize(1);
		assertThat(composedDef.getProperties()).containsEntry("spring.cloud.task.name", "composed");
	}

	@Test
	void packageProtectedConstructor() {
		TaskDefinition definition = new TaskDefinition("timestamp", "label",
				Collections.singletonMap("spring.cloud.task.name", "label"));
		assertThat(definition.getName()).isEqualTo("label");
		assertThat(definition.getRegisteredAppName()).isEqualTo("timestamp");
		assertThat(definition.getProperties()).hasSize(1);
		assertThat(definition.getProperties()).containsEntry("spring.cloud.task.name", "label");
	}

	@Test
	void builder() {
		TaskDefinition definition = new TaskDefinition.TaskDefinitionBuilder()
				.from(new TaskDefinition("test", "timestamp"))
				.build();
		assertThat(definition.getName()).isEqualTo("test");
		assertThat(definition.getRegisteredAppName()).isEqualTo("timestamp");
		assertThat(definition.getProperties()).hasSize(1);
		assertThat(definition.getProperties()).containsEntry("spring.cloud.task.name", "test");
	}

	@Test
	void equality() {
		TaskDefinition definitionOne = new TaskDefinition("test", "timestamp");
		TaskDefinition definitionTwo = new TaskDefinition("test", "timestamp");

		assertThat(definitionTwo).as("TaskDefinitions were expected to be equal.").isEqualTo(definitionOne);
		assertThat(definitionOne).as("TaskDefinitions were expected to be equal.").isEqualTo(definitionOne);

	}

	@Test
	void inequality() {
		TaskDefinition definitionOne = new TaskDefinition("test", "timestamp");
		TaskDefinition definitionFoo = new TaskDefinition("test", "foo");

		assertThat(definitionFoo).as("TaskDefinitions were not expected to be equal.").isNotEqualTo(definitionOne);
		assertThat(definitionOne).as("TaskDefinitions were not expected to be equal.").isNotEqualTo(null);
		assertThat(definitionOne).as("TaskDefinitions were not expected to be equal.").isNotEqualTo("HI");
	}

	@Test
	void testHashCode() {
		TaskDefinition definitionOne = new TaskDefinition("test", "timestamp");
		TaskDefinition definitionTwo = new TaskDefinition("test", "timestamp");
		TaskDefinition definitionFoo = new TaskDefinition("test", "foo");

		assertThat(definitionTwo.hashCode()).as("TaskDefinitions' hashcodes were expected to be equal.").isEqualTo(definitionOne.hashCode());
		assertThat(definitionOne.hashCode() == definitionFoo.hashCode()).as("TaskDefinitions' hashcodes were not expected to be equal.").isFalse();
	}

	@Test
	void definitionWithArguments() {
		TaskDefinition definition = new TaskDefinition("test", "timestamp --timestamp.format=yyyy");
		assertThat(definition.getName()).isEqualTo("test");
		assertThat(definition.getDslText()).isEqualTo("timestamp --timestamp.format=yyyy");
		assertThat(definition.getRegisteredAppName()).isEqualTo("timestamp");
		assertThat(definition.getProperties()).hasSize(2);
		assertThat(definition.getProperties()).containsEntry("spring.cloud.task.name", "test");
		assertThat(definition.getProperties()).containsEntry("timestamp.format", "yyyy");
	}

	@Test
	void builderSetProperties() {
		Map<String,String> properties = new HashMap<>();
		properties.put("foo", "bar");
		TaskDefinition definition = new TaskDefinition.TaskDefinitionBuilder()
				.from(new TaskDefinition("test", "timestamp"))
				.setProperties(properties)
				.build();
		assertThat(definition.getName()).isEqualTo("test");
		assertThat(definition.getRegisteredAppName()).isEqualTo("timestamp");
		assertThat(definition.getProperties()).hasSize(1);
		assertThat(definition.getProperties()).containsEntry("foo", "bar");
	}

	}
