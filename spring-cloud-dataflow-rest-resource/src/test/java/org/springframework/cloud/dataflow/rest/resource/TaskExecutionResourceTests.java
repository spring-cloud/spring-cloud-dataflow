/*
 * Copyright 2018-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecutionRel;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.core.io.UrlResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Provides tests for the {@link TaskExecutionResourceTests} class.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
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
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, null);
		assertEquals(TaskExecutionStatus.UNKNOWN, taskExecutionResource.getTaskExecutionStatus());
	}

	@Test
	public void testTaskExecutionStatusWithRunningTaskExecution()  {
		final TaskExecution taskExecution = new TaskExecution();
		taskExecution.setStartTime(new Date());
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, null);
		assertEquals(TaskExecutionStatus.RUNNING, taskExecutionResource.getTaskExecutionStatus());
		assertNull(taskExecutionResource.getExitCode());
	}

	@Test
	public void testTaskExecutionStatusWithSuccessfulTaskExecution()  {
		final TaskExecution taskExecution = getDefaultTaskExecution();
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, null);
		assertEquals(TaskExecutionStatus.COMPLETE, taskExecutionResource.getTaskExecutionStatus());
	}

	@Test
	public void testCTRExecutionStatusWithSuccessfulJobExecution()  {
		final TaskExecution taskExecution = getDefaultTaskExecution();
		JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setExitStatus(ExitStatus.COMPLETED);
		TaskJobExecution taskJobExecution = new TaskJobExecution(taskExecution.getExecutionId(), jobExecution, true);
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, taskJobExecution);
		assertEquals(TaskExecutionStatus.COMPLETE, taskExecutionResource.getTaskExecutionStatus());
	}

	@Test
	public void testCTRExecutionStatusWithFailedJobExecution()  {
		final TaskExecution taskExecution = new TaskExecution();
		taskExecution.setStartTime(new Date());
		taskExecution.setEndTime(new Date());
		taskExecution.setExitCode(0);
		JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setExitStatus(ExitStatus.FAILED);
		TaskJobExecution taskJobExecution = new TaskJobExecution(taskExecution.getExecutionId(), jobExecution, true);
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, taskJobExecution);
		assertEquals(TaskExecutionStatus.ERROR, taskExecutionResource.getTaskExecutionStatus());
	}

	@Test
	public void testTaskExecutionStatusWithFailedTaskExecution()  {
		final TaskExecution taskExecution = new TaskExecution();
		taskExecution.setStartTime(new Date());
		taskExecution.setEndTime(new Date());
		taskExecution.setExitCode(123);
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, null);
		assertEquals(TaskExecutionStatus.ERROR, taskExecutionResource.getTaskExecutionStatus());
	}

	@Test
	public void testTaskExecutionForTaskExecutionRel() throws Exception{
		final TaskExecution taskExecution = getDefaultTaskExecution();
		TaskManifest taskManifest = new TaskManifest();
		taskManifest.setPlatformName("testplatform");
		taskManifest.setTaskDeploymentRequest(new AppDeploymentRequest(new AppDefinition("testapp", Collections.emptyMap()), new UrlResource("http://foo")));
		TaskJobExecutionRel taskJobExecutionRel = new TaskJobExecutionRel(taskExecution, new ArrayList<>(), taskManifest, null);
		TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskJobExecutionRel);
		assertEquals("testplatform", taskExecutionResource.getPlatformName());
		assertEquals(TaskExecutionStatus.COMPLETE, taskExecutionResource.getTaskExecutionStatus());
		taskJobExecutionRel = new TaskJobExecutionRel(taskExecution, new ArrayList<>(), null, null);
		taskExecutionResource = new TaskExecutionResource(taskJobExecutionRel);
		assertNull(taskExecutionResource.getPlatformName());
		assertEquals(TaskExecutionStatus.COMPLETE, taskExecutionResource.getTaskExecutionStatus());
		JobExecution jobExecution = new JobExecution(1L, null, "foo");
		jobExecution.setExitStatus(ExitStatus.FAILED);

		TaskJobExecution ctrTaskJobExecution = new TaskJobExecution(1, jobExecution, true);
		taskJobExecutionRel = new TaskJobExecutionRel(taskExecution, new ArrayList<>(), null, ctrTaskJobExecution);
		taskExecutionResource = new TaskExecutionResource(taskJobExecutionRel);
		assertNull(taskExecutionResource.getPlatformName());
		assertEquals(TaskExecutionStatus.ERROR, taskExecutionResource.getTaskExecutionStatus());
		jobExecution.setExitStatus(ExitStatus.COMPLETED);
		ctrTaskJobExecution = new TaskJobExecution(1, jobExecution, true);
		taskJobExecutionRel = new TaskJobExecutionRel(taskExecution, new ArrayList<>(), null, ctrTaskJobExecution);
		taskExecutionResource = new TaskExecutionResource(taskJobExecutionRel);
		assertNull(taskExecutionResource.getPlatformName());
		assertEquals(TaskExecutionStatus.COMPLETE, taskExecutionResource.getTaskExecutionStatus());
	}

	private TaskExecution getDefaultTaskExecution() {
		final TaskExecution taskExecution = new TaskExecution();
		taskExecution.setStartTime(new Date());
		taskExecution.setEndTime(new Date());
		taskExecution.setExitCode(0);
		return taskExecution;
	}

}
