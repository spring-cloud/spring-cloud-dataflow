/*
 * Copyright 2021 the original author or authors.
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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.cloud.dataflow.rest.job.StepExecutionHistory;

/**
 * Jackson 2 module to handle dataflow related instances like batch.
 *
 * @author Janne Valkealahti
 */
public class Jackson2DataflowModule extends SimpleModule {

	public Jackson2DataflowModule() {
		super("spring-cloud-dataflow-module", new Version(1, 0, 0, null, "org.springframework.cloud", "spring-cloud-dataflow"));

		setMixInAnnotation(JobExecution.class, JobExecutionJacksonMixIn.class);
		setMixInAnnotation(JobParameters.class, JobParametersJacksonMixIn.class);
		setMixInAnnotation(JobParameter.class, JobParameterJacksonMixIn.class);
		setMixInAnnotation(JobInstance.class, JobInstanceJacksonMixIn.class);
		setMixInAnnotation(ExitStatus.class, ExitStatusJacksonMixIn.class);
		setMixInAnnotation(StepExecution.class, StepExecutionJacksonMixIn.class);
		setMixInAnnotation(ExecutionContext.class, ExecutionContextJacksonMixIn.class);
		setMixInAnnotation(StepExecutionHistory.class, StepExecutionHistoryJacksonMixIn.class);
	}
}
