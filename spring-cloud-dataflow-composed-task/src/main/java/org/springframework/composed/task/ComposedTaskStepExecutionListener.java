/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.composed.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;

/**
 * Listener for the TaskLauncherTasklet that waits for the task to complete
 * and sets the appropriate result for this step based on the launched task
 * exit code.
 * @author Glenn Renfro
 */
public class ComposedTaskStepExecutionListener implements StepExecutionListener{

	private TaskExplorer taskExplorer;

	private ComposedTaskProperties properties;

	private final Logger logger = LoggerFactory.getLogger(ComposedTaskStepExecutionListener.class);

	public ComposedTaskStepExecutionListener(TaskExplorer taskExplorer, ComposedTaskProperties properties) {
		this.taskExplorer = taskExplorer;
		this.properties = properties;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {

	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		ExitStatus result = ExitStatus.COMPLETED;
		logger.info(String.format("AfterStep processing for stepExecution %s",
				stepExecution.getStepName()));

		String executionId = (String)stepExecution.getExecutionContext().get("task-execution-id");
		if(executionId == null) {
			throw new IllegalStateException("TaskLauncherTasklet did not " +
					"return a task-execution-id.  Check to see if task " +
					"exists.");
		}
		boolean waitStatus = waitForTaskToComplete(executionId);
		if(!waitStatus) {
			result = ExitStatus.FAILED;
		}
		else {
			TaskExecution resultExecution = taskExplorer.getTaskExecution(Long.valueOf(executionId));
			if(resultExecution.getExitCode() != 0) {
				result = ExitStatus.FAILED;
			}
		}
		logger.info(String.format("AfterStep processing complete for " +
						"stepExecution %s with taskExecution %s",
				stepExecution.getStepName(), executionId));
		return result;
	}

	private boolean waitForTaskToComplete(String taskExecutionId) {
		long timeout = System.currentTimeMillis() + (
				properties.getMaxWaitTime());
		boolean isComplete = false;
		while (!isComplete && System.currentTimeMillis() < timeout) {
			try {
				Thread.sleep(properties.getIntervalTimeBetweenChecks());
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e.getMessage(), e);
			}
			TaskExecution taskExecution =
					taskExplorer.getTaskExecution(Long.valueOf(taskExecutionId));
			if(taskExecution != null && taskExecution.getEndTime() != null) {
				isComplete = true;
			}

		}
		return isComplete;
	}
}
