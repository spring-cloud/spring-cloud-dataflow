/*
 * Copyright 2018-2023 the original author or authors.
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecutionRel;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.core.io.UrlResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provides tests for the {@link TaskExecutionResourceTests} class.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
class TaskExecutionResourceTests {

	@Test
	void taskExecutionStatusWithNoTaskExecutionSet() {
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource();
		assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.UNKNOWN);
	}

	@Test
	void taskExecutionStatusWithNoStartTime() {
		final TaskExecution taskExecution = new TaskExecution();
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, null);
		assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.UNKNOWN);
	}

	@Test
	void taskExecutionStatusWithRunningTaskExecution() {
		final TaskExecution taskExecution = new TaskExecution();
		taskExecution.setStartTime(LocalDateTime.now());
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, null);
		assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.RUNNING);
		assertThat(taskExecutionResource.getExitCode()).isNull();
	}

	@Test
	void taskExecutionStatusWithSuccessfulTaskExecution() {
		final TaskExecution taskExecution = getDefaultTaskExecution();
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, null);
		assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.COMPLETE);
	}

	@Test
	void ctrExecutionStatusWithSuccessfulJobExecution() {
		final TaskExecution taskExecution = getDefaultTaskExecution();
		JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setExitStatus(ExitStatus.COMPLETED);
		TaskJobExecution taskJobExecution = new TaskJobExecution(taskExecution.getExecutionId(), jobExecution, true);
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, taskJobExecution);
		assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.COMPLETE);
	}

	@Test
	void ctrExecutionStatusWithFailedJobExecution() {
		final TaskExecution taskExecution = new TaskExecution();
		taskExecution.setStartTime(LocalDateTime.now());
		taskExecution.setEndTime(LocalDateTime.now());
		taskExecution.setExitCode(0);
		JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setExitStatus(ExitStatus.FAILED);
		TaskJobExecution taskJobExecution = new TaskJobExecution(taskExecution.getExecutionId(), jobExecution, true);
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, taskJobExecution);
		assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.ERROR);
	}

	@Test
	void taskExecutionStatusWithFailedTaskExecution() {
		final TaskExecution taskExecution = new TaskExecution();
		taskExecution.setStartTime(LocalDateTime.now());
		taskExecution.setEndTime(LocalDateTime.now());
		taskExecution.setExitCode(123);
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, null);
		assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.ERROR);
	}

	@Test
	void taskExecutionForTaskExecutionRel() throws Exception {

		TaskExecution taskExecution = getDefaultTaskExecution();
		TaskManifest taskManifest = new TaskManifest();
		taskManifest.setPlatformName("testplatform");
		taskManifest.setTaskDeploymentRequest(new AppDeploymentRequest(new AppDefinition("testapp", Collections.emptyMap()), new UrlResource("http://foo")));
		TaskJobExecutionRel taskJobExecutionRel = new TaskJobExecutionRel(taskExecution, new ArrayList<>(), taskManifest, null);
		TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskJobExecutionRel);
		assertThat(taskExecutionResource.getPlatformName()).isEqualTo("testplatform");
		assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.COMPLETE);
		taskJobExecutionRel = new TaskJobExecutionRel(taskExecution, new ArrayList<>(), null, null);
		taskExecutionResource = new TaskExecutionResource(taskJobExecutionRel);
		assertThat(taskExecutionResource.getPlatformName()).isNull();
		assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.COMPLETE);
		JobExecution jobExecution = new JobExecution(1L, new JobParameters());
		jobExecution.setExitStatus(ExitStatus.FAILED);

		TaskJobExecution ctrTaskJobExecution = new TaskJobExecution(1, jobExecution, true);
		taskJobExecutionRel = new TaskJobExecutionRel(taskExecution, new ArrayList<>(), null, ctrTaskJobExecution);
		taskExecutionResource = new TaskExecutionResource(taskJobExecutionRel);
		assertThat(taskExecutionResource.getPlatformName()).isNull();
		assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.ERROR);
		jobExecution.setExitStatus(ExitStatus.COMPLETED);
		ctrTaskJobExecution = new TaskJobExecution(1, jobExecution, true);
		taskJobExecutionRel = new TaskJobExecutionRel(taskExecution, new ArrayList<>(), null, ctrTaskJobExecution);
		taskExecutionResource = new TaskExecutionResource(taskJobExecutionRel);
		assertThat(taskExecutionResource.getPlatformName()).isNull();
		assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.COMPLETE);
	}

	private TaskExecution getDefaultTaskExecution() {
		final TaskExecution taskExecution = new TaskExecution();
		taskExecution.setStartTime(LocalDateTime.now());
		taskExecution.setEndTime(LocalDateTime.now());
		taskExecution.setExitCode(0);
		return taskExecution;
	}

}
