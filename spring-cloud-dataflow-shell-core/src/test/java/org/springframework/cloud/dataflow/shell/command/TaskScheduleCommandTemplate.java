package org.springframework.cloud.dataflow.shell.command;

import java.util.Arrays;
import java.util.Collections;

import org.mockito.Mockito;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.cloud.dataflow.server.controller.TaskSchedulerController;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.shell.table.Table;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TaskScheduleCommandTemplate {

	private SchedulerService schedule;

	private JLineShellComponent dataFlowShell;

	public TaskScheduleCommandTemplate(JLineShellComponent dataFlowShell, ApplicationContext applicationContext) {
		this.dataFlowShell = dataFlowShell;

		ConfigurableListableBeanFactory beanFactory = ((AnnotationConfigServletWebServerApplicationContext) applicationContext)
				.getBeanFactory();
		schedule = Mockito.mock(SchedulerService.class);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(TaskSchedulerController.class);
		builder.addConstructorArgValue(schedule);
		DefaultListableBeanFactory listableBeanFactory = (DefaultListableBeanFactory) beanFactory;
		listableBeanFactory.setAllowBeanDefinitionOverriding(true);
		listableBeanFactory.registerBeanDefinition("taskSchedulerController", builder.getBeanDefinition());
	}

	public void create(String name, String definition, String expression, String properties, String args) {
		String wholeCommand = String.format(
				"task schedule create --name \"%s\" --definitionName \"%s\" --expression \"%s\" --properties \"%s\" --arguments \"%s\"",
				name, definition, expression, properties, args);
		CommandResult cr = dataFlowShell.executeCommand(wholeCommand);
		verify(schedule).schedule(name, definition, Collections.singletonMap("scheduler.cron.expression", "* * * * *"),
				Collections.singletonList("[]"));
		assertEquals("Created schedule 'schedName'", cr.getResult());
	}

	public void unschedule(String name) {
		String wholeCommand = String.format("task schedule destroy --name \"%s\"", name);
		CommandResult cr = dataFlowShell.executeCommand(wholeCommand);
		verify(schedule).unschedule(name);
		assertEquals("Deleted task schedule 'schedName'", cr.getResult());
	}

	public void list() {
		ScheduleInfo scheduleInfo = new ScheduleInfo();
		scheduleInfo.setScheduleName("schedName");
		scheduleInfo.setTaskDefinitionName("testDefinition");
		scheduleInfo.setScheduleProperties(Collections.EMPTY_MAP);

		when(schedule.list()).thenReturn(Arrays.asList(scheduleInfo));

		String wholeCommand = "task schedule list";
		CommandResult cr = dataFlowShell.executeCommand(wholeCommand);

		Table table = (Table) cr.getResult();
		assertEquals("schedName", table.getModel().getValue(1, 0));
	}

	public void listByTaskDefinition(String definitionName) {
		ScheduleInfo scheduleInfo = new ScheduleInfo();
		scheduleInfo.setScheduleName("schedName");
		scheduleInfo.setTaskDefinitionName("testDefinition");
		scheduleInfo.setScheduleProperties(Collections.EMPTY_MAP);

		when(schedule.list(definitionName)).thenReturn(Arrays.asList(scheduleInfo));

		String wholeCommand = String.format("task schedule list --definitionName %s", definitionName);
		CommandResult cr = dataFlowShell.executeCommand(wholeCommand);

		Table table = (Table) cr.getResult();
		assertEquals("schedName", table.getModel().getValue(1, 0));
	}
}
