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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecutionRel;
import org.springframework.cloud.dataflow.schema.AppBootSchemaVersion;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.UrlResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provides tests for the {@link TaskExecutionResourceTests} class.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
public class TaskExecutionResourceTests {

	@Test
	public void testTaskExecutionStatusWithNoTaskExecutionSet() {
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource();
		assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.UNKNOWN);
	}

	@Test
	public void testTaskExecutionStatusWithNoStartTime() {
		for (AppBootSchemaVersion version : AppBootSchemaVersion.values()) {
			SchemaVersionTarget target = SchemaVersionTarget.createDefault(version);
			final AggregateTaskExecution taskExecution = new AggregateTaskExecution();
			taskExecution.setSchemaTarget(target.getName());
			final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, null);
			assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.UNKNOWN);
		}
	}

	@Test
	public void testTaskExecutionStatusWithRunningTaskExecution() {
		for (AppBootSchemaVersion version : AppBootSchemaVersion.values()) {
			SchemaVersionTarget target = SchemaVersionTarget.createDefault(version);
			final AggregateTaskExecution taskExecution = new AggregateTaskExecution();
			taskExecution.setSchemaTarget(target.getName());
			taskExecution.setStartTime(new Date());
			final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, null);
			assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.RUNNING);
			assertThat(taskExecutionResource.getExitCode()).isNull();
		}
	}

	@Test
	public void testTaskExecutionStatusWithSuccessfulTaskExecution() {
		for (AppBootSchemaVersion version : AppBootSchemaVersion.values()) {
			SchemaVersionTarget target = SchemaVersionTarget.createDefault(version);
			final AggregateTaskExecution taskExecution = getDefaultTaskExecution(target.getName());
			final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, null);
			assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.COMPLETE);
		}
	}

	@Test
	public void testCTRExecutionStatusWithSuccessfulJobExecution() {
		for (AppBootSchemaVersion version : AppBootSchemaVersion.values()) {
			SchemaVersionTarget target = SchemaVersionTarget.createDefault(version);
			final AggregateTaskExecution taskExecution = getDefaultTaskExecution(target.getName());
			JobExecution jobExecution = new JobExecution(1L);
			jobExecution.setExitStatus(ExitStatus.COMPLETED);
			TaskJobExecution taskJobExecution = new TaskJobExecution(taskExecution.getExecutionId(), jobExecution, true, target.getName());
			final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, taskJobExecution);
			assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.COMPLETE);

		}
	}

	@Test
	public void testCTRExecutionStatusWithFailedJobExecution() {
		final AggregateTaskExecution taskExecution = new AggregateTaskExecution();
		taskExecution.setStartTime(new Date());
		taskExecution.setEndTime(new Date());
		taskExecution.setExitCode(0);
		JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setExitStatus(ExitStatus.FAILED);
		final String defaultSchemaTarget = SchemaVersionTarget.defaultTarget().getName();
		TaskJobExecution taskJobExecution = new TaskJobExecution(taskExecution.getExecutionId(), jobExecution, true, defaultSchemaTarget);
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, taskJobExecution);
		assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.ERROR);
	}

	@Test
	public void testTaskExecutionStatusWithFailedTaskExecution() {
		final AggregateTaskExecution taskExecution = new AggregateTaskExecution();
		taskExecution.setStartTime(new Date());
		taskExecution.setEndTime(new Date());
		taskExecution.setExitCode(123);
		final TaskExecutionResource taskExecutionResource = new TaskExecutionResource(taskExecution, null);
		assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.ERROR);
	}

	@Test
	public void testTaskExecutionForTaskExecutionRel() throws Exception {
		for (AppBootSchemaVersion version : AppBootSchemaVersion.values()) {
			SchemaVersionTarget target = SchemaVersionTarget.createDefault(version);

			final AggregateTaskExecution taskExecution = getDefaultTaskExecution(target.getName());
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
			JobExecution jobExecution = new JobExecution(1L, null, "foo");
			jobExecution.setExitStatus(ExitStatus.FAILED);

			TaskJobExecution ctrTaskJobExecution = new TaskJobExecution(1, jobExecution, true, target.getName());
			taskJobExecutionRel = new TaskJobExecutionRel(taskExecution, new ArrayList<>(), null, ctrTaskJobExecution);
			taskExecutionResource = new TaskExecutionResource(taskJobExecutionRel);
			assertThat(taskExecutionResource.getPlatformName()).isNull();
			assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.ERROR);
			jobExecution.setExitStatus(ExitStatus.COMPLETED);
			ctrTaskJobExecution = new TaskJobExecution(1, jobExecution, true, target.getName());
			taskJobExecutionRel = new TaskJobExecutionRel(taskExecution, new ArrayList<>(), null, ctrTaskJobExecution);
			taskExecutionResource = new TaskExecutionResource(taskJobExecutionRel);
			assertThat(taskExecutionResource.getPlatformName()).isNull();
			assertThat(taskExecutionResource.getTaskExecutionStatus()).isEqualTo(TaskExecutionStatus.COMPLETE);
		}
	}

	private AggregateTaskExecution getDefaultTaskExecution(String schemaTarget) {
		if(!StringUtils.hasText(schemaTarget)) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		final AggregateTaskExecution taskExecution = new AggregateTaskExecution();
		taskExecution.setStartTime(new Date());
		taskExecution.setEndTime(new Date());
		taskExecution.setExitCode(0);
		taskExecution.setSchemaTarget(schemaTarget);
		return taskExecution;
	}

}
