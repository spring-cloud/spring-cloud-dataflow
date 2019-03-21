/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.resource;

import java.util.Date;

import org.junit.Test;

import org.springframework.cloud.task.repository.TaskExecution;

import static org.junit.Assert.assertEquals;

/**
 * Provides tests for the {@link TaskExecutionResourceTests} class.
 *
 * @author Gunnar Hillert
 */
public class TaskExecutionResourceTests {

	@Test
	public void testTaskExecutionStatusWithNoTaskExecutionSet()  {
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource();
		assertEquals(TaskExecutionStatus.UNKNOWN, taskExecutionResource.getTaskExecutionStatus());
	}

	@Test
	public void testTaskExecutionStatusWithNoStartTime()  {
		final TaskExecution taskExecution = new TaskExecution();
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution);
		assertEquals(TaskExecutionStatus.UNKNOWN, taskExecutionResource.getTaskExecutionStatus());
	}

	@Test
	public void testTaskExecutionStatusWithRunningTaskExecution()  {
		final TaskExecution taskExecution = new TaskExecution();
		taskExecution.setStartTime(new Date());
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution);
		assertEquals(TaskExecutionStatus.RUNNING, taskExecutionResource.getTaskExecutionStatus());
	}

	@Test
	public void testTaskExecutionStatusWithSuccessfulTaskExecution()  {
		final TaskExecution taskExecution = new TaskExecution();
		taskExecution.setStartTime(new Date());
		taskExecution.setEndTime(new Date());
		taskExecution.setExitCode(0);
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution);
		assertEquals(TaskExecutionStatus.COMPLETE, taskExecutionResource.getTaskExecutionStatus());
	}

	@Test
	public void testTaskExecutionStatusWithFailedTaskExecution()  {
		final TaskExecution taskExecution = new TaskExecution();
		taskExecution.setStartTime(new Date());
		taskExecution.setEndTime(new Date());
		taskExecution.setExitCode(123);
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution);
		assertEquals(TaskExecutionStatus.ERROR, taskExecutionResource.getTaskExecutionStatus());
	}

}
