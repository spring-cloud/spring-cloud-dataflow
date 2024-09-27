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

package org.springframework.cloud.dataflow.rest.client;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.batch.item.ExecutionContext;

/**
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
class ExecutionContextSerializationTests {

	@Test
	void serializationOfExecutionContext() throws IOException {
		final ObjectMapper objectMapper = DataFlowTemplate.prepareObjectMapper(new ObjectMapper());

		final ExecutionContext stepExecutionExecutionContext = new ExecutionContext();
		stepExecutionExecutionContext.put("foo", "bar");
		stepExecutionExecutionContext.put("foo2", "bar2");

		final String serializedExecutionContext = objectMapper.writeValueAsString(stepExecutionExecutionContext);
		final String expectedExecutionContext = "{\"dirty\":true,\"empty\":false,\"values\":[{\"foo\":\"bar\"},{\"foo2\":\"bar2\"}]}";
		assertThat(serializedExecutionContext).isEqualTo(expectedExecutionContext);

	}

}
