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
import java.io.InputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.within;

/**
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
public class ExecutionContextDeserializationTests {

	@Test
	public void testDeserializationOfBasicExecutionContext() throws IOException {

		final ObjectMapper objectMapper = DataFlowTemplate.prepareObjectMapper(new ObjectMapper());

		final InputStream inputStream = ExecutionContextDeserializationTests.class
				.getResourceAsStream("/BasicExecutionContextJson.txt");

		final String json = new String(StreamUtils.copyToByteArray(inputStream));

		ExecutionContext executionContext = objectMapper.readValue(json,
				new TypeReference<ExecutionContext>() {
				});
		assertThat(executionContext.entrySet().size()).isEqualTo(2);
		assertThat(executionContext.get("batch.taskletType")).isEqualTo("org.springframework.cloud.task.app.timestamp.batch.TimestampBatchTaskConfiguration$1");
		assertThat(executionContext.get("batch.stepType")).isEqualTo("org.springframework.batch.core.step.tasklet.TaskletStep");
		assertThat(executionContext.isDirty()).isFalse();
		assertThat(executionContext.isEmpty()).isFalse();
	}

	/**
	 * Edge-case. If the JSON data contains values in the execution context but
	 * the {@code empty} property is {@code true}. The deserialized object will return
	 * {@code false}. The property is ignored during deserializtion.
	 *
	 * @throws IOException
	 */
	@Test
	public void testFaultyExecutionContext() throws IOException {

		final ObjectMapper objectMapper = DataFlowTemplate.prepareObjectMapper(new ObjectMapper());

		final InputStream inputStream = ExecutionContextDeserializationTests.class
				.getResourceAsStream("/FaultyExecutionContextJson.txt");

		final String json = new String(StreamUtils.copyToByteArray(inputStream));

		ExecutionContext executionContext = objectMapper.readValue(json,
				new TypeReference<ExecutionContext>() {
				});
		assertThat(executionContext.entrySet().size()).isEqualTo(2);
		assertThat(executionContext.get("batch.taskletType")).isEqualTo("org.springframework.cloud.task.app.timestamp.batch.TimestampBatchTaskConfiguration$1");
		assertThat(executionContext.get("batch.stepType")).isEqualTo("org.springframework.batch.core.step.tasklet.TaskletStep");
		assertThat(executionContext.isDirty()).isTrue();
		assertThat(executionContext.isEmpty()).isFalse();
	}

	@Test
	public void testExecutionContextWithNonStringValues() throws IOException {

		final ObjectMapper objectMapper = DataFlowTemplate.prepareObjectMapper(new ObjectMapper());

		final InputStream inputStream = ExecutionContextDeserializationTests.class
				.getResourceAsStream("/ExecutionContextJsonWithNonStringValues.txt");

		final String json = new String(StreamUtils.copyToByteArray(inputStream));

		final ExecutionContext executionContext = objectMapper.readValue(json,
				new TypeReference<ExecutionContext>() {
				});
		assertThat(executionContext.entrySet().size()).isEqualTo(6);
		assertThat(executionContext.getInt("barNumber")).isEqualTo(1234);
		assertThat(executionContext.getString("barNumberAsString")).isEqualTo("1234");

		try {
			executionContext.getLong("barNumber");
			fail("Expected a ClassCastException to be thrown.");
		}
		catch (ClassCastException ce) {
			assertThat(ce.getMessage()).contains("key=[barNumber] is not of type: [class java.lang.Long], it is [(class java.lang.Integer)");
		}

		try {
			executionContext.getDouble("barNumber");
			fail("Expected a ClassCastException to be thrown.");
		}
		catch (ClassCastException ce) {
			assertThat(ce.getMessage()).contains("key=[barNumber] is not of type: [class java.lang.Double], it is [(class java.lang.Integer)");
		}

		assertThat(executionContext.getLong("longNumber")).isEqualTo(22222222222L);

		try {
			executionContext.getInt("longNumber");
			fail("Expected a ClassCastException to be thrown.");
		}
		catch (ClassCastException ce) {
			assertThat(ce.getMessage()).contains("key=[longNumber] is not of type: [class java.lang.Integer], it is [(class java.lang.Long)");
		}

		assertThat(executionContext.get("fooBoolean")).isEqualTo("true");
		assertThat(executionContext.getDouble("floatNumber")).isCloseTo(3.5, within(0.1));
		assertThat(executionContext.getString("floatNumberArray")).isEqualTo("[1,2,3]");

		assertThat(executionContext.isDirty()).isFalse();
		assertThat(executionContext.isEmpty()).isFalse();
	}
}
