/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.service.impl.diff;


import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.server.service.impl.diff.PropertiesDiff.PropertyChange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesDiff}.
 *
 * @author Janne Valkealahti
 * @author Corneil du Plessis
 */
class PropertiesDiffTests {

	@Test
	void emptyMaps() {
		Map<String, String> left = new HashMap<>();
		Map<String, String> right = new HashMap<>();
		PropertiesDiff diff = PropertiesDiff.builder().left(left).right(right).build();

		assertThat(diff.areEqual()).isTrue();
		assertThat(diff.getAdded()).isEmpty();
		assertThat(diff.getRemoved()).isEmpty();
		assertThat(diff.getChanged()).isEmpty();
		assertThat(diff.getCommon()).isEmpty();
		assertThat(diff.getDeleted()).isEmpty();
	}

	@Test
	void addedRemovedChanging() {
		Map<String, String> left = new HashMap<>();
		left.put("key1", "value1");
		left.put("key2", "value21");
		left.put("key3", "value3");
		Map<String, String> right = new HashMap<>();
		right.put("key1", "value1");
		right.put("key2", "value22");
		right.put("key4", "value4");
		PropertiesDiff diff = PropertiesDiff.builder().left(left).right(right).build();

		assertThat(diff.areEqual()).isFalse();
		assertThat(diff.getAdded()).hasSize(1);
		assertThat(diff.getRemoved()).hasSize(1);
		assertThat(diff.getChanged()).hasSize(1);
		assertThat(diff.getCommon()).hasSize(1);
		assertThat(diff.getDeleted()).isEmpty();
	}

	@Test
	void removedByEffectivelyNull() {
		Map<String, String> left = new HashMap<>();
		left.put("key1", "value1");
		left.put("key2", "value2");
		left.put("key3", "value3");
		Map<String, String> right = new HashMap<>();
		right.put("key1", "value1");
		right.put("key2", "");
		right.put("key3", null);
		PropertiesDiff diff = PropertiesDiff.builder().left(left).right(right).build();

		assertThat(diff.areEqual()).isFalse();
		assertThat(diff.getAdded()).isEmpty();
		assertThat(diff.getRemoved()).isEmpty();
		assertThat(diff.getChanged()).isEmpty();
		assertThat(diff.getCommon()).hasSize(1);
		assertThat(diff.getDeleted()).hasSize(2);
	}

	@Test
	void changedValues() {
		Map<String, String> left = new HashMap<>();
		left.put("key1", "value1");
		Map<String, String> right = new HashMap<>();
		right.put("key1", "value2");
		PropertiesDiff diff = PropertiesDiff.builder().left(left).right(right).build();

		assertThat(diff.getChanged()).hasSize(1);
		PropertyChange propertyChange = diff.getChanged().get("key1");
		assertThat(propertyChange).isNotNull();
		assertThat(propertyChange.getOriginal()).isEqualTo("value1");
		assertThat(propertyChange.getReplaced()).isEqualTo("value2");
	}

}
