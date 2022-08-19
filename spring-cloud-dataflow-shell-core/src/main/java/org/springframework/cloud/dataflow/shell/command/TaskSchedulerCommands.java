/*
 * Copyright 2019-2022 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.util.Strings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.SchedulerOperations;
import org.springframework.cloud.dataflow.rest.client.dsl.task.TaskSchedule;
import org.springframework.cloud.dataflow.rest.resource.ScheduleInfoResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.hateoas.PagedModel;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;

/**
 * Task Scheduler commands
 *
 * @author Daniel Serleg
 * @author Chris Bono
 */
@ShellComponent
public class TaskSchedulerCommands {

	private static final String PROPERTIES_OPTION = "properties";
	private static final String PROPERTIES_FILE_OPTION = "propertiesFile";

	private static final String SCHEDULER_CREATE = "task schedule create";

	private static final String SCHEDULER_LIST = "task schedule list";

	private static final String SCHEDULER_UNSCHEDULE = "task schedule destroy";

	@Autowired
	private DataFlowShell dataFlowShell;

	public Availability availableWithCreateRole() {
		return availabilityFor(RoleType.CREATE, OpsType.TASK);
	}

	public Availability availableWithListRole() {
		return availabilityFor(RoleType.VIEW, OpsType.TASK);
	}

	public Availability availableWithUnscheduleRole() {
		return availabilityFor(RoleType.DESTROY, OpsType.TASK);
	}

	private Availability availabilityFor(RoleType roleType, OpsType opsType) {
		return dataFlowShell.hasAccess(roleType, opsType)
				? Availability.available()
				: Availability.unavailable("you do not have permissions");
	}

	@ShellMethod(key = SCHEDULER_CREATE, value = "Create new task schedule")
	@ShellMethodAvailability("availableWithCreateRole")
	public String create(
			@ShellOption(help = "the name to give to the schedule") String name,
			@ShellOption(value = "--definitionName", help = "a task definition name") String definitionName,
			@ShellOption(help = "the cron expression of the schedule") String expression,
			@ShellOption(help = "a task properties (comma separated string eg.: --properties 'prop.first=prop,prop.sec=prop2'", defaultValue = ShellOption.NULL) String properties,
			@ShellOption(value = "--propertiesFile", help = "the properties for this deployment (as a File)", defaultValue = ShellOption.NULL) File propertiesFile,
			@ShellOption(help = "command line args (space separated string eg.: --arguments 'a b c d'", defaultValue = ShellOption.NULL) String arguments,
			@ShellOption(help = "the name of the platform from which to create the schedule", defaultValue = ShellOption.NULL) String platform) throws IOException {

		int which = Assertions.atMostOneOf(PROPERTIES_OPTION, properties, PROPERTIES_FILE_OPTION,
				propertiesFile);
		Map<String, String> propertiesToUse = DeploymentPropertiesUtils.parseDeploymentProperties(properties,
				propertiesFile, which);

		List<String> args = DeploymentPropertiesUtils.parseArgumentList(arguments, " ");
		propertiesToUse.put(TaskSchedule.CRON_EXPRESSION_KEY, expression);

		scheduleOperations().schedule(name, definitionName, propertiesToUse, args, platform);
		return String.format("Created schedule '%s'", name);
	}

	@ShellMethod(key =SCHEDULER_LIST, value = "List task schedules by task definition name")
	@ShellMethodAvailability("availableWithListRole")
	public Table listByDefinition(
			@ShellOption(help = "the name platform from which to retrieve a list of schedules", defaultValue = ShellOption.NULL) String platform,
			@ShellOption(value = "--definitionName", help = "the task definition name", defaultValue = ShellOption.NULL) String definitionName) {
		PagedModel<ScheduleInfoResource> schedules;
		if (Strings.isEmpty(definitionName)) {
			schedules = scheduleOperations().listByPlatform(platform);
		}
		else {
			schedules = scheduleOperations().list(definitionName, platform);
		}

		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("scheduleName", "Schedule Name");
		headers.put("taskDefinitionName", "Task Definition Name");
		headers.put("scheduleProperties", "Properties");
		final TableBuilder builder = new TableBuilder(new BeanListTableModel<>(schedules, headers));
		return DataFlowTables.applyStyle(builder).build();
	}

	@ShellMethod(key =SCHEDULER_UNSCHEDULE, value = "Delete task schedule")
	@ShellMethodAvailability("availableWithUnscheduleRole")
	public String unschedule(
			@ShellOption(help = "The name of the task schedule") String name,
			@ShellOption(help = "the name platform from which to unschedule", defaultValue = ShellOption.NULL) String platform) {
		scheduleOperations().unschedule(name, platform);
		return String.format("Deleted task schedule '%s'", name);
	}

	private SchedulerOperations scheduleOperations() {
		return dataFlowShell.getDataFlowOperations().schedulerOperations();
	}
}
