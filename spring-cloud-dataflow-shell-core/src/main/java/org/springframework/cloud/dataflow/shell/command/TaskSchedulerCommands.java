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

package org.springframework.cloud.dataflow.shell.command;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.util.Strings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.SchedulerOperations;
import org.springframework.cloud.dataflow.rest.resource.ScheduleInfoResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.hateoas.PagedModel;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.stereotype.Component;

/**
 * Task Scheduler commands
 *
 * @author Daniel Serleg
 */
@Component
public class TaskSchedulerCommands implements CommandMarker {

	private static final String SCHEDULER_CREATE = "task schedule create";

	private static final String SCHEDULER_LIST = "task schedule list";

	private static final String SCHEDULER_UNSCHEDULE = "task schedule destroy";

	@Autowired
	private DataFlowShell dataFlowShell;

	@CliAvailabilityIndicator({ SCHEDULER_CREATE })
	public boolean availableWithCreateRole() {
		return dataFlowShell.hasAccess(RoleType.CREATE, OpsType.TASK);
	}

	@CliAvailabilityIndicator({ SCHEDULER_LIST })
	public boolean availableWithListRole() {
		return dataFlowShell.hasAccess(RoleType.VIEW, OpsType.TASK);
	}

	@CliAvailabilityIndicator({ SCHEDULER_UNSCHEDULE })
	public boolean availableWithUnscheduleRole() {
		return dataFlowShell.hasAccess(RoleType.DESTROY, OpsType.TASK);
	}

	@CliCommand(value = SCHEDULER_CREATE, help = "Create new task schedule")
	public String create(
			@CliOption(mandatory = true, key = { "name" }, help = "the name to give to the schedule") String name,
			@CliOption(mandatory = true, key = {
					"definitionName" }, help = "a task definition name") String definitionName,
			@CliOption(mandatory = true, key = {
					"expression" }, help = "the cron expression of the schedule") String expression,
			@CliOption(key = {
					"properties" }, help = "a task properties (coma separated string eg.: --properties 'prop.first=prop,prop.sec=prop2'") String properties,
			@CliOption(key = {
					"arguments" }, help = "command line args (space separated string eg.: --arguments 'a b c d'") String arguments,
			@CliOption(key = { "platform" }, help = "the name of the platform from which to create the schedule") String platform) {
		Map<String, String> params = DeploymentPropertiesUtils.parse(properties);
		List<String> args = DeploymentPropertiesUtils.parseArgumentList(arguments, " ");
		params.put("scheduler.cron.expression", expression);

		scheduleOperations().schedule(name, definitionName, params, args, platform);
		return String.format("Created schedule '%s'", name);
	}

	@CliCommand(value = SCHEDULER_LIST, help = "List task schedules by task definition name")
	public Table listByDefinition(
			@CliOption(key = { "platform" }, help = "the name platform from which to retrieve a list of schedules") String platform,
			@CliOption(key = { "definitionName" }, help = "the task definition name") String definitionName) {
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

	@CliCommand(value = SCHEDULER_UNSCHEDULE, help = "Delete task schedule")
	public String unschedule(
			@CliOption(mandatory = true, key = { "name" }, help = "The name of the task schedule") String name,
			@CliOption(key = { "platform" }, help = "the name platform from which to unschedule") String platform) {
		scheduleOperations().unschedule(name, platform);
		return String.format("Deleted task schedule '%s'", name);
	}

	private SchedulerOperations scheduleOperations() {
		return dataFlowShell.getDataFlowOperations().schedulerOperations();
	}
}
