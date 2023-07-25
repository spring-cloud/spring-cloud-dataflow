/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner;

import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 */
public class ComposedTaskStepExecutionListenerTests {

	private TaskExplorerContainer taskExplorerContainer;
	private TaskExplorer taskExplorer;

	private StepExecution stepExecution;

	private ComposedTaskStepExecutionListener taskListener;

	@BeforeEach
	public void setup() {
		this.taskExplorer = mock(TaskExplorer.class);
		this.taskExplorerContainer = new TaskExplorerContainer(Collections.emptyMap(), taskExplorer);
		this.stepExecution = getStepExecution();
		this.taskListener = new ComposedTaskStepExecutionListener(this.taskExplorerContainer);
	}

	@Test
	public void testSuccessfulRun() {
		TaskExecution taskExecution = getDefaultTaskExecution(0, null);
		when(this.taskExplorer.getTaskExecution(anyLong())).thenReturn(taskExecution);
		populateExecutionContext(taskExecution.getTaskName(),111L, SchemaVersionTarget.defaultTarget().getName());
		assertThat(this.taskListener.afterStep(this.stepExecution)).isEqualTo(ExitStatus.COMPLETED);
	}

	@Test
	public void testExitMessageRunSuccess() {
		ExitStatus expectedTaskStatus = new ExitStatus("TEST_EXIT_MESSAGE");
		TaskExecution taskExecution = getDefaultTaskExecution(0,
				expectedTaskStatus.getExitCode());
		when(this.taskExplorer.getTaskExecution(anyLong())).thenReturn(taskExecution);
		populateExecutionContext(taskExecution.getTaskName(), 111L, SchemaVersionTarget.defaultTarget().getName());

		assertThat(this.taskListener.afterStep(this.stepExecution)).isEqualTo(expectedTaskStatus);
	}

	@Test
	public void testExitMessageRunFail() {
		ExitStatus expectedTaskStatus = new ExitStatus("TEST_EXIT_MESSAGE");
		TaskExecution taskExecution = getDefaultTaskExecution(1,
				expectedTaskStatus.getExitCode());
		when(this.taskExplorer.getTaskExecution(anyLong())).thenReturn(taskExecution);
		populateExecutionContext(taskExecution.getTaskName(), 111L, SchemaVersionTarget.defaultTarget().getName());

		assertThat(this.taskListener.afterStep(this.stepExecution)).isEqualTo(expectedTaskStatus);
	}

	@Test
	public void testFailedRun() {
		TaskExecution taskExecution = getDefaultTaskExecution(1, null);
		when(this.taskExplorer.getTaskExecution(anyLong())).thenReturn(taskExecution);
		populateExecutionContext(taskExecution.getTaskName(), 111L, SchemaVersionTarget.defaultTarget().getName());

		assertThat(this.taskListener.afterStep(this.stepExecution)).isEqualTo(ExitStatus.FAILED);
	}

//	@Test(IllegalArgumentException.class)
//	public void testNullExecutionId() {
//		TaskExecution taskExecution = new TaskExecution();
//		when(this.taskExplorer.getTaskExecution(anyLong())).thenReturn(taskExecution);
//		populateExecutionContext(null);
//		this.taskListener.afterStep(this.stepExecution);
//	}

	private StepExecution getStepExecution() {
		final long JOB_EXECUTION_ID = 123L;
		final String STEP_NAME = "myTestStep";

		JobExecution jobExecution = new JobExecution(JOB_EXECUTION_ID);
		return new StepExecution(STEP_NAME, jobExecution);
	}

	private void populateExecutionContext(String taskName, Long taskExecutionId, String schemaTarget) {
		this.stepExecution.getExecutionContext().put("task-name", taskName);
		this.stepExecution.getExecutionContext().put("task-execution-id", taskExecutionId);
		this.stepExecution.getExecutionContext().put("schema-target", schemaTarget);
	}

	private TaskExecution getDefaultTaskExecution (int exitCode,
			String exitMessage) {
		TaskExecution taskExecution = new TaskExecution();
		taskExecution.setTaskName("test-ctr");
		taskExecution.setExitMessage(exitMessage);
		taskExecution.setExitCode(exitCode);
		taskExecution.setEndTime(new Date());
		return taskExecution;
	}
}
