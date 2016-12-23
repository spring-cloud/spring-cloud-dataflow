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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.util.Assert;

/**
 * Executes task launch request using Spring Cloud Data Flow's Restful API
 * then returns the execution id once the task launched.
 *
 * @author Glenn Renfro
 */
public class TaskLauncherTasklet implements Tasklet {

	private TaskOperations taskOperations;

	private Map<String, String> properties;

	private List<String> arguments;

	private String taskName;

	private final Logger logger = LoggerFactory.getLogger(TaskLauncherTasklet.class);

	private String taskExecutionId;

	public TaskLauncherTasklet(String taskExecutionId, TaskOperations taskOperations, String taskName,
			Map<String, String> properties, List<String> arguments) {
		Assert.hasText(taskName, "taskName must not be empty nor null.");
		Assert.notNull(taskOperations, "taskOperations must not be null.");

		this.taskName = taskName;
		if (properties == null) {
			this.properties = new HashMap<>();
		}
		else {
			this.properties = properties;
		}
		if (arguments == null) {
			this.arguments = new ArrayList<>();
		}
		else {
			this.arguments = arguments;
		}
		this.arguments.add("--spring.cloud.task.executionid=" + taskExecutionId);
		this.taskExecutionId = taskExecutionId;
		this.taskOperations = taskOperations;
	}

	/**
	 * Executes the task as specified by the taskName with the associated
	 * properties and arguments.
	 * @param contribution mutable state to be passed back to update the current step execution
	 * @param chunkContext contains the task-execution-id used by the listener.
	 * @return Repeat status of FINISHED.
	 */
	@Override
	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) throws Exception {
		logger.info(String.format("Launching %s", taskName));
		taskOperations.launch(taskName, this.properties, this.arguments);
		chunkContext.getStepContext().getStepExecution().getExecutionContext()
				.put("task-execution-id", taskExecutionId);
		return RepeatStatus.FINISHED;
	}

}
