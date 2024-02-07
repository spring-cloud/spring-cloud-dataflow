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

package org.springframework.cloud.dataflow.tasklauncher.sink;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.CurrentTaskExecutionsResource;
import org.springframework.cloud.dataflow.rest.resource.LaunchResponseResource;
import org.springframework.cloud.dataflow.rest.resource.LauncherResource;
import org.springframework.cloud.dataflow.tasklauncher.LaunchRequest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.PagedModel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David Turanski
 **/
@SpringBootTest(classes = { TaskLauncherSinkTests.TestConfig.class },
		properties = { "spring.cloud.function.definition=launchRequestConsumer", "spring.cloud.stream.bindings.launchRequestConsumer.consumer.retry-template-name=launchRequestConsumerRetry",
				"retry.initial-delay=100" })
public class TaskLauncherSinkTests {

	private static final Logger logger = LoggerFactory.getLogger(TaskLauncherSinkTests.class);

	@Autowired
	ApplicationContext context;
	@Test
	public void consumerPausesWhenMaxTaskExecutionsReached() {
		AtomicBoolean errorReceived = new AtomicBoolean();
		errorReceived.set(false);
		SubscribableChannel errorChannel = context.getBean("errorChannel", SubscribableChannel.class);
		errorChannel.subscribe(message -> {
			try {
				logger.info("received:error:{}", message);
				errorReceived.set(true);
			}
			catch (Exception e) {
				fail(e.toString());
			}
		});
		CurrentTaskExecutionsResource resource = new CurrentTaskExecutionsResource();
		resource.setRunningExecutionCount(8);
		resource.setMaximumTaskExecutions(8);
		resource.setType("default");
		resource.setName("default");
		TaskOperations taskOperations = context.getBean(TaskOperations.class);
		when(taskOperations.currentTaskExecutions()).thenReturn(Collections.singletonList(resource));

		LaunchRequest launchRequest = new LaunchRequest("test", Collections.emptyList(), Collections.emptyMap());
		InputDestination inputDestination = context.getBean(InputDestination.class);
		logger.info("sending:input={}", launchRequest);
		long start = System.currentTimeMillis();
		inputDestination.send(new GenericMessage<>(launchRequest));
		Awaitility.await("Error produced").until(errorReceived::get);
		long total = System.currentTimeMillis() - start;
		assertThat(total).isGreaterThan(600L);
	}

	@Test
	public void launchValidRequest() {
		AtomicBoolean errorReceived = new AtomicBoolean();
		errorReceived.set(false);
		SubscribableChannel errorChannel = context.getBean("errorChannel", SubscribableChannel.class);
		errorChannel.subscribe(message -> {
			try {
				logger.info("received:error:{}", message);
				errorReceived.set(true);
			}
			catch (Exception e) {
				fail(e.toString());
			}
		});
		CurrentTaskExecutionsResource resource = new CurrentTaskExecutionsResource();
		resource.setRunningExecutionCount(0);
		resource.setMaximumTaskExecutions(8);
		resource.setType("default");
		resource.setName("default");
		TaskOperations taskOperations = context.getBean(TaskOperations.class);
		when(taskOperations.currentTaskExecutions()).thenReturn(Collections.singletonList(resource));
		when(taskOperations.launch(anyString(), anyMap(), anyList()))
			.thenReturn(new LaunchResponseResource(1, "boot3"));
		InputDestination inputDestination = context.getBean(InputDestination.class);
		LaunchRequest launchRequest = new LaunchRequest("test", Collections.emptyList(), Collections.emptyMap());
		logger.info("sending:input={}", launchRequest);
		inputDestination.send(new GenericMessage<>(launchRequest));
		Awaitility.await("Expecting no errors")
			.failFast(errorReceived::get)
			.pollDelay(Duration.ofSeconds(2))
			.timeout(Duration.ofSeconds(5))
			.until(() -> !errorReceived.get());
	}

	@Test
	public void launchRequestFailure() {

		AtomicBoolean errorReceived = new AtomicBoolean();
		errorReceived.set(false);
		SubscribableChannel errorChannel = context.getBean("errorChannel", SubscribableChannel.class);
		errorChannel.subscribe(message -> {
			try {
				logger.info("received:error:{}", message);
				errorReceived.set(true);
			}
			catch (Exception e) {
				fail(e.toString());
			}
		});
		CurrentTaskExecutionsResource resource = new CurrentTaskExecutionsResource();
		resource.setRunningExecutionCount(0);
		resource.setMaximumTaskExecutions(8);
		resource.setType("default");
		resource.setName("default");
		TaskOperations taskOperations = context.getBean(TaskOperations.class);
		when(taskOperations.currentTaskExecutions()).thenReturn(Collections.singletonList(resource));
		when(taskOperations.launch(anyString(), anyMap(), anyList())).thenThrow(new RuntimeException("Cannot launch"));
		InputDestination inputDestination = context.getBean(InputDestination.class);
		LaunchRequest launchRequest = new LaunchRequest("test", Collections.emptyList(), Collections.emptyMap());
		logger.info("sending:input={}", launchRequest);
		inputDestination.send(new GenericMessage<>(launchRequest));
		Awaitility.await("Expecting error").until(errorReceived::get);
	}


	@SpringBootApplication
	@Import({ TestChannelBinderConfiguration.class, TaskLauncherSinkConfiguration.class })
	static class TestConfig {
		@Bean
		DataFlowOperations dataFlowOperations(TaskOperations taskOperations) {
			DataFlowOperations dataFlowOperations = mock(DataFlowOperations.class);
			when(dataFlowOperations.taskOperations()).thenReturn(taskOperations);
			return dataFlowOperations;
		}
		@Bean
		TaskOperations taskOperations() {
			TaskOperations taskOperations = mock(TaskOperations.class);
			List<LauncherResource> launcherResources = new ArrayList<>();
			LauncherResource launcherResource0 = mock(LauncherResource.class);
			when(launcherResource0.getName()).thenReturn("default");
			LauncherResource launcherResource1 = mock(LauncherResource.class);
			when(launcherResource1.getName()).thenReturn("other");

			LauncherResource launcherResource = mock(LauncherResource.class);
			when(launcherResource.getName()).thenReturn("default");

			launcherResources.add(launcherResource0);
			launcherResources.add(launcherResource1);

			when(taskOperations.listPlatforms())
				.thenReturn(PagedModel.of(launcherResources, (PagedModel.PageMetadata) null));

			return taskOperations;
		}
	}
}
