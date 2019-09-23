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

package org.springframework.cloud.dataflow.scheduler.launcher;


import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.rest.client.DataFlowClientException;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.LauncherResource;
import org.springframework.cloud.dataflow.scheduler.launcher.configuration.SchedulerTaskLauncher;
import org.springframework.cloud.dataflow.scheduler.launcher.configuration.SchedulerTaskLauncherException;
import org.springframework.cloud.dataflow.scheduler.launcher.configuration.SchedulerTaskLauncherProperties;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.core.env.Environment;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.mediatype.vnderrors.VndErrors;
import org.springframework.mock.env.MockEnvironment;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SchedulerTaskLauncherTests {

	private static final String TASK_NAME = "testTaskDefinition";

	private TaskOperations taskOperations;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setup() {
		Launcher launcher = new Launcher("default", "default", Mockito.mock(TaskLauncher.class), Mockito.mock(Scheduler.class));
		LauncherResource resource = new LauncherResource(launcher);
		this.taskOperations = Mockito.mock(TaskOperations.class);
		Mockito.when(this.taskOperations.listPlatforms()).thenReturn(new PagedModel(Collections.singletonList(resource), null,Collections.emptyList()));
	}

	@Test
	public void testFailedLaunch() {
		final String errorMessage = "ERROR MESSAGE";
		SchedulerTaskLauncherProperties schedulerTaskLauncherProperties = new SchedulerTaskLauncherProperties();
		schedulerTaskLauncherProperties.setTaskName(TASK_NAME);
		SchedulerTaskLauncher schedulerTaskLauncher = getSchedulerTaskLauncher(schedulerTaskLauncherProperties, new MockEnvironment());
		VndErrors vndErrors = new VndErrors(errorMessage, errorMessage, new Link("https://invalidlink"));
		Mockito.when(this.taskOperations.launch(Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any())).
				thenThrow(new DataFlowClientException(vndErrors));
		thrown.expect(SchedulerTaskLauncherException.class);
		thrown.expectMessage(errorMessage);

		schedulerTaskLauncher.launchTask();
	}

	@Test
	public void testValid() {
		SchedulerTaskLauncherProperties schedulerTaskLauncherProperties = new SchedulerTaskLauncherProperties();
		SchedulerTaskLauncher schedulerTaskLauncher = getSchedulerTaskLauncher(schedulerTaskLauncherProperties, new MockEnvironment());
		final ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

		schedulerTaskLauncher.launchTask();
		verify(this.taskOperations, times(1)).launch(argument.capture(), Mockito.any(), Mockito.any(), Mockito.any());
		Assert.assertEquals(TASK_NAME, argument.getAllValues().get(0));
	}

	@Test
	public void testTaskNameNotProvided() {
		SchedulerTaskLauncherProperties schedulerTaskLauncherProperties = new SchedulerTaskLauncherProperties();

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("`taskName` cannot be null");

		new SchedulerTaskLauncher(schedulerTaskLauncherProperties.getTaskName(),
				schedulerTaskLauncherProperties.getPlatformName(), taskOperations,
				schedulerTaskLauncherProperties, new MockEnvironment());
	}

	@Test
	public void testValidWithProperties() {
		final String propertyPrefix = "app.timestamp.timestamp";
		final ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
		MockEnvironment mockEnvironment = new MockEnvironment();
		SchedulerTaskLauncherProperties schedulerTaskLauncherProperties = new SchedulerTaskLauncherProperties();
		mockEnvironment.setProperty(String.format("%s.%s",
				schedulerTaskLauncherProperties.getTaskLauncherPropertyPrefix(),
				propertyPrefix), "YYYY" );
		SchedulerTaskLauncher schedulerTaskLauncher = getSchedulerTaskLauncher(schedulerTaskLauncherProperties, mockEnvironment);

		schedulerTaskLauncher.launchTask();
		verify(this.taskOperations, times(1)).launch(Mockito.any(), argument.capture(), Mockito.any(), Mockito.any());
		Assert.assertTrue(argument.getValue().containsKey(propertyPrefix));
		Assert.assertEquals("YYYY", argument.getValue().get(propertyPrefix));
	}

	private SchedulerTaskLauncher getSchedulerTaskLauncher(SchedulerTaskLauncherProperties schedulerTaskLauncherProperties,
			Environment environment) {
		schedulerTaskLauncherProperties.setTaskName(TASK_NAME);
		return new SchedulerTaskLauncher(schedulerTaskLauncherProperties.getTaskName(),
				schedulerTaskLauncherProperties.getPlatformName(), taskOperations,
				schedulerTaskLauncherProperties, environment);
	}

}
