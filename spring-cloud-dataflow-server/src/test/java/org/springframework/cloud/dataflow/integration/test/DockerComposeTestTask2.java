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
package org.springframework.cloud.dataflow.integration.test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.integration.test.util.Wait;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.JobOperations;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.JobInstanceResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionStatus;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 *
 * @author Christian Tzolov
 */
public class DockerComposeTestTask2 {

	private static final Logger logger = LoggerFactory.getLogger(DockerComposeTestTask2.class);

	public static final int EXIT_CODE_SUCCESS = 0;
	public static final int EXIT_CODE_ERROR = 1;

	private static DataFlowTemplate dataFlowOperations;
	private Tasks tasks;

	@BeforeClass
	public static void beforeClass() {
		dataFlowOperations = new DataFlowTemplate(URI.create("http://localhost:9393"));
		logger.info("Configured platforms: " + dataFlowOperations.streamOperations().listPlatforms().stream()
				.map(d -> String.format("[%s:%s]", d.getName(), d.getType())).collect(Collectors.joining()));
	}

	@Before
	public void before() {
		Wait.on(dataFlowOperations.appRegistryOperations()).until(appRegistry ->
				appRegistry.list().getMetadata().getTotalElements() >= 68L);
		tasks = new Tasks(dataFlowOperations);
	}

	@After
	public void after() {
		dataFlowOperations.taskOperations().destroyAll();
	}

	// -----------------------------------------------------------------------
	//                               TASK TESTS
	// -----------------------------------------------------------------------
	@Test
	public void timestampTask() {
		logger.info("task-timestamp-test");

		try (Task task = tasks.builder()
				.name(randomTaskName())
				.definition("timestamp")
				.description("Test timestamp task")
				.create()) {

			// task first launch
			long id1 = task.launch();

			Wait.on(task).until(t -> t.executionStatus(id1) == TaskExecutionStatus.COMPLETE);
			assertThat(task.executions().size(), is(1));
			assertThat(task.execution(id1).get().getExitCode(), is(EXIT_CODE_SUCCESS));

			// task second launch
			long id2 = task.launch();

			Wait.on(task).until(t -> t.executionStatus(id2) == TaskExecutionStatus.COMPLETE);
			assertThat(task.executions().size(), is(2));
			assertThat(task.execution(id2).get().getExitCode(), is(EXIT_CODE_SUCCESS));

			// All
			task.executions().forEach(execution -> assertThat(execution.getExitCode(), is(EXIT_CODE_SUCCESS)));
		}
	}

	@Test
	public void composedTask() {
		try (Task task = tasks.builder()
				.name(randomTaskName())
				.definition("a: timestamp && b:timestamp")
				.description("Test composedTask")
				.create()) {

			assertThat("Two additional child tasks are created automatically",
					task.children().size(), is(2));

			// first launch
			logger.info("task-composed-task-runner-test: Single Launch");
			long firstExecutionId = task.launch();

			Wait.on(task).until(t -> t.executionStatus(firstExecutionId) == TaskExecutionStatus.COMPLETE);

			assertThat(task.executions().size(), is(1));
			assertThat(task.executionStatus(firstExecutionId), is(TaskExecutionStatus.COMPLETE));
			assertThat(task.execution(firstExecutionId).get().getExitCode(), is(EXIT_CODE_SUCCESS));

			task.children().forEach(childTask -> {
				assertThat(childTask.executions().size(), is(1));
				assertThat(childTask.executionByParentExecutionId(firstExecutionId).get().getExitCode(), is(EXIT_CODE_SUCCESS));
			});

			task.executions().forEach(execution -> assertThat(execution.getExitCode(), is(EXIT_CODE_SUCCESS)));

			// second launch
			logger.info("task-composed-task-runner-test: Multiple Launch");
			long secondExecutionId = task.launch();

			Wait.on(task).until(t -> t.executionStatus(secondExecutionId) == TaskExecutionStatus.ERROR);

			assertThat(task.executions().size(), is(2));
			assertThat(task.executionStatus(secondExecutionId), is(TaskExecutionStatus.ERROR));
			assertThat(task.execution(secondExecutionId).get().getExitCode(), is(EXIT_CODE_ERROR));

			task.children().forEach(childTask -> {
				assertThat("Child task shouldn't run due to parent failure",
						childTask.executions().size(), is(1));
				assertThat("Child task shouldn't run due to parent failure",
						childTask.executionByParentExecutionId(secondExecutionId).isPresent(), is(false));
			});

			assertThat(tasks.list().size(), is(3));
		}
		assertThat(tasks.list().size(), is(0));
	}

	@Test
	public void multipleComposedTaskWithArguments2() {
		try (Task task = tasks.builder()
				.name(randomTaskName())
				.definition("a: timestamp && b:timestamp")
				.description("Test multipleComposedTaskhWithArguments")
				.create()) {

			assertThat("Two additional child tasks are created automatically",
					task.children().size(), is(2));

			// first launch
			logger.info("task-multiple-composed-task-with-arguments-test: First Launch");
			List<String> arguments = Arrays.asList("--increment-instance-enabled=true");
			long firstExecutionId = task.launch(arguments);

			Wait.on(task).until(t -> t.executionStatus(firstExecutionId) == TaskExecutionStatus.COMPLETE);

			assertThat(task.executions().size(), is(1));
			assertThat(task.executionStatus(firstExecutionId), is(TaskExecutionStatus.COMPLETE));
			assertThat(task.execution(firstExecutionId).get().getExitCode(), is(EXIT_CODE_SUCCESS));

			task.children().forEach(childTask -> {
				assertThat(childTask.executions().size(), is(1));
				assertThat(childTask.executionByParentExecutionId(firstExecutionId).get().getExitCode(), is(EXIT_CODE_SUCCESS));
			});

			task.executions().forEach(execution -> assertThat(execution.getExitCode(), is(EXIT_CODE_SUCCESS)));

			// second launch
			logger.info("task-multiple-composed-task-with-arguments-test: Second Launch");
			long secondExecutionId = task.launch(arguments);

			Wait.on(task).until(t -> t.executionStatus(secondExecutionId) == TaskExecutionStatus.COMPLETE);

			assertThat(task.executions().size(), is(2));
			assertThat(task.executionStatus(secondExecutionId), is(TaskExecutionStatus.COMPLETE));
			assertThat(task.execution(secondExecutionId).get().getExitCode(), is(EXIT_CODE_SUCCESS));

			task.children().forEach(childTask -> {
				assertThat(childTask.executions().size(), is(2));
				assertThat(childTask.executionByParentExecutionId(secondExecutionId).get().getExitCode(), is(EXIT_CODE_SUCCESS));
			});

			assertThat(task.jobExecutionResources().size(), is(2));

			assertThat(tasks.list().size(), is(3));
		}
		assertThat(tasks.list().size(), is(0));
	}

	private String randomTaskName() {
		return "task-" + UUID.randomUUID().toString().substring(0, 10);
	}

	//
	// TASK DSL
	// TODO: Task DSL should go either to core (next to the Stream DSL) or in a separate IT test utils package.
	//
	public static class Tasks {
		private final DataFlowOperations dataFlowOperations;

		public Tasks(DataFlowOperations dataFlowOperations) {
			this.dataFlowOperations = dataFlowOperations;
		}

		public TaskBuilder builder() {
			return new TaskBuilder(dataFlowOperations.taskOperations(), dataFlowOperations.jobOperations());
		}

		public List<Task> list() {
			return this.dataFlowOperations.taskOperations().list().getContent().stream()
					.map(td -> new Task(td.getName(), this.dataFlowOperations.taskOperations(), this.dataFlowOperations.jobOperations()))
					.collect(Collectors.toList());
		}
	}

	public static class TaskBuilder {
		private TaskOperations taskOperations;
		private JobOperations jobOperations;

		TaskBuilder(TaskOperations taskOperations, JobOperations jobOperations) {
			this.taskOperations = taskOperations;
			this.jobOperations = jobOperations;
		}

		private String taskDefinitionName;
		private String taskDefinition;
		private String taskDescription;

		public TaskBuilder name(String name) {
			this.taskDefinitionName = name;
			return this;
		}

		public TaskBuilder definition(String taskDefinition) {
			this.taskDefinition = taskDefinition;
			return this;
		}

		public TaskBuilder description(String taskDescription) {
			this.taskDescription = taskDescription;
			return this;
		}

		public Task create() {
			TaskDefinitionResource td = this.taskOperations.create(this.taskDefinitionName, this.taskDefinition, this.taskDescription);
			return new Task(td.getName(), this.taskOperations, this.jobOperations);
		}
	}

	public static class Task implements AutoCloseable {
		private final TaskOperations taskOperations;
		private final String taskName;
		private JobOperations jobOperations;

		Task(String taskName, TaskOperations taskOperations, JobOperations jobOperations) {
			this.taskOperations = taskOperations;
			this.taskName = taskName;
			this.jobOperations = jobOperations;
		}

		public boolean isComposed() {
			return this.definitionResource().isComposed();
		}

		public long launch() {
			return this.taskOperations.launch(this.taskName, Collections.EMPTY_MAP, Collections.EMPTY_LIST, null);
		}

		public long launch(List<String> arguments) {
			return this.taskOperations.launch(this.taskName, Collections.EMPTY_MAP, arguments, null);
		}

		public void destroy() {
			this.taskOperations.destroy(this.taskName);
		}

		public Collection<TaskExecutionResource> executions() {
			return taskOperations.executionListByTaskName(this.taskName).getContent();
		}


		public Optional<TaskExecutionResource> execution(long executionId) {
			return this.executions().stream()
					.filter(Objects::nonNull)
					.filter(e -> e.getExecutionId() == executionId)
					.findFirst();
		}

		public Optional<TaskExecutionResource> executionByParentExecutionId(long parentExecutionId) {
			return this.executions().stream()
					.filter(Objects::nonNull)
					.filter(e -> e.getParentExecutionId() == parentExecutionId)
					.findFirst();
		}

		public TaskExecutionStatus executionStatus(long executionId) {
			return this.execution(executionId)
					.map(e -> e.getTaskExecutionStatus())
					.orElse(TaskExecutionStatus.UNKNOWN);
		}

		// TODO Decide whether we should use the XXX Resource domain classes, the SCT and SBatch domain classes or create new for the DSL
		//public Collection<TaskExecution> executions() {
		//	return taskOperations.executionListByTaskName(this.taskName).getContent().stream()
		//			.map(tr -> new TaskExecution(tr.getExecutionId(), tr.getExitCode(), tr.getTaskName(),
		//					tr.getStartTime(), tr.getEndTime(), tr.getExitMessage(), tr.getArguments(),
		//					tr.getErrorMessage(), tr.getExternalExecutionId(), tr.getParentExecutionId()))
		//			.collect(Collectors.toList());
		//}

		//public Optional<TaskExecution> execution(long executionId) {
		//	return this.executions().stream()
		//			.filter(Objects::nonNull)
		//			.filter(e -> e.getExecutionId() == executionId)
		//			.findFirst();
		//}

		//public Optional<TaskExecution> executionByParentExecutionId(long parentExecutionId) {
		//	return this.executions().stream()
		//			.filter(Objects::nonNull)
		//			.filter(e -> e.getParentExecutionId() == parentExecutionId)
		//			.findFirst();
		//}

		//public TaskExecutionStatus executionStatus(long executionId) {
		//	return this.execution(executionId)
		//			.map(this::toTaskExecutionStatus)
		//			.orElse(TaskExecutionStatus.UNKNOWN);
		//}

		//private TaskExecutionStatus toTaskExecutionStatus(TaskExecution te) {
		//
		//	if (te.getStartTime() == null) {
		//		return TaskExecutionStatus.UNKNOWN;
		//	}
		//	if (te.getEndTime() == null) {
		//		return TaskExecutionStatus.RUNNING;
		//	}
		//	else {
		//		return (te.getExitCode() == null) ? TaskExecutionStatus.RUNNING :
		//				((te.getExitCode() == 0) ? TaskExecutionStatus.COMPLETE : TaskExecutionStatus.ERROR);
		//	}
		//}

		public List<Task> children() {
			return !isComposed() ?
					new ArrayList<>() :
					this.taskOperations.list().getContent().stream()
							.filter(Objects::nonNull)
							.filter(t -> t.getName().startsWith(this.taskName + "-"))
							.map(t -> new Task(t.getName(), this.taskOperations, this.jobOperations))
							.collect(Collectors.toList());
		}

		public Collection<JobExecutionResource> jobExecutionResources() {
			return this.jobOperations.executionListByJobName(this.taskName).getContent();
		}

		public Collection<JobInstanceResource> jobInstanceResources() {
			return this.jobOperations.instanceList(this.taskName).getContent();
		}

		private TaskDefinitionResource definitionResource() {
			return this.taskOperations.list().getContent().stream()
					.filter(Objects::nonNull)
					.filter(t -> t.getName().equals(this.taskName))
					.findFirst().get();
		}

		@Override
		public void close() {
			destroy();
		}
	}

}
