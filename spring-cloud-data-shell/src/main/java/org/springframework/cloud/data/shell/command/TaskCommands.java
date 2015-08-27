/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.shell.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.data.rest.client.TaskOperations;
import org.springframework.cloud.data.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.data.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.data.shell.config.CloudDataShell;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.table.Table;
import org.springframework.shell.support.table.TableHeader;
import org.springframework.stereotype.Component;

/**
 * Task commands.
 *
 * @author Glenn Renfro
 */
@Component
// todo: reenable optionContext attributes
public class TaskCommands implements CommandMarker {

	private static final String LIST = "task list";

	private static final String CREATE = "task create";

	private static final String LAUNCH = "task launch";

	private static final String STATUS = "task status";

	private static final String DESTROY = "task destroy";

	private static final String PROPERTIES_OPTION = "properties";

	private static final String PROPERTIES_FILE_OPTION = "propertiesFile";


	@Autowired
	private CloudDataShell cloudDataShell;

	@Autowired
	private UserInput userInput;

	@CliAvailabilityIndicator({ LIST, CREATE, LAUNCH, STATUS, DESTROY})
	public boolean available() {
		return cloudDataShell.getCloudDataOperations() != null;
	}

	@CliCommand(value = LIST, help = "List created tasks")
	public Table list() {
		final PagedResources<TaskDefinitionResource> tasks = taskOperations().list();

		final Table table = new Table()
				.addHeader(1, new TableHeader("Task Name"))
				.addHeader(2, new TableHeader("Task Definition"))
				.addHeader(3, new TableHeader("Task Status"));
		for (TaskDefinitionResource task : tasks) {
			table.newRow()
					.addValue(1, task.getName())
					.addValue(2, task.getDslText())
					.addValue(3, (task.getStatus() == null) ? "" : task.getStatus());
		}

		return table;
	}

	@CliCommand(value = CREATE, help = "Create a new task definition")
	public String create(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the task") String name,
			@CliOption(mandatory = true, key = { "definition" }, help = "a task definition, using the DSL (e.g. \"taskName\")") String dsl){
		this.taskOperations().create(name, dsl);
		return String.format(
				"Created new task '%s'", name);
	}

	@CliCommand(value = LAUNCH, help = "Launch a previously created task")
	public String launch(
			@CliOption(key = { "", "name" }, help = "the name of the task to launch", mandatory = true) String name,
			@CliOption(key = { PROPERTIES_OPTION }, help = "the properties for this launch", mandatory = false) String properties,
			@CliOption(key = { PROPERTIES_FILE_OPTION }, help = "the properties for this launch (as a File)", mandatory = false) File propertiesFile
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
				propertiesToUse = Collections.<String, String> emptyMap();
				break;
			default:
				throw new AssertionError();
		}
		taskOperations().launch(name, propertiesToUse);
		return String.format("Launched task '%s'", name);
	}
	
	@CliCommand(value = DESTROY, help = "Destroy an existing task")
	public String destroy(
			@CliOption(key = { "", "name" }, help = "the name of the task to destroy", mandatory = true) String name) {
		taskOperations().destroy(name);
		return String.format("Destroyed task '%s'", name);
	}

	@CliCommand(value = STATUS, help = "Retrieve status info on an existing task")
	public String status(
			@CliOption(key = { "", "name" }, help = "the name of the task ", mandatory = true) String name) {

		return String.format("Feature Not Available");
	}
	
	TaskOperations taskOperations() {
		return cloudDataShell.getCloudDataOperations().taskOperations();
	}
}
