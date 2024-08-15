/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.cloud.dataflow.shell.command;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.mockito.Mockito;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.cloud.dataflow.rest.client.dsl.task.TaskSchedule;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.server.controller.TaskSchedulerController;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.shell.ShellCommandRunner;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.shell.table.Table;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Helper methods for scheduler commands to execute in the shell.
 * <p/>
 * It should mimic the client side API of SchedulerOperations as much as possible.
 *
 * @author Daniel Serleg
 * @author Chris Bono
 * @author Corneil du Plessis
 */
public class TaskScheduleCommandTemplate {

	private SchedulerService schedule;

	private ShellCommandRunner commandRunner;

	public TaskScheduleCommandTemplate(ShellCommandRunner commandRunner, ApplicationContext applicationContext) {
		this.commandRunner = commandRunner;

		ConfigurableListableBeanFactory beanFactory = ((AnnotationConfigServletWebServerApplicationContext) applicationContext)
				.getBeanFactory();
		schedule = Mockito.mock(SchedulerService.class);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TaskSchedulerController.class);
		builder.addConstructorArgValue(schedule);
		DefaultListableBeanFactory listableBeanFactory = (DefaultListableBeanFactory) beanFactory;
		listableBeanFactory.setAllowBeanDefinitionOverriding(true);
		listableBeanFactory.registerBeanDefinition("taskSchedulerController", builder.getBeanDefinition());
	}

	public void create(String name, String definition, String expression, String properties, String args) throws IOException {
		String wholeCommand = String.format(
				"task schedule create --name \"%s\" --definitionName \"%s\" --expression \"%s\"", name, definition, expression);
		if (StringUtils.hasText(properties)) {
			wholeCommand += String.format(" --properties \"%s\"", properties);
		}
		if (StringUtils.hasText(args)) {
			wholeCommand += String.format(" --arguments \"%s\"", args);
		}
		Object result = commandRunner.executeCommand(wholeCommand);

		Map<String, String> expectedProperties = DeploymentPropertiesUtils.parseDeploymentProperties(properties, null, 0);
		expectedProperties.put(TaskSchedule.CRON_EXPRESSION_KEY, expression);

		List<String> expectedArgs = args != null ? Arrays.asList(args) : Collections.emptyList();

		verify(schedule).schedule(name, definition, expectedProperties, expectedArgs, null);
		assertThat(result).hasToString(String.format("Created schedule '%s'", name));
	}

	public void createWithPropertiesFile(String name, String definition, String expression, String propertiesFile, String args) throws IOException {
		String wholeCommand = String.format(
				"task schedule create --name \"%s\" --definitionName \"%s\" --expression \"%s\" --propertiesFile \"%s\" --arguments \"%s\"",
				name, definition, expression, propertiesFile, args);
		Object result = commandRunner.executeCommand(wholeCommand);

		Map<String, String> expectedProperties = DeploymentPropertiesUtils.parseDeploymentProperties("", new File(propertiesFile), 1);
		expectedProperties.put(TaskSchedule.CRON_EXPRESSION_KEY, expression);

		List<String> expectedArgs = args != null ? Arrays.asList(args) : Collections.emptyList();

		verify(schedule).schedule(name, definition, expectedProperties, expectedArgs, null);
		assertThat(result).hasToString(String.format("Created schedule '%s'", name));
	}

	public void createWithPropertiesAndPropertiesFile(String name, String definition, String expression, String properties, String propertiesFile, String args) {
		String wholeCommand = String.format(
				"task schedule create --name \"%s\" --definitionName \"%s\" --expression \"%s\" --properties \"%s\"  --propertiesFile \"%s\" --arguments \"%s\"",
				name, definition, expression, properties, propertiesFile, args);
		commandRunner.executeCommand(wholeCommand);
	}

	public void unschedule(String name) {
		assertThat(commandRunner.executeCommand(String.format("task schedule destroy --name \"%s\"", name)))
				.extracting(Object::toString)
				.isEqualTo(String.format("Deleted task schedule '%s'", name));
		verify(schedule).unschedule(name, null);
	}

	public void list() {
		ScheduleInfo scheduleInfo = new ScheduleInfo();
		scheduleInfo.setScheduleName("schedName");
		scheduleInfo.setTaskDefinitionName("testDefinition");
		scheduleInfo.setScheduleProperties(Collections.EMPTY_MAP);

		when(schedule.listForPlatform(null)).thenReturn(Arrays.asList(scheduleInfo));

		String wholeCommand = "task schedule list";
		Object result = commandRunner.executeCommand(wholeCommand);

		Table table = (Table) result;
		assertThat(table.getModel().getValue(1, 0)).isEqualTo("schedName");
	}

	public void listByTaskDefinition(String definitionName) {
		ScheduleInfo scheduleInfo = new ScheduleInfo();
		scheduleInfo.setScheduleName("schedName");
		scheduleInfo.setTaskDefinitionName("testDefinition");
		scheduleInfo.setScheduleProperties(Collections.EMPTY_MAP);

		when(schedule.list(definitionName, null)).thenReturn(Arrays.asList(scheduleInfo));

		String wholeCommand = String.format("task schedule list --definitionName %s", definitionName);
		Object result = commandRunner.executeCommand(wholeCommand);

		Table table = (Table) result;
		assertThat(table.getModel().getValue(1, 0)).isEqualTo("schedName");
	}
}
