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

package org.springframework.cloud.dataflow.tasklauncher;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.CurrentTaskExecutionsResource;
import org.springframework.cloud.dataflow.rest.resource.LaunchResponseResource;
import org.springframework.cloud.dataflow.rest.resource.LauncherResource;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.PagedModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class TaskLauncherFunctionApplicationTests {

	@Autowired
	private TaskLauncherFunction taskLauncherFunction;

	@Autowired
	private TaskOperations taskOperations;

	@Test
	public void successfulLaunch() {
		LaunchRequest launchRequest = new LaunchRequest();
		launchRequest.setTaskName("someTask");
		setCurrentExecutionState(1);
		taskLauncherFunction.accept(launchRequest);

		verify(taskOperations).launch("someTask",
				Collections.singletonMap(TaskLauncherFunction.TASK_PLATFORM_NAME, "default"),
				Collections.emptyList());
	}

	@Test
	public void taskPlatformAtCapacity() {
		LaunchRequest launchRequest = new LaunchRequest();
		launchRequest.setTaskName("someTask");
		setCurrentExecutionState(3);
		assertThatThrownBy(() -> {
		taskLauncherFunction.accept(launchRequest);
		}).isInstanceOf(CannotHandleRequestException.class);
	}

	@Test
	public void platformMismatch() {
		LaunchRequest launchRequest = new LaunchRequest();
		launchRequest.setTaskName("someTask");
		launchRequest
				.setDeploymentProperties(Collections.singletonMap(TaskLauncherFunction.TASK_PLATFORM_NAME, "other"));
		setCurrentExecutionState(0);
		assertThatIllegalStateException().isThrownBy(() -> taskLauncherFunction.accept(launchRequest))
				.withStackTraceContaining("does not match the platform configured for the Task Launcher");
	}

	private void setCurrentExecutionState(int runningExecutions) {
		CurrentTaskExecutionsResource currentTaskExecutionsResource = new CurrentTaskExecutionsResource();
		currentTaskExecutionsResource.setMaximumTaskExecutions(3);
		currentTaskExecutionsResource.setRunningExecutionCount(runningExecutions);
		currentTaskExecutionsResource.setName("default");
		when(taskOperations.currentTaskExecutions())
				.thenReturn(Collections.singletonList(currentTaskExecutionsResource));
		when(taskOperations.launch(anyString(), anyMap(), anyList())).thenReturn(new LaunchResponseResource(1L, SchemaVersionTarget.defaultTarget().getName()));
	}

	@Configuration
	@Import(TaskLauncherFunctionConfiguration.class)
	static class TestConfig {

		@Bean
		@Profile("default")
		TaskOperations taskOperations() {
			TaskOperations taskOperations = mock(TaskOperations.class);
			LauncherResource launcherResource = mock(LauncherResource.class);
			when(launcherResource.getName()).thenReturn("default");

			when(taskOperations.listPlatforms()).thenReturn(PagedModel.of(
					Collections.singletonList(launcherResource), (PagedModel.PageMetadata) null));
			return taskOperations;
		}

		@Bean
		@Profile("nolaunchers")
		TaskOperations taskOperationsNoLaunchers() {
			TaskOperations taskOperations = mock(TaskOperations.class);
			when(taskOperations.listPlatforms()).thenReturn(PagedModel.of(
					Collections.emptyList(), (PagedModel.PageMetadata) null));
			return taskOperations;
		}

		@Bean
		DataFlowOperations dataFlowOperations(TaskOperations taskOperations) {
			DataFlowOperations dataFlowOperations = mock(DataFlowOperations.class);
			when(dataFlowOperations.taskOperations()).thenReturn(taskOperations);
			return dataFlowOperations;
		}
	}

	@SpringBootApplication
	static class TaskLauncherFunctionTestApplication {
	}
}
