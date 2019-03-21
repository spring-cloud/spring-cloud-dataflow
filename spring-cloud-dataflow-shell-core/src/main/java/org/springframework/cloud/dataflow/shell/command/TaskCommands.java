/*
 * Copyright 2015-2016 the original author or authors.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
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
 */
@Component
// todo: reenable optionContext attributes
public class TaskCommands implements CommandMarker {

	private static final String LIST = "task list";

	private static final String CREATE = "task create";

	private static final String LAUNCH = "task launch";

	private static final String DESTROY = "task destroy";

	private static final String TASK_EXECUTION_STATUS = "task execution status";

	private static final String PROPERTIES_OPTION = "properties";

	private static final String PROPERTIES_FILE_OPTION = "propertiesFile";

	private static final String ARGUMENTS_OPTION = "arguments";

	private static final String EXECUTION_LIST = "task execution list";


	@Autowired
	private DataFlowShell dataFlowShell;

	@CliAvailabilityIndicator({ LIST, CREATE, LAUNCH, TASK_EXECUTION_STATUS, DESTROY, EXECUTION_LIST })
	public boolean available() {
		DataFlowOperations dataFlowOperations = dataFlowShell.getDataFlowOperations();
		return dataFlowOperations != null && dataFlowOperations.taskOperations() != null;
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

	@CliCommand(value = CREATE, help = "Create a new task definition")
	public String create(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the task") String name,
			@CliOption(mandatory = true, key = { "definition" }, help = "a task definition, using the DSL (e.g. \"timestamp --format=YYYY\")", optionContext = "disable-string-converter completion-task") String dsl){
		this.taskOperations().create(name, dsl);
		return String.format("Created new task '%s'", name);
	}

	@CliCommand(value = LAUNCH, help = "Launch a previously created task")
	public String launch(
			@CliOption(key = { "", "name" }, help = "the name of the task to launch", mandatory = true) String name,
			@CliOption(key = { PROPERTIES_OPTION }, help = "the properties for this launch", mandatory = false) String properties,
			@CliOption(key = { PROPERTIES_FILE_OPTION }, help = "the properties for this launch (as a File)", mandatory = false) File propertiesFile,
			@CliOption(key = { ARGUMENTS_OPTION }, help = "the commandline arguments for this launch", mandatory = false) String arguments
			) throws IOException {
		int which = Assertions.atMostOneOf(PROPERTIES_OPTION, properties, PROPERTIES_FILE_OPTION, propertiesFile);
		Map<String, String> propertiesToUse;
		switch (which) {
			case 0:
				propertiesToUse = DeploymentPropertiesUtils.parse(properties);
				break;
			case 1:
				Properties props = new Properties();
				try (FileInputStream fis = new FileInputStream(propertiesFile)) {
					props.load(fis);
				}
				propertiesToUse = DeploymentPropertiesUtils.convert(props);
				break;
			case -1: // Neither option specified
				propertiesToUse = Collections.emptyMap();
				break;
			default:
				throw new AssertionError();
		}
		List<String> argumentsToUse = new ArrayList<String>();
		if (StringUtils.hasText(arguments)) {
			argumentsToUse.add(arguments);
		}
		taskOperations().launch(name, propertiesToUse, argumentsToUse);
		return String.format("Launched task '%s'", name);
	}

	@CliCommand(value = DESTROY, help = "Destroy an existing task")
	public String destroy(
			@CliOption(key = { "", "name" }, help = "the name of the task to destroy", mandatory = true) String name) {
		taskOperations().destroy(name);
		return String.format("Destroyed task '%s'", name);
	}

	@CliCommand(value = EXECUTION_LIST,
			help = "List created task executions filtered by taskName")
	public Table executionListByName(
			@CliOption(key = { "name" },
					help = "the task name to be used as a filter",
					mandatory = false) String name) {

		final PagedResources<TaskExecutionResource> tasks;
		if(name == null){
			tasks = taskOperations().executionList();
		}
		else{
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
	public Table display(
			@CliOption(key = { "id" },
					help = "the task execution id",
					mandatory = true) long id) {

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
		modelBuilder.addRow().addValue("External Execution Id ").addValue(taskExecutionResource.getExternalExecutionId());

		TableBuilder builder = new TableBuilder(modelBuilder.build());

		DataFlowTables.applyStyle(builder);

		return builder.build();
	}

	private TaskOperations taskOperations() {
		return dataFlowShell.getDataFlowOperations().taskOperations();
	}
}
