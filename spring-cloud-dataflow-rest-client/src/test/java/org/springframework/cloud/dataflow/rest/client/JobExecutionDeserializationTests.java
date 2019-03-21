/*
 * Copyright 2016 the original author or authors.
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
import org.junit.Assert;
import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.cloud.dataflow.rest.client.support.ExecutionContextJacksonMixIn;
import org.springframework.cloud.dataflow.rest.client.support.ExitStatusJacksonMixIn;
import org.springframework.cloud.dataflow.rest.client.support.JobExecutionJacksonMixIn;
import org.springframework.cloud.dataflow.rest.client.support.JobInstanceJacksonMixIn;
import org.springframework.cloud.dataflow.rest.client.support.JobParameterJacksonMixIn;
import org.springframework.cloud.dataflow.rest.client.support.JobParametersJacksonMixIn;
import org.springframework.cloud.dataflow.rest.client.support.StepExecutionHistoryJacksonMixIn;
import org.springframework.cloud.dataflow.rest.client.support.StepExecutionJacksonMixIn;
import org.springframework.cloud.dataflow.rest.job.StepExecutionHistory;
import org.springframework.cloud.dataflow.rest.job.support.ISO8601DateFormatWithMilliSeconds;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.util.StreamUtils;

/**
 * @author Gunnar Hillert
 * @author Glenn Renfro
 */
public class JobExecutionDeserializationTests {

	@Test
	public void testDeserializationOfMultipleJobExecutions() throws IOException {

		final ObjectMapper objectMapper = new ObjectMapper();

		final InputStream inputStream =
				JobExecutionDeserializationTests.class.getResourceAsStream("/JobExecutionJson.txt");

		final String json = new String(StreamUtils.copyToByteArray(inputStream));

		objectMapper.registerModule(new Jackson2HalModule());
		objectMapper.addMixIn(JobExecution.class, JobExecutionJacksonMixIn.class);
		objectMapper.addMixIn(JobParameters.class, JobParametersJacksonMixIn.class);
		objectMapper.addMixIn(JobParameter.class, JobParameterJacksonMixIn.class);
		objectMapper.addMixIn(JobInstance.class, JobInstanceJacksonMixIn.class);
		objectMapper.addMixIn(StepExecution.class, StepExecutionJacksonMixIn.class);
		objectMapper.addMixIn(StepExecutionHistory.class, StepExecutionHistoryJacksonMixIn.class);
		objectMapper.addMixIn(ExecutionContext.class, ExecutionContextJacksonMixIn.class);
		objectMapper.addMixIn(ExitStatus.class, ExitStatusJacksonMixIn.class);

		PagedResources<Resource<JobExecutionResource>> paged = objectMapper.readValue(json,
				new TypeReference<PagedResources<Resource<JobExecutionResource>>>() {});
		JobExecutionResource jobExecutionResource = paged.getContent().iterator().next().getContent();
		Assert.assertEquals("Expect 1 JobExecutionInfoResource", 6, paged.getContent().size());
		Assert.assertEquals(Long.valueOf(6), jobExecutionResource.getJobId());
		Assert.assertEquals("job200616815", jobExecutionResource.getName());
		Assert.assertEquals("COMPLETED", jobExecutionResource.getJobExecution().getStatus().name());

	}

	@Test
	public void testDeserializationOfSingleJobExecution() throws IOException {

		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new Jackson2HalModule());

		final InputStream inputStream = JobExecutionDeserializationTests.class.getResourceAsStream("/SingleJobExecutionJson.txt");

		final String json = new String(StreamUtils.copyToByteArray(inputStream));

		objectMapper.addMixIn(JobExecution.class, JobExecutionJacksonMixIn.class);
		objectMapper.addMixIn(JobParameters.class, JobParametersJacksonMixIn.class);
		objectMapper.addMixIn(JobParameter.class, JobParameterJacksonMixIn.class);
		objectMapper.addMixIn(JobInstance.class, JobInstanceJacksonMixIn.class);
		objectMapper.addMixIn(StepExecution.class, StepExecutionJacksonMixIn.class);
		objectMapper.addMixIn(StepExecutionHistory.class, StepExecutionHistoryJacksonMixIn.class);
		objectMapper.addMixIn(ExecutionContext.class, ExecutionContextJacksonMixIn.class);
		objectMapper.addMixIn(ExitStatus.class, ExitStatusJacksonMixIn.class);
		objectMapper.setDateFormat(new ISO8601DateFormatWithMilliSeconds());

		final JobExecutionResource jobExecutionInfoResource = objectMapper.readValue(json,
				JobExecutionResource.class);

		Assert.assertNotNull(jobExecutionInfoResource);
		Assert.assertEquals(Long.valueOf(1), jobExecutionInfoResource.getJobId());
		Assert.assertEquals("ff.job", jobExecutionInfoResource.getName());
		Assert.assertEquals("COMPLETED", jobExecutionInfoResource.getJobExecution().getStatus().name());

	}

}
