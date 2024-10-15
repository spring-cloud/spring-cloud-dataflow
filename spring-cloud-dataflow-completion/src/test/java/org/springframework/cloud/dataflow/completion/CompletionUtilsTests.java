/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.completion;


import java.util.LinkedList;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.DefaultStreamDefinitionService;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CompletionUtils.
 *
 * @author Eric Bottard
 * @author Corneil du Plessis
 */
class CompletionUtilsTests {

	final StreamDefinitionService streamDefinitionService  = new DefaultStreamDefinitionService();

	@Test
	void labelQualification() {
		StreamDefinition streamDefinition = new StreamDefinition("foo", "http | filter");
		LinkedList<StreamAppDefinition> appDefinitions = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertThat(CompletionUtils.maybeQualifyWithLabel("filter", appDefinitions))
			.isEqualTo("filter2: filter");

		streamDefinition = new StreamDefinition("foo", "http | filter");
		appDefinitions = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertThat(CompletionUtils.maybeQualifyWithLabel("transform", appDefinitions))
			.isEqualTo("transform");

		streamDefinition = new StreamDefinition("foo", "http | filter | filter2: filter");
		appDefinitions = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertThat(CompletionUtils.maybeQualifyWithLabel("filter", appDefinitions)).isEqualTo("filter3: filter");
	}

}
