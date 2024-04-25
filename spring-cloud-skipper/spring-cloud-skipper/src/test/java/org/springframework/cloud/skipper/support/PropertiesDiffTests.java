/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.support;


import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.skipper.support.PropertiesDiff.PropertyChange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesDiff}.
 *
 * @author Janne Valkealahti
 *
 */
public class PropertiesDiffTests {

	@Test
	public void testEmptyMaps() {
		Map<String, String> left = new HashMap<>();
		Map<String, String> right = new HashMap<>();
		PropertiesDiff diff = PropertiesDiff.builder().left(left).right(right).build();

		assertThat(diff.areEqual()).isTrue();
		assertThat(diff.getAdded()).hasSize(0);
		assertThat(diff.getRemoved()).hasSize(0);
		assertThat(diff.getChanged()).hasSize(0);
		assertThat(diff.getCommon()).hasSize(0);
	}

	@Test
	public void testAddedRemovedChanging() {
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
	}

	@Test
	public void testChangedValues() {
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
