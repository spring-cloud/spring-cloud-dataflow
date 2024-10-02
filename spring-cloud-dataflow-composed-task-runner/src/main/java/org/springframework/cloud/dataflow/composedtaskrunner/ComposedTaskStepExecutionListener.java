/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Listener for the TaskLauncherTasklet that waits for the task to complete
 * and sets the appropriate result for this step based on the launched task
 * exit code.
 *
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
public class ComposedTaskStepExecutionListener implements StepExecutionListener {
	private final static Logger logger = LoggerFactory.getLogger(ComposedTaskStepExecutionListener.class);

	private final TaskExplorer taskExplorer;

	public ComposedTaskStepExecutionListener(TaskExplorer taskExplorer) {
		Assert.notNull(taskExplorer, "taskExplorer must not be null.");
		this.taskExplorer = taskExplorer;
	}

	/**
	 * If endTime for task is null then the ExitStatus will be set to  UNKNOWN.
	 * If an exitMessage is returned by the TaskExecution then the exit status
	 * returned will be the ExitMessage.  If no exitMessage is set for the task execution or
	 * {@link TaskLauncherTasklet#IGNORE_EXIT_MESSAGE_PROPERTY} is set to true as a task property
	 * and the task returns an exitCode != to zero an exit status of FAILED is
	 * returned.  If no exit message is set or
	 * {@link TaskLauncherTasklet#IGNORE_EXIT_MESSAGE_PROPERTY} is set to true as a task property
	 * and the exit code of the task is zero then the ExitStatus of COMPLETED is returned.
	 *
	 * @param stepExecution The stepExecution that kicked of the Task.
	 * @return ExitStatus of COMPLETED else FAILED.
	 */
	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		logger.info("AfterStep processing for stepExecution {}:{}", stepExecution.getStepName(), stepExecution.getJobExecutionId());
		ExitStatus result = ExitStatus.COMPLETED;
		Long executionId = (Long) stepExecution.getExecutionContext().get("task-execution-id");
		Assert.notNull(executionId, "TaskLauncherTasklet for job " + stepExecution.getJobExecutionId() +
				" did not return a task-execution-id. Check to see if task exists.");
		TaskExecution resultExecution = taskExplorer.getTaskExecution(executionId);
		if (!stepExecution.getExecutionContext().containsKey(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE) &&
				StringUtils.hasText(resultExecution.getExitMessage())) {
			result = new ExitStatus(resultExecution.getExitMessage());
		} else if (resultExecution.getExitCode() != 0) {
			result = ExitStatus.FAILED;
		}
		logger.info("AfterStep processing complete for stepExecution {} with taskExecution {}:{}", stepExecution.getStepName(), stepExecution.getJobExecutionId());
		return result;
	}
}
