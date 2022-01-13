/*
 * Copyright 2016-2020 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

/**
 * Jackson MixIn for {@link StepExecution} de-serialization.
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
@JsonIgnoreProperties({ "jobExecution" })
public abstract class StepExecutionJacksonMixIn {

	@JsonCreator
	StepExecutionJacksonMixIn(@JsonProperty("stepName") String stepName) {
	}

	@JsonProperty(access = Access.READ_ONLY)
	abstract int getSkipCount();

	@JsonProperty(access = Access.READ_ONLY)
	abstract String getSummary();

	@JsonProperty(access = Access.READ_ONLY)
	abstract JobParameters getJobParameters();

	@JsonProperty(access = Access.READ_ONLY)
	abstract Long getJobExecutionId();
}
