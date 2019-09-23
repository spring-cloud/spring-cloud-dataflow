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

package org.springframework.cloud.dataflow.scheduler.launcher.configuration;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configure the SchedulerTaskLauncher application.
 *
 * @author Glenn Renfro
 */
@Configuration
@EnableConfigurationProperties({SchedulerTaskLauncherProperties.class})
@EnableAutoConfiguration(exclude = {DataFlowClientAutoConfiguration.class})
public class LauncherConfiguration {

	@Bean
	public SchedulerTaskLauncher launchRequestConsumer(
			SchedulerTaskLauncherProperties schedulerTaskLauncherProperties,
			TaskOperations taskOperations, Environment environment) {
		return new SchedulerTaskLauncher(schedulerTaskLauncherProperties.getTaskName(),
				schedulerTaskLauncherProperties.getPlatformName(), taskOperations,
				schedulerTaskLauncherProperties, environment);
	}

	@Bean
	public TaskOperations getTaskOperations(SchedulerTaskLauncherProperties schedulerTaskLauncherProperties) {
		try {
			final URI dataFlowUri = new URI(schedulerTaskLauncherProperties.getDataflowServerUri());
			final DataFlowOperations dataFlowOperations = new DataFlowTemplate(dataFlowUri);
			if (dataFlowOperations.taskOperations() == null) {
				throw new SchedulerTaskLauncherException("The SCDF server does not support task operations");
			}
			return dataFlowOperations.taskOperations();
		}
		catch (URISyntaxException e) {
			throw new SchedulerTaskLauncherException("Invalid Spring Cloud Data Flow URI", e);
		}
	}

	@Bean
	public CommandLineRunner commandLineRunner(SchedulerTaskLauncher schedulerTaskLauncher) {
		return args -> {
			schedulerTaskLauncher.launchTask(args);
		};
	}

}
