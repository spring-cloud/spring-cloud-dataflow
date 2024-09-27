/*
 * Copyright 2015-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.shell.ShellCommandRunner;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Helper methods for task commands to execute in the shell.
 * <p/>
 * It should mimic the client side API of TaskOperations as much as possible.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @author David Turanski
 * @author Chris Bono
 * @author Corneil du Plessis
 */
public class TaskCommandTemplate {
	private static final Logger logger = LoggerFactory.getLogger(TaskCommandTemplate.class);

	private final static int WAIT_INTERVAL = 500;

	private final static int MAX_WAIT_TIME = 3000;

	private final ShellCommandRunner commandRunner;

	private final List<String> tasks = new ArrayList<String>();

	private boolean allowErrors;

	/**
	 * Construct a new TaskCommandTemplate, given a spring shell.
	 *
	 * @param commandRunner the spring shell to execute commands against
	 */
	public TaskCommandTemplate(ShellCommandRunner commandRunner) {
		this.commandRunner = commandRunner;
	}

	/**
	 * @return a copy of this template that allows errors to be returned from the command runner when executing commands.
	 */
	public TaskCommandTemplate allowErrors() {
		TaskCommandTemplate copy = new TaskCommandTemplate(this.commandRunner);
		copy.allowErrors = true;
		return copy;
	}

	/**
	 * Create a task.
	 * <p>
	 * Note the name of the task will be stored so that when the method destroyCreatedTasks is
	 * called, the task will be destroyed.
	 *
	 * @param taskName the name of the task
	 * @param taskDefinition the task definition DSL
	 * @param values will be injected into taskdefinition according to
	 *     {@link String#format(String, Object...)} syntax
	 */
	public void create(String taskName, String taskDefinition, Object... values) {
		doCreate(taskName, taskDefinition, true, values);
	}

	/**
	 * Launch a task and validate the result from shell.
	 *
	 * @param taskName the name of the task
	 */
	public long launch(String taskName) {
		// add the task name to the tasks list before assertion
		tasks.add(taskName);
		Object result = commandRunner.executeCommand("task launch --name " + taskName);
		Object idResult = commandRunner.executeCommand("task execution list --name " + taskName);
		Table idResultTable = resultAsTable(idResult);
		logger.debug("launch:{} = {}", taskName, render(idResult));
		long value = (long) idResultTable.getModel().getValue(1, 1);
		assertThat(result.toString()).contains("with execution id " + value);
		return value;
	}


	/**
	 * Launch a task and validate the result from shell.
	 *
	 * @param taskName the name of the task
	 * @param ctrAppName the app to use when launching a ComposedTask if default is not wanted.
	 */
	public long launchWithAlternateCTR(String taskName, String ctrAppName) {
		// add the task name to the tasks list before assertion
		tasks.add(taskName);
		Object result = commandRunner.executeCommand(String.format("task launch --name %s --composedTaskRunnerName %s", taskName, ctrAppName));
		Object idResult = commandRunner.executeCommand("task execution list --name " + taskName);
		logger.debug("launchWithCTR:{}:{} = {}", taskName, ctrAppName, render(idResult));
		Table idResultTable = resultAsTable(idResult);

		long value = (long) idResultTable.getModel().getValue(1, 1);
		assertThat(result.toString()).contains("with execution id " + value);
		return value;
	}

	/**
	 * Launch a task and validate the result from shell on default platform.
	 *
	 * @param taskName the name of the task
	 */
	public String getTaskExecutionLog(String taskName) throws Exception{
		long id = launchTaskExecutionForLog(taskName);

		Object result = commandRunner.executeCommand("task execution log --id " + id);
		logger.debug("getTaskExecutionLog:{}={}", taskName, render(result));
		assertThat(result.toString()).contains("Starting");
		return result.toString();
	}

	/**
	 * Launch a task with invalid platform.
	 *
	 * @param taskName the name of the task
	 */
	public Object getTaskExecutionLogInvalidPlatform(String taskName) throws Exception {
		long id = launchTaskExecutionForLog(taskName);
		return commandRunner.executeCommand(String.format("task execution log --id %s --platform %s", id, "foo"));
	}

	/**
	 * Launch a task with invalid task execution id
	 */
	public void getTaskExecutionLogInvalidId() {
		Object result = commandRunner.executeCommand(String.format("task execution log --id %s", 88));
		logger.debug("getTaskExecutionLogInvalidId:{}", render(result));
	}

	private long launchTaskExecutionForLog(String taskName) throws Exception{
		// add the task name to the tasks list before assertion
		tasks.add(taskName);
		Object result = commandRunner.executeCommand(String.format("task launch --name %s", taskName));
		logger.debug("launchTaskExecutionForLog:{} = {}", taskName, render(result));
		Object idResult = commandRunner.executeCommand("task execution list --name " + taskName);
		logger.debug("launchTaskExecutionForLog:list:{} = {}", taskName, render(idResult));
		Table taskExecutionResult = resultAsTable(idResult);

		long id = (long) taskExecutionResult.getModel().getValue(1, 1);
		assertThat(result.toString()).contains("with execution id " + id);
		waitForDBToBePopulated(id);
		return id;
	}

	private void waitForDBToBePopulated(long id) throws Exception {
		for (int waitTime = 0; waitTime <= MAX_WAIT_TIME; waitTime += WAIT_INTERVAL) {
			if (isStartTime(id)) {
				break;
			}
			Thread.sleep(WAIT_INTERVAL);
		}
	}
	private void waitForEnd(long id) throws Exception {
		for (int waitTime = 0; waitTime <= MAX_WAIT_TIME; waitTime += WAIT_INTERVAL) {
			if (isEndTime(id)) {
				break;
			}
			Thread.sleep(WAIT_INTERVAL);
		}
	}
	private boolean isStartTime(long id) {
		Object result = taskExecutionStatus(id);
		logger.debug("isStartTime:{}:status={}", id, render(result));
		Table table = resultAsTable(result);
		return (table.getModel().getValue(8, 1) != null);

	}

	private String render(Object result) {
		if(result instanceof Table) {
			return ((Table) result).render(120);
		}
		return result.toString();
	}

	private boolean isEndTime(long id) {
		Object result = taskExecutionStatus(id);
		logger.debug("isEndTime:{}:status={}", id,render(result));
		Table table = resultAsTable(result);
		return (table.getModel().getValue(9, 1) != null);
	}
	/**
	 * Stop a task execution.
	 *
	 * @param id the id of a {@link org.springframework.cloud.task.repository.TaskExecution}
	 */
	public Object stop(long id) {
		return commandRunner.executeCommand("task execution stop --ids "+ id);
	}

	/**
	 * Stop a task execution.
	 *
	 * @param id the id of a {@link org.springframework.cloud.task.repository.TaskExecution}
	 * @param platform the name of the platform where the task is executing.
	 */
	public Object stopForPlatform(long id, String platform) {
		return  commandRunner.executeCommand(String.format("task execution stop --ids %s --platformName %s", id, platform));
	}


	/**
	 * Executes a task execution list.
	 */
	public Object taskExecutionList() {
		return commandRunner.executeCommand("task execution list");

	}

	/**
	 * Lists the platform accounts for tasks.
	 */
	public Object taskPlatformList() {
		return commandRunner.executeCommand("task platform-list");
	}

	/**
	 * Lists task executions by predefined name 'foo'.
	 */
	public Object taskExecutionListByName() {
		return commandRunner.executeCommand("task execution list --name foo");

	}

	/**
	 * Returns the count of currently executing tasks and related information.
	 */
	public Object taskExecutionCurrent() {
		return commandRunner.executeCommand("task execution current");
	}

	/**
	 * Validates the task definition.
	 */
	public Object taskValidate(String taskDefinitionName) {
		return commandRunner.executeCommand("task validate --name " + taskDefinitionName);
	}

	/**
	 * Lists task executions by given name.
	 */
	public Object taskExecutionListByName(String name) {
		return commandRunner.executeCommand("task execution list --name " + name);

	}

	private void doCreate(String taskName, String taskDefinition, Object... values) {
		String actualDefinition = String.format(taskDefinition, values);
		// Shell parser expects quotes to be escaped by \
		String wholeCommand = String.format("task create --name \"%s\" --definition \"%s\"", taskName,
				actualDefinition.replaceAll("\"", "\\\\\""));

		Object result = commandRunner.executeCommand(wholeCommand);
		logger.debug("doCreate:{} = {}", wholeCommand, render(result));
		// add the task name to the tasks list before assertion
		tasks.add(taskName);
		String createMsg = "Created";

		assertThat(result).hasToString(createMsg + " new task '" + taskName + "'");

		verifyExists(taskName, actualDefinition);
	}

	/**
	 * Destroy all tasks that were created using the 'create' method. Commonly called in
	 * a @After annotated method.
	 */
	public void destroyCreatedTasks() {
		for (int s = tasks.size() - 1; s >= 0; s--) {
			String taskname = tasks.get(s);
			commandRunner.executeCommand("task destroy --name " + taskname);
			// stateVerifier.waitForDestroy(taskname);
		}
	}

	/**
	 * Destroy a specific task identified by the given name.
	 *
	 * @param task The task to destroy
	 */
	public void destroyTask(String task) {
		Object result = commandRunner.executeCommand("task destroy --name " + task);
		// stateVerifier.waitForDestroy(task);
		tasks.remove(task);
	}

	/**
	 * Destroy a specific task identified by the given name and cleanup if set to true.
	 *
	 * @param task The task to destroy
	 * @param cleanup the boolean flag to clean up task executions and other resources
	 */
	public void destroyTask(String task, boolean cleanup) {
		commandRunner.executeCommand(String.format("task destroy --name %s %s", task, (cleanup) ? "--cleanup" : ""));
		tasks.remove(task);
	}

	/**
	 * Destroy all tasks.
	 *
	 */
	public void destroyAllTasks() {
		commandRunner.executeCommand("task all destroy --force");
		// stateVerifier.waitForDestroy(task);
		tasks.clear();
	}

	/**
	 * Verify the task is listed in task list.
	 *
	 * @param taskName the name of the task
	 * @param definition definition of the task
	 */
	public void verifyExists(String taskName, String definition) {
		Table table = resultAsTable(commandRunner.executeCommand("task list"));
		TableModel model = table.getModel();
		for (int row = 0; row < model.getRowCount(); row++) {
			if (taskName.equals(model.getValue(row, 0))
					&& definition.replace("\\\\", "\\").equals(model.getValue(row, 1))) {
				return;
			}
		}
		fail("Task named " + taskName + " was not created");
	}

	/**
	 * Return the results of executing the shell command:
	 * <code>dataflow: task execution status --id 1</code> where 1 is the id for the task
	 * execution requested.
	 *
	 * @param id the identifier for the task execution
	 * @return the results of the shell command.
	 */
	public Object taskExecutionStatus(long id) {
		return commandRunner.executeCommand("task execution status --id " + id);
	}

	public Object taskExecutionCleanup(long id) {
		return commandRunner.executeCommand("task execution cleanup --id " + id + " --force");
	}

	public Object taskExecutionCleanup() {
		return commandRunner.executeCommand("task execution cleanup --all --force");
	}

	private Table resultAsTable(Object result) {
		assertThat(result).isInstanceOf(Table.class);
		return (Table) result;
	}
}
