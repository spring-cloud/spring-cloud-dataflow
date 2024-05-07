/*
 * Copyright 2016-2019 the original author or authors.
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

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
public class JobExecutionDeserializationTests {

	@Test
	public void testDeserializationOfMultipleJobExecutions() throws IOException {

		final ObjectMapper objectMapper = DataFlowTemplate.prepareObjectMapper(new ObjectMapper());

		final InputStream inputStream = JobExecutionDeserializationTests.class
				.getResourceAsStream("/JobExecutionJson.txt");

		final String json = new String(StreamUtils.copyToByteArray(inputStream));

		final PagedModel<EntityModel<JobExecutionResource>> paged = objectMapper.readValue(json,
				new TypeReference<PagedModel<EntityModel<JobExecutionResource>>>() {
				});
		final JobExecutionResource jobExecutionResource = paged.getContent().iterator().next().getContent();
		assertThat(paged.getContent().size()).as("Expect 1 JobExecutionInfoResource").isEqualTo(6);
		assertThat(jobExecutionResource.getJobId()).isEqualTo(Long.valueOf(6));
		assertThat(jobExecutionResource.getName()).isEqualTo("job200616815");
		assertThat(jobExecutionResource.getJobExecution().getStatus().name()).isEqualTo("COMPLETED");
	}

	@Test
	public void testDeserializationOfSingleJobExecution() throws IOException {

		final ObjectMapper objectMapper = DataFlowTemplate.prepareObjectMapper(new ObjectMapper());

		final InputStream inputStream = JobExecutionDeserializationTests.class
				.getResourceAsStream("/SingleJobExecutionJson.txt");

		final String json = new String(StreamUtils.copyToByteArray(inputStream));

		final JobExecutionResource jobExecutionInfoResource = objectMapper.readValue(json, JobExecutionResource.class);

		assertThat(jobExecutionInfoResource).isNotNull();
		assertThat(jobExecutionInfoResource.getJobId()).isEqualTo(Long.valueOf(1));
		assertThat(jobExecutionInfoResource.getName()).isEqualTo("ff.job");
		assertThat(jobExecutionInfoResource.getJobExecution().getStatus().name()).isEqualTo("COMPLETED");
		assertThat(jobExecutionInfoResource.getJobExecution().getStepExecutions().size()).isEqualTo(1);

		final StepExecution stepExecution = jobExecutionInfoResource.getJobExecution().getStepExecutions().iterator().next();
		assertThat(stepExecution).isNotNull();

		final ExecutionContext stepExecutionExecutionContext = stepExecution.getExecutionContext();

		assertThat(stepExecutionExecutionContext).isNotNull();
		assertThat(stepExecutionExecutionContext.size()).isEqualTo(2);
	}

}
