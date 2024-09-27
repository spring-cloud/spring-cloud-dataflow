/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;
import java.util.HashSet;

import static org.springframework.cloud.dataflow.server.config.CleanupTaskExecutionProperties.CLEANUP_TASK_EXECUTION_PROPS_PREFIX;

/**
 * Configuration for task executions cleanup schedule.
 * @author Ganghun Cho
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ CleanupTaskExecutionProperties.class })
@ConditionalOnProperty(prefix = CLEANUP_TASK_EXECUTION_PROPS_PREFIX, name = "enabled", havingValue = "true")
@ConditionalOnBean({ EnableDataFlowServerConfiguration.Marker.class })
@EnableScheduling
public class CleanupTaskExecutionConfiguration {
	private final TaskDeleteService taskDeleteService;

	private final CleanupTaskExecutionProperties cleanupTaskExecutionProperties;

	public CleanupTaskExecutionConfiguration(TaskDeleteService taskDeleteService,
		CleanupTaskExecutionProperties cleanupTaskExecutionProperties) {
		this.taskDeleteService = taskDeleteService;
		this.cleanupTaskExecutionProperties = cleanupTaskExecutionProperties;
	}

	@Scheduled(cron = "${spring.cloud.dataflow.task.execution.cleanup.schedule:0 0 * * * *}")
	public void getAsyncExecutor() {
		this.taskDeleteService.cleanupExecutions(
			new HashSet<>(Arrays.asList(cleanupTaskExecutionProperties.getAction())),
			cleanupTaskExecutionProperties.getTaskName(),
			cleanupTaskExecutionProperties.isCompleted(),
			cleanupTaskExecutionProperties.getDays());
	}

}
