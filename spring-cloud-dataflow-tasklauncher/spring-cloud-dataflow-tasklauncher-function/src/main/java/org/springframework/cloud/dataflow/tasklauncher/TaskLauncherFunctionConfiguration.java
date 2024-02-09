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

package org.springframework.cloud.dataflow.tasklauncher;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the TaskLauncher Data Flow Sink.
 *
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
@Configuration
@EnableConfigurationProperties({TaskLauncherFunctionProperties.class, DataFlowClientProperties.class})
public class TaskLauncherFunctionConfiguration {

	@Bean
	public TaskLauncherFunction taskLauncherFunction(DataFlowOperations dataFlowOperations, TaskLauncherFunctionProperties functionProperties) {

		if (dataFlowOperations.taskOperations() == null) {
			throw new IllegalArgumentException("The SCDF server does not support task operations");
		}
		TaskLauncherFunction function = new TaskLauncherFunction(dataFlowOperations.taskOperations());
		function.setPlatformName(functionProperties.getPlatformName());
		return function;
	}
}
