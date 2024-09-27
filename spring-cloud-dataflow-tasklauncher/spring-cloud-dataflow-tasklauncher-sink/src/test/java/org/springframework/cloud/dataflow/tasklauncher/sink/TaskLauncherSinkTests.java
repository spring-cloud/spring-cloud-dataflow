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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.CurrentTaskExecutionsResource;
import org.springframework.cloud.dataflow.rest.resource.LaunchResponseResource;
import org.springframework.cloud.dataflow.rest.resource.LauncherResource;
import org.springframework.cloud.dataflow.tasklauncher.LaunchRequest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.task.batch.configuration.TaskBatchAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.PagedModel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author David Turanski
 **/
@SpringBootTest(classes = { TaskLauncherSinkTests.TestConfig.class },
	properties = {
		"spring.cloud.function.definition=launchRequestConsumer",
		"retry.initial-delay=100",
				"retry.max-period=3000", "retry.max-attempts=6"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class TaskLauncherSinkTests {

	private static final Logger logger = LoggerFactory.getLogger(TaskLauncherSinkTests.class);

	@Autowired
	ApplicationContext context;

	static class ErrorHandler implements MessageHandler {

		final AtomicInteger errorsReceived = new AtomicInteger(0);

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			try {
				logger.info("received:error:{}", message);
				errorsReceived.incrementAndGet();
			}
			catch (Exception e) {
				fail(e.toString());
			}
		}
		public boolean hasErrors() {
			return errorsReceived.get() > 0;
		}
	}
	@Test
	public void consumerPausesWhenMaxTaskExecutionsReached() {

		SubscribableChannel errorChannel = context.getBean("errorChannel", SubscribableChannel.class);
		ErrorHandler errorHandler = new ErrorHandler();
		errorChannel.subscribe(errorHandler);
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
		Awaitility.await("Error produced").until(errorHandler::hasErrors);
		long total = System.currentTimeMillis() - start;
		assertThat(total).isGreaterThan(600L);
		assertThat(total).isLessThan(1500L);
		assertThat(errorHandler.errorsReceived).hasValue(1);
	}

	@Test
	public void launchValidRequest() {

		SubscribableChannel errorChannel = context.getBean("errorChannel", SubscribableChannel.class);
		ErrorHandler errorHandler = new ErrorHandler();
		errorChannel.subscribe(errorHandler);
		CurrentTaskExecutionsResource resource = new CurrentTaskExecutionsResource();
		resource.setRunningExecutionCount(0);
		resource.setMaximumTaskExecutions(8);
		resource.setType("default");
		resource.setName("default");
		TaskOperations taskOperations = context.getBean(TaskOperations.class);
		when(taskOperations.currentTaskExecutions()).thenReturn(Collections.singletonList(resource));
		when(taskOperations.launch(anyString(), anyMap(), anyList()))
			.thenReturn(new LaunchResponseResource(1));
		InputDestination inputDestination = context.getBean(InputDestination.class);
		LaunchRequest launchRequest = new LaunchRequest("test", Collections.emptyList(), Collections.emptyMap());
		logger.info("sending:input={}", launchRequest);
		inputDestination.send(new GenericMessage<>(launchRequest));
		verify(taskOperations, times(1)).launch(anyString(), anyMap(), anyList());
		assertThat(errorHandler.hasErrors()).isFalse();
	}

	@Test
	public void launchRequestFailure() {


		SubscribableChannel errorChannel = context.getBean("errorChannel", SubscribableChannel.class);
		ErrorHandler errorHandler = new ErrorHandler();
		errorChannel.subscribe(errorHandler);

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
		Awaitility.await("Expecting error").until(errorHandler::hasErrors);
	}

	@SpringBootApplication(exclude = { BatchAutoConfiguration.class, TaskBatchAutoConfiguration.class,
			DataSourceAutoConfiguration.class })
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
