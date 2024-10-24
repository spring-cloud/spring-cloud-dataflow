/*
 * Copyright 2018-2022 the original author or authors.
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.CurrentTaskExecutionsResource;
import org.springframework.cloud.dataflow.rest.resource.LaunchResponseResource;
import org.springframework.cloud.dataflow.rest.resource.LauncherResource;
import org.springframework.cloud.dataflow.rest.resource.TaskAppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionThinResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.command.support.TablesInfo;
import org.springframework.cloud.dataflow.shell.completer.TaskNameValueProvider;
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
import org.springframework.shell.table.TableModelBuilder;
import org.springframework.util.Assert;
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
 * @author Mike Baranski
 * @author Chris Bono
 * @author Corneil du Plessis
 */
@ShellComponent
public class TaskCommands {

	private static final String PROPERTIES_OPTION = "properties";

	private static final String PROPERTIES_FILE_OPTION = "propertiesFile";

	private static final String ARGUMENTS_OPTION = "arguments";

	private static final String PLATFORM_OPTION = "platformName";

	// Create Role

	private static final String CREATE = "task create";

	// Deploy Role

	private static final String LAUNCH = "task launch";

	private static final String STOP = "task execution stop";

	private static final String LOG = "task execution log";

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

	@Autowired
	protected UserInput userInput;

	@Autowired
	private DataFlowShell dataFlowShell;

	public Availability availableWithViewRole() {
		return availabilityFor(RoleType.VIEW, OpsType.TASK);
	}

	public Availability availableWithCreateRole() {
		return availabilityFor(RoleType.CREATE, OpsType.TASK);
	}

	public Availability availableWithDeployRole() {
		return availabilityFor(RoleType.DEPLOY, OpsType.TASK);
	}

	public Availability availableWithUnDeployRole() {
		return availabilityFor(RoleType.DEPLOY, OpsType.TASK);
	}

	public Availability availableWithDestroyRole() {
		return availabilityFor(RoleType.DESTROY, OpsType.TASK);
	}

	private Availability availabilityFor(RoleType roleType, OpsType opsType) {
		return dataFlowShell.hasAccess(roleType, opsType)
				? Availability.available()
				: Availability.unavailable("you do not have permissions");
	}

	@ShellMethod(key = LIST, value = "List created tasks")
	@ShellMethodAvailability("availableWithViewRole")
	public Table list() {
		final PagedModel<TaskDefinitionResource> tasks = taskOperations().list();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Task Name");
		headers.put("dslText", "Task Definition");
		headers.put("description", "description");
		headers.put("status", "Task Status");
		final TableBuilder builder = new TableBuilder(new BeanListTableModel<>(tasks, headers));
		return DataFlowTables.applyStyle(builder).build();
	}

	@ShellMethod(key = PLATFORM_LIST, value = "List platform accounts for tasks")
	@ShellMethodAvailability("availableWithViewRole")
	public Table listPlatforms() {
		final PagedModel<LauncherResource> platforms = taskOperations().listPlatforms();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Platform Name");
		headers.put("type", "Platform Type");
		headers.put("description", "Description");
		final TableBuilder builder = new TableBuilder(new BeanListTableModel<>(platforms, headers));
		return DataFlowTables.applyStyle(builder).build();
	}

	@ShellMethod(key = VALIDATE, value = "Validate apps contained in task definitions")
	public TablesInfo validate(
			@ShellOption(value = {"", "--name"}, help = "the task definition name") String name) throws OperationNotSupportedException {
		final TaskAppStatusResource task = taskOperations().validateTaskDefinition(name);
		TablesInfo result = new TablesInfo();
		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
		modelBuilder.addRow().addValue("Task Name").addValue("Task Definition");
		modelBuilder.addRow().addValue(task.getAppName())
				.addValue(task.getDsl());
		TableBuilder builder = DataFlowTables.applyStyle(new TableBuilder(modelBuilder.build()));
		result.addTable(builder.build());

		modelBuilder = new TableModelBuilder<>();
		modelBuilder.addRow().addValue("App Name").addValue("Validation Status");
		boolean isValidStream = true;
		for (Map.Entry<String, String> entry : task.getAppStatuses().entrySet()) {
			modelBuilder.addRow().addValue(entry.getKey())
					.addValue(entry.getValue());
			if (entry.getValue().equals("invalid")) {
				isValidStream = false;
			}
		}
		builder = DataFlowTables.applyStyle(new TableBuilder(modelBuilder.build()));

		if (isValidStream) {
			result.addFooter(String.format("\n%s is a valid task.", task.getAppName()));
		} else {
			result.addFooter(String.format("\n%s is an invalid task.", task.getAppName()));
		}
		result.addTable(builder.build());
		return result;
	}

	@ShellMethod(key = CREATE, value = "Create a new task definition")
	@ShellMethodAvailability("availableWithCreateRole")
	public String create(
			@ShellOption(value = {"", "--name"}, help = "the name to give to the task") String name,
			@ShellOption(value = "--definition", help = "a task definition, using the DSL (e.g. \"timestamp --format=YYYY\")") String dsl,
			@ShellOption(help = "a sort description about the task", defaultValue = "") String description) {
		this.taskOperations().create(name, dsl, description);
		return String.format("Created new task '%s'", name);
	}

	@ShellMethod(key = LAUNCH, value = "Launch a previously created task")
	@ShellMethodAvailability("availableWithDeployRole")
	public String launch(
			@ShellOption(value = {"", "--name"}, help = "the name of the task to launch") String name,
			@ShellOption(help = "the properties for this launch", defaultValue = ShellOption.NULL) String properties,
			@ShellOption(value = "--propertiesFile", help = "the properties for this launch (as a File)", defaultValue = ShellOption.NULL) File propertiesFile,
			@ShellOption(help = "the commandline arguments for this launch", defaultValue = ShellOption.NULL) String arguments,
			@ShellOption(value = "--platformName", help = "the platform name to use for this launch", defaultValue = ShellOption.NULL) String platformName)
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
		LaunchResponseResource response = taskOperations().launch(name, propertiesToUse, argumentsToUse);
		return String.format("Launched task '%s' with execution id %d", name, response.getExecutionId());
	}

	@ShellMethod(key = STOP, value = "Stop executing tasks")
	@ShellMethodAvailability("availableWithUnDeployRole")
	public String stop(
			@ShellOption(value = {"", "--ids"}, help = "the task execution id") String ids,
			@ShellOption(value = "--platformName", help = "the name of the platform where the task is executing", defaultValue = ShellOption.NULL) String platform) {

		String message = null;
		if (StringUtils.hasText(platform)) {
			taskOperations().stop(ids, platform);
			message = String.format(
					"Request to stop the task execution with id(s): %s for platform %s has been submitted", ids,
					platform);
		} else {
			taskOperations().stop(ids);
			message = String.format("Request to stop the task execution with id(s): %s has been submitted", ids);
		}
		return message;
	}

	@ShellMethod(key = LOG, value = "Retrieve task execution log")
	public String retrieveTaskExecutionLog(
			@ShellOption(value = {"", "--id"}, help = "the task execution id", defaultValue = ShellOption.NULL) Long id,
			@ShellOption(value = {"", "--externalExecutionId"}, help = "the task external execution id", defaultValue = ShellOption.NULL) String externalExecutionId,
			@ShellOption(help = "the platform of the task execution", defaultValue = ShellOption.NULL) String platform) {
		if(externalExecutionId == null) {
			TaskExecutionResource taskExecutionResource = taskOperations().taskExecutionStatus(id);
			externalExecutionId = taskExecutionResource.getExternalExecutionId();
		}
		String result;
		if (platform != null) {
			result = taskOperations().taskExecutionLog(externalExecutionId, platform);
		} else {
			result = taskOperations().taskExecutionLog(externalExecutionId);
		}
		return result;
	}

	@ShellMethod(key = DESTROY, value = "Destroy an existing task")
	@ShellMethodAvailability("availableWithDestroyRole")
	public String destroy(
			@ShellOption(value = {"", "--name"}, help = "the name of the task to destroy", valueProvider = TaskNameValueProvider.class) String name,
			@ShellOption(help = "the boolean flag to set if task executions and related resources should NOT also be cleaned up", defaultValue = "false") boolean cleanup) {
		taskOperations().destroy(name, cleanup);
		return String.format("Destroyed task '%s'", name);
	}

	@ShellMethod(key = DESTROY_TASK_ALL, value = "Destroy all existing tasks")
	@ShellMethodAvailability("availableWithDestroyRole")
	public String destroyAll(
			@ShellOption(help = "bypass confirmation prompt", defaultValue = "false") boolean force) {
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really destroy all tasks?", "n", "y", "n"))) {
			taskOperations().destroyAll();
			return "All tasks destroyed";
		}
		return "";
	}

	@ShellMethod(key = EXECUTION_LIST, value = "List created task executions filtered by taskName")
	@ShellMethodAvailability("availableWithViewRole")
	public Table executionListByName(
			@ShellOption(value = {"", "--name"}, help = "the task name to be used as a filter", valueProvider = TaskNameValueProvider.class, defaultValue = ShellOption.NULL) String name) {

		PagedModel<TaskExecutionThinResource> thinTasks = null;
		if (name == null) {
			thinTasks = taskOperations().thinExecutionList();
		} else {
			thinTasks = taskOperations().thinExecutionListByTaskName(name);
		}
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("taskName", "Task Name");
		headers.put("executionId", "ID");
		headers.put("startTime", "Start Time");
		headers.put("endTime", "End Time");
		headers.put("exitCode", "Exit Code");
		TableBuilder builder = new TableBuilder(new BeanListTableModel<>(thinTasks, headers));
		return DataFlowTables.applyStyle(builder).build();
	}

	@ShellMethod(key = TASK_EXECUTION_STATUS, value = "Display the details of a specific task execution")
	@ShellMethodAvailability("availableWithViewRole")
	public Table display(@ShellOption(value = {"", "--id"}, help = "the task execution id") long id) {

		TaskExecutionResource taskExecutionResource = taskOperations().taskExecutionStatus(id);

		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();

		modelBuilder.addRow().addValue("Key ").addValue("Value ");
		modelBuilder.addRow().addValue("Id ").addValue(taskExecutionResource.getExecutionId());
		modelBuilder.addRow().addValue("Resource URL ").addValue(taskExecutionResource.getResourceUrl());
		modelBuilder.addRow().addValue("Name ").addValue(taskExecutionResource.getTaskName());
		modelBuilder.addRow().addValue("CLI Arguments ").addValue(taskExecutionResource.getArguments());
		modelBuilder.addRow().addValue("App Arguments ").addValue(taskExecutionResource.getAppProperties());
		modelBuilder.addRow().addValue("Deployment Properties ")
				.addValue(taskExecutionResource.getDeploymentProperties());
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

	@ShellMethod(key = TASK_EXECUTION_CURRENT, value = "Display count of currently executin tasks and related information")
	@ShellMethodAvailability("availableWithViewRole")
	public Table currentExecutions() {
		Collection<CurrentTaskExecutionsResource> taskExecutionsResources = taskOperations().currentTaskExecutions();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Platform Name");
		headers.put("type", "Platform Type");
		headers.put("runningExecutionCount", "Execution Count");
		headers.put("maximumTaskExecutions", "Maximum Executions");

		TableBuilder builder = new TableBuilder(new BeanListTableModel<>(taskExecutionsResources, headers));
		DataFlowTables.applyStyle(builder);
		return builder.build();
	}

	@ShellMethod(key = TASK_EXECUTION_CLEANUP, value = "Clean up any platform specific resources linked to a task "
			+ "execution")
	@ShellMethodAvailability("availableWithDestroyRole")
	public String cleanup(
			@ShellOption(value = {"", "--id"}, help = "the task execution id", defaultValue = ShellOption.NULL) Long id,
			@ShellOption(help = "all task execution IDs", defaultValue = "false") boolean all,
			@ShellOption(help = "include non-completed task executions", defaultValue = "false") boolean includeNonCompleted,
			@ShellOption(value = "--task-name", help = "the name of the task to cleanup", defaultValue = ShellOption.NULL) String taskName,
			@ShellOption(help = "bypass confirmation prompt", defaultValue = "false") boolean force) {
		Assert.isTrue(!(id != null && all && StringUtils.hasText(taskName)),
				"`taskName`, `id` and `all` options are mutually exclusive.");
		boolean completedOnly = !includeNonCompleted;
		if (all) {
			Integer taskExecutionsCount = this.taskOperations().getAllTaskExecutionsCount(completedOnly, null);
			if (taskExecutionsCount > 0) {
				String taskExecutions = (completedOnly) ? taskExecutionsCount + " completed"
						: taskExecutionsCount.toString();
				String warn = String.format("About to delete %s task executions and related records", taskExecutions);
				warn = warn + ". This operation can not be reverted. Are you sure (y/n)? ";
				if (force || "y".equalsIgnoreCase(userInput.promptWithOptions(warn, "n", "y", "n"))) {
					taskOperations().cleanupAllTaskExecutions(completedOnly, null);
					return "Request to clean up resources for task executions has been submitted";
				}
			} else {
				return String.format("No %stask executions available for deletion.", (completedOnly) ? "completed " : "");
			}
		} else if (StringUtils.hasText(taskName)) {
			Integer taskExecutionsCount = this.taskOperations().getAllTaskExecutionsCount(completedOnly, taskName);
			if (taskExecutionsCount > 0) {
				String taskExecutions = (completedOnly) ? taskExecutionsCount + " completed"
						: taskExecutionsCount.toString();
				String warn = String.format("About to delete %s task executions and related records", taskExecutions);
				warn = warn + ". This operation can not be reverted. Are you sure (y/n)? ";
				if (force || "y".equalsIgnoreCase(userInput.promptWithOptions(warn, "n", "y", "n"))) {
					taskOperations().cleanupAllTaskExecutions(completedOnly, taskName);
					return "Request to clean up resources for task executions has been submitted";
				}
			} else {
				return String.format("No %stask executions available for deletion.", (completedOnly) ? "completed " : "");
			}
		} else {
			Assert.notNull(id, "Task Execution ID should be set");
			String warn = "About to delete 1 task execution. Are you sure (y/n)?";
			if (force || "y".equalsIgnoreCase(userInput.promptWithOptions(warn, "n", "y", "n")))
				taskOperations().cleanup(id);
			return String.format("Request to clean up resources for task execution %s has been submitted", id);
		}
		return "Cleanup process is canceled";
	}

	private TaskOperations taskOperations() {
		return dataFlowShell.getDataFlowOperations().taskOperations();
	}
}
