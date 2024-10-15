/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.support.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * Tests that the {@link ExecutionContextJacksonMixIn} works as expected.
 *
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
class StepExecutionJacksonMixInTests {

	/**
	 * Assert that without using the {@link ExecutionContextJacksonMixIn} Jackson does not
	 * render the Step Execution Context correctly (Missing values).
	 *
	 * @throws JsonProcessingException if a Json generation error occurs.
	 */
	@Test
	void serializationOfSingleStepExecutionWithoutMixin() throws JsonProcessingException {
		assertThatExceptionOfType(JsonMappingException.class).isThrownBy(() -> {
		final ObjectMapper objectMapper = new ObjectMapper();

		final StepExecution stepExecution = getStepExecution();
		final String result = objectMapper.writeValueAsString(stepExecution);

			assertThat(result).contains("\"executionContext\":{\"dirty\":true,\"empty\":false}");
		});
	}

	/**
	 * Assert that by using the {@link ExecutionContextJacksonMixIn} Jackson renders the
	 * Step Execution Context correctly.
	 *
	 * @throws JsonProcessingException if a Json generation error occurs.
	 */
	@Test
	void serializationOfSingleStepExecution() throws JsonProcessingException {

		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());

		objectMapper.addMixIn(StepExecution.class, StepExecutionJacksonMixIn.class);
		objectMapper.addMixIn(ExecutionContext.class, ExecutionContextJacksonMixIn.class);

		final StepExecution stepExecution = getStepExecution();
		final String result = objectMapper.writeValueAsString(stepExecution);

		assertThat(result).doesNotContain("\"executionContext\":{\"dirty\":true,\"empty\":false}");
		assertThat(result).contains("\"executionContext\":{\"dirty\":true,\"empty\":false,\"values\":[{");

		assertThat(result).contains("{\"counter\":1234}");
		assertThat(result).contains("{\"myDouble\":1.123456}");
		assertThat(result).contains("{\"Josh\":4444444444}");
		assertThat(result).contains("{\"awesomeString\":\"Yep\"}");
		assertThat(result).contains("{\"hello\":\"world\"");
		assertThat(result).contains("{\"counter2\":9999}");
	}

	private StepExecution getStepExecution() {
		JobExecution jobExecution = new JobExecution(1L, new JobParameters());
		final StepExecution stepExecution = new StepExecution("step1", jobExecution);
		jobExecution.createStepExecution("step1");
		final ExecutionContext executionContext = stepExecution.getExecutionContext();

		executionContext.putInt("counter", 1234);
		executionContext.putDouble("myDouble", 1.123456d);
		executionContext.putLong("Josh", 4444444444L);
		executionContext.putString("awesomeString", "Yep");
		executionContext.put("hello", "world");
		executionContext.put("counter2", 9999);

		return stepExecution;
	}
}
