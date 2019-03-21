/*
 * Copyright 2018-2019 the original author or authors.
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.CurrentTaskExecutionsResource;
import org.springframework.cloud.dataflow.rest.resource.LauncherResource;
import org.springframework.cloud.dataflow.rest.resource.TaskAppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModelBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Task commands.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 * @author David Turanski
 */
@Component
public class TaskCommands implements CommandMarker {

	private static final String PROPERTIES_OPTION = "properties";
	private static final String PROPERTIES_FILE_OPTION = "propertiesFile";
	private static final String ARGUMENTS_OPTION = "arguments";
	private static final String PLATFORM_OPTION = "platformName";

	// Create Role

	private static final String CREATE = "task create";

	// Deploy Role

	private static final String LAUNCH = "task launch";

	// Destroy Role

	private static final String DESTROY = "task destroy";
	private static final String DESTROY_TASK_ALL = "task all destroy";
	private static final String TASK_EXECUTION_CLEANUP = "task execution cleanup";

	// View Role

	private static final String EXECUTION_LIST = "task execution list";

	private static final String LIST = "task list";
	private static final String PLATFORM_LIST = "task platform-list";
	private static final String TASK_EXECUTION_CURRENT = "task execution current";
	private static final String TASK_EXECUTION_STATUS = "task execution status";
	private static final String VALIDATE = "task validate";
	private static final String PLATFORM_NAME = "platformName";


	@Autowired
	protected UserInput userInput;

	@Autowired
	private DataFlowShell dataFlowShell;

	@CliAvailabilityIndicator({ CREATE })
	public boolean availableWithCreateRole() {
		return dataFlowShell.hasAccess(RoleType.CREATE, OpsType.TASK);
	}

	@CliAvailabilityIndicator({ LAUNCH })
	public boolean availableWithDeployRole() {
		return dataFlowShell.hasAccess(RoleType.DEPLOY, OpsType.TASK);
	}

	@CliAvailabilityIndicator({ DESTROY, DESTROY_TASK_ALL, TASK_EXECUTION_CLEANUP })
	public boolean availableWithDestroyRole() {
		return dataFlowShell.hasAccess(RoleType.DESTROY, OpsType.TASK);
	}

	@CliAvailabilityIndicator({ EXECUTION_LIST, LIST, PLATFORM_LIST, TASK_EXECUTION_CURRENT, TASK_EXECUTION_STATUS, VALIDATE })
	public boolean availableWithViewRole() {
		return dataFlowShell.hasAccess(RoleType.VIEW, OpsType.TASK);
	}

	@CliCommand(value = LIST, help = "List created tasks")
	public Table list() {
		final PagedResources<TaskDefinitionResource> tasks = taskOperations().list();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Task Name");
		headers.put("dslText", "Task Definition");
		headers.put("status", "Task Status");
		final TableBuilder builder = new TableBuilder(new BeanListTableModel<>(tasks, headers));
		return DataFlowTables.applyStyle(builder).build();
	}

	@CliCommand(value = PLATFORM_LIST, help = "List platform accounts for tasks")
	public Table listPlatforms() {
		final PagedResources<LauncherResource> platforms = taskOperations().listPlatforms();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Platform Name");
		headers.put("type", "Platform Type");
		headers.put("description", "Description");
		final TableBuilder builder = new TableBuilder(new BeanListTableModel<>(platforms, headers));
		return DataFlowTables.applyStyle(builder).build();
	}

	@CliCommand(value = VALIDATE, help = "Validate apps contained in task definitions")
	public List<Object> validate(@CliOption(key = { "", "name" }, help = "the task definition name", mandatory = true) String name) throws OperationNotSupportedException {
		final TaskAppStatusResource task = taskOperations().validateTaskDefinition(name);
		List<Object> result = new ArrayList<>();
		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
		modelBuilder.addRow().addValue("Task Name").addValue("Task Definition");
		modelBuilder.addRow().addValue(task.getAppName())
				.addValue(task.getDsl());
		TableBuilder builder = DataFlowTables.applyStyle(new TableBuilder(modelBuilder.build()));
		result.add(builder.build());

		modelBuilder = new TableModelBuilder<>();
		modelBuilder.addRow().addValue("App Name").addValue("Validation Status");
		boolean isValidStream = true;
		for(Map.Entry<String,String> entry : task.getAppStatuses().entrySet()) {
			modelBuilder.addRow().addValue(entry.getKey())
					.addValue(entry.getValue());
			if (entry.getValue().equals("invalid")) {
				isValidStream = false;
			}
		}
		builder = DataFlowTables.applyStyle(new TableBuilder(modelBuilder.build()));

		if(isValidStream) {
			result.add(String.format("\n%s is a valid task.", task.getAppName()));
		}
		else {
			result.add(String.format("\n%s is an invalid task.", task.getAppName()));
		}
		result.add(builder.build());
		return result;
	}

	@CliCommand(value = CREATE, help = "Create a new task definition")
	public String create(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the task") String name,
			@CliOption(mandatory = true, key = { "definition" }, help = "a task definition, using the DSL (e.g. "
					+ "\"timestamp --format=YYYY\")", optionContext = "disable-string-converter completion-task") String dsl) {
		this.taskOperations().create(name, dsl);
		return String.format("Created new task '%s'", name);
	}

	@CliCommand(value = LAUNCH, help = "Launch a previously created task")
	public String launch(
			@CliOption(key = { "", "name" }, help = "the name of the task to launch", mandatory = true,
				optionContext = "existing-task disable-string-converter") String name,
			@CliOption(key = {
					PROPERTIES_OPTION }, help = "the properties for this launch") String properties,
			@CliOption(key = {
					PROPERTIES_FILE_OPTION }, help = "the properties for this launch (as a File)") File propertiesFile,
			@CliOption(key = {
					ARGUMENTS_OPTION }, help = "the commandline arguments for this launch") String arguments,
			@CliOption(key = {
					PLATFORM_NAME}, help = "the platform name to use for this launch",
					unspecifiedDefaultValue = "default") String platformName)
			throws IOException {
		int which = Assertions.atMostOneOf(PROPERTIES_OPTION, properties, PROPERTIES_FILE_OPTION, propertiesFile);
		Map<String, String> propertiesToUse = DeploymentPropertiesUtils.parseDeploymentProperties(properties,
				propertiesFile, which);

		List<String> argumentsToUse = new ArrayList<String>();
		if (StringUtils.hasText(arguments)) {
			argumentsToUse.add(arguments);
		}
		DeploymentPropertiesUtils.validateDeploymentProperties(propertiesToUse);
		if (StringUtils.hasText(platformName)) {
			propertiesToUse.put("spring.cloud.dataflow.task.platformName", platformName);
		}
		long taskExecutionId = taskOperations().launch(name, propertiesToUse, argumentsToUse);
		return String.format("Launched task '%s' with execution id %d", name, taskExecutionId);
	}

	@CliCommand(value = DESTROY, help = "Destroy an existing task")
	public String destroy(
			@CliOption(key = { "", "name" }, help = "the name of the task to destroy", mandatory = true,
				optionContext = "existing-task disable-string-converter") String name) {
		taskOperations().destroy(name);
		return String.format("Destroyed task '%s'", name);
	}

	@CliCommand(value = DESTROY_TASK_ALL, help = "Destroy all existing tasks")
	public String destroyAll(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force) {
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really destroy all tasks?", "n", "y", "n"))) {
			taskOperations().destroyAll();
			return String.format("All tasks destroyed");
		} else {
			return "";
		}
	}

	@CliCommand(value = EXECUTION_LIST, help = "List created task executions filtered by taskName")
	public Table executionListByName(@CliOption(key = "name", help = "the task name to be used as a filter",
		optionContext = "existing-task disable-string-converter") String name) {

		final PagedResources<TaskExecutionResource> tasks;
		if (name == null) {
			tasks = taskOperations().executionList();
		}
		else {
			tasks = taskOperations().executionListByTaskName(name);
		}
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("taskName", "Task Name");
		headers.put("executionId", "ID");
		headers.put("startTime", "Start Time");
		headers.put("endTime", "End Time");
		headers.put("exitCode", "Exit Code");
		final TableBuilder builder = new TableBuilder(new BeanListTableModel<>(tasks, headers));
		return DataFlowTables.applyStyle(builder).build();
	}

	@CliCommand(value = TASK_EXECUTION_STATUS, help = "Display the details of a specific task execution")
	public Table display(@CliOption(key = { "", "id" }, help = "the task execution id", mandatory = true) long id) {

		TaskExecutionResource taskExecutionResource = taskOperations().taskExecutionStatus(id);

		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();

		modelBuilder.addRow().addValue("Key ").addValue("Value ");
		modelBuilder.addRow().addValue("Id ").addValue(taskExecutionResource.getExecutionId());
		modelBuilder.addRow().addValue("Name ").addValue(taskExecutionResource.getTaskName());
		modelBuilder.addRow().addValue("Arguments ").addValue(taskExecutionResource.getArguments());
		modelBuilder.addRow().addValue("Job Execution Ids ").addValue(taskExecutionResource.getJobExecutionIds());
		modelBuilder.addRow().addValue("Start Time ").addValue(taskExecutionResource.getStartTime());
		modelBuilder.addRow().addValue("End Time ").addValue(taskExecutionResource.getEndTime());
		modelBuilder.addRow().addValue("Exit Code ").addValue(taskExecutionResource.getExitCode());
		modelBuilder.addRow().addValue("Exit Message ").addValue(taskExecutionResource.getExitMessage());
		modelBuilder.addRow().addValue("Error Message ").addValue(taskExecutionResource.getErrorMessage());
		modelBuilder.addRow().addValue("External Execution Id ")
				.addValue(taskExecutionResource.getExternalExecutionId());

		TableBuilder builder = new TableBuilder(modelBuilder.build());

		DataFlowTables.applyStyle(builder);

		return builder.build();
	}

	@CliCommand(value = TASK_EXECUTION_CURRENT,
		help = "Display count of currently executin tasks and related information")
	public Table currentExecutions() {
		CurrentTaskExecutionsResource taskExecutionsResource = taskOperations().currentTaskExecutions();
		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
		modelBuilder.addRow().addValue("Current Running Tasks").addValue(taskExecutionsResource.getRunningExecutionCount());
		modelBuilder.addRow().addValue("Maximum Concurrent Executions").addValue(taskExecutionsResource
			.getMaximumTaskExecutions());
		TableBuilder builder = new TableBuilder(modelBuilder.build());
		DataFlowTables.applyStyle(builder);
		return builder.build();
	}

	@CliCommand(value = TASK_EXECUTION_CLEANUP, help = "Clean up any platform specific resources linked to a task "
			+ "execution")
	public String cleanup(@CliOption(key = { "", "id" }, help = "the task execution id", mandatory = true) long id) {
		taskOperations().cleanup(id);
		return String.format("Request to clean up resources for task execution %s has been submitted", id);
	}

	private TaskOperations taskOperations() {
		return dataFlowShell.getDataFlowOperations().taskOperations();
	}
}
