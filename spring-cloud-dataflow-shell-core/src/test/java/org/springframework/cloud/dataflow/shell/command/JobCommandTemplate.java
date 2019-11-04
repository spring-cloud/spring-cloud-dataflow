/*
 * Copyright 2016 the original author or authors.
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

import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;

/**
 * Helper methods for task commands to execute in the shell.
 * <p/>
 * It should mimic the client side API of JobOperations as much as possible.
 *
 * @author Glenn Renfro
 */
public class JobCommandTemplate {

	private final JLineShellComponent shell;

	/**
	 * Construct a new JobCommandTemplate, given a spring shell.
	 *
	 * @param shell the spring shell to execute commands against.
	 */
	public JobCommandTemplate(JLineShellComponent shell) {
		this.shell = shell;
	}

	/**
	 * Executes a task execution list.
	 */
	public CommandResult jobExecutionList() {
		return shell.executeCommand("job execution list");
	}

	/**
	 * Executes a task execution list.
	 */
	public CommandResult jobExecutionListByName(String jobName) {
		return shell.executeCommand("job execution list --name " + jobName);
	}

	/**
	 * Return the results of executing the shell command:
	 * <code>dataflow: job execution display --id 1</code> where id is the id for the job
	 * execution requested.
	 *
	 * @param id the identifier for the job execution.
	 * @return the results of the shell command.
	 */
	public CommandResult executionDisplay(long id) {
		return shell.executeCommand("job execution display --id " + id);
	}

	/**
	 * Return the results of executing the shell command:
	 * <code>dataflow: job instance display --id 1</code> where id is the id for the job
	 * instance requested.
	 *
	 * @param id the identifier for the job instance.
	 * @return the results of the shell command.
	 */
	public CommandResult instanceDisplay(long id) {
		return shell.executeCommand("job instance display --id " + id);
	}

	/**
	 * Return the results of executing the shell command:
	 * <code>dataflow: job execution step list --id 1</code> where id is the id for the
	 * job execution requested.
	 *
	 * @param id the identifier for the job execution.
	 * @return the results of the shell command.
	 */
	public CommandResult jobStepExecutionList(long id) {
		return shell.executeCommand("job execution step list --id " + id);
	}

	/**
	 * Return the results of executing the shell command:
	 * <code>dataflow: job execution step progress --id 1 --jobExecutionId 1</code> where
	 * id is the id for the step execution and jobExecutionId is for the jobExecution
	 * requested.
	 *
	 * @param id the identifier for the step execution.
	 * @param jobExecutionId the id for the job execution.
	 * @return the results of the shell command.
	 */
	public CommandResult jobStepExecutionProgress(long id, long jobExecutionId) {
		return shell.executeCommand("job execution step progress --id " + id + " --jobExecutionId " + jobExecutionId);
	}

	/**
	 * Return the results of executing the shell command:
	 * <code>dataflow: job execution step display --id 1 --jobExecutionId 1</code> where
	 * id is the id for the step execution and jobExecutionId is for the jobExecution
	 * requested.
	 *
	 * @param id the identifier for the step execution.
	 * @param jobExecutionId the id for the job execution.
	 * @return the results of the shell command.
	 */
	public CommandResult jobStepExecutionDisplay(long id, long jobExecutionId) {
		return shell.executeCommand("job execution step display --id " + id + " --jobExecutionId " + jobExecutionId);
	}
}
