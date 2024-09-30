/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.rest.util.TaskSanitizer;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
class TaskSanitizerTest {

	private final TaskSanitizer taskSanitizer = new TaskSanitizer();


	@Test
	void taskExecutionArguments() {
		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName("a1");
		taskExecution.setArguments(Arrays.asList("--username=test", "--password=testing"));
		TaskExecution sanitizedTaskExecution = this.taskSanitizer.sanitizeTaskExecutionArguments(taskExecution);
		assertThat(sanitizedTaskExecution.getArguments().get(0)).isEqualTo("--username=******");
		assertThat(sanitizedTaskExecution.getArguments().get(1)).isEqualTo("--password=******");
	}

	@Test
	void taskManifest() {
		TaskManifest taskManifest = new TaskManifest();
		AppDeploymentRequest appDeploymentRequest = mock(AppDeploymentRequest.class);
		Map<String, String> appProperties = new HashMap<>();
		appProperties.put("secret", "testing");
		appProperties.put("user.key", "dev");
		when(appDeploymentRequest.getDefinition()).thenReturn(new AppDefinition("test-app", appProperties));
		when(appDeploymentRequest.getResource()).thenReturn(mock(Resource.class));
		when(appDeploymentRequest.getCommandlineArguments()).thenReturn(Arrays.asList("--username=test", "--password=testing"));
		Map<String, String> deploymentProperties = new HashMap<>();
		deploymentProperties.put("secret", "testing");
		deploymentProperties.put("user.key", "dev");
		when(appDeploymentRequest.getDeploymentProperties()).thenReturn(deploymentProperties);
		taskManifest.setTaskDeploymentRequest(appDeploymentRequest);
		TaskManifest sanitizedTaskManifest = this.taskSanitizer.sanitizeTaskManifest(taskManifest);
		List<String> commandLineArgs = sanitizedTaskManifest.getTaskDeploymentRequest().getCommandlineArguments();
		assertThat(commandLineArgs.get(0)).isEqualTo("--username=******");
		assertThat(commandLineArgs.get(1)).isEqualTo("--password=******");
		Map<String, String> deploymentProps = sanitizedTaskManifest.getTaskDeploymentRequest().getDeploymentProperties();
		assertThat(sanitizedTaskManifest.getTaskDeploymentRequest().getDefinition().getProperties()).containsEntry("secret", "******");
		assertThat(sanitizedTaskManifest.getTaskDeploymentRequest().getDefinition().getProperties()).containsEntry("user.key", "******");
		assertThat(deploymentProps)
				.containsEntry("secret", "******")
				.containsEntry("user.key", "******");

	}
}
