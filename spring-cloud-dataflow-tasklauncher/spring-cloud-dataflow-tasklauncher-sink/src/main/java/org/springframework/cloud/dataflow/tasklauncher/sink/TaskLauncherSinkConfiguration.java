/*
 * Copyright 2021-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.tasklauncher.sink;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.tasklauncher.CannotHandleRequestException;
import org.springframework.cloud.dataflow.tasklauncher.LaunchRequest;
import org.springframework.cloud.dataflow.tasklauncher.TaskLauncherFunction;
import org.springframework.cloud.dataflow.tasklauncher.TaskLauncherFunctionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration class for the TaskLauncher Data Flow Sink.
 *
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
@EnableConfigurationProperties({ RetryProperties.class })
@Import(TaskLauncherFunctionConfiguration.class)
public class TaskLauncherSinkConfiguration {

	private static final Log log = LogFactory.getLog(TaskLauncherSinkConfiguration.class);

	static class LaunchRequestConsumer implements LaunchRequestMessageConsumer {

		private final TaskLauncherFunction taskLauncherFunction;

		public LaunchRequestConsumer(TaskLauncherFunction taskLauncherFunction) {
			this.taskLauncherFunction = taskLauncherFunction;
		}

		@Override
		public void accept(Message<LaunchRequest> message) {
			taskLauncherFunction.accept(message.getPayload());
		}

	}

	@Bean(name = "launchRequestConsumer")
	LaunchRequestConsumer launchRequestConsumer(TaskLauncherFunction taskLauncherFunction) {
		return new LaunchRequestConsumer(taskLauncherFunction);
	}

	@Bean(name = "launchRequestConsumerRetry")
	public RetryTemplate retryTemplate(@Validated RetryProperties retryProperties) {
		if (log.isDebugEnabled()) {
			log.debug("retryTemplate:retryProperties=" + retryProperties);
		}
		RetryTemplateBuilder builder = new RetryTemplateBuilder();
		builder.retryOn(CannotHandleRequestException.class)
			.exponentialBackoff(retryProperties.getInitialDelay(), retryProperties.getMultiplier(),
					retryProperties.getMaxPeriod());
		if (retryProperties.getMaxAttempts() >= 0) {
			builder.maxAttempts(retryProperties.getMaxAttempts());
		}
		else {
			builder.infiniteRetry();
		}
		return builder.build();
	}
}
