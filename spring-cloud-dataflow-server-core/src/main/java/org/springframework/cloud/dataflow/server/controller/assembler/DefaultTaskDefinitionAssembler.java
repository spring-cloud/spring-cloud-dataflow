/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.cloud.dataflow.server.controller.assembler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.cloud.dataflow.rest.util.TaskSanitizer;
import org.springframework.cloud.dataflow.server.controller.TaskDefinitionController;
import org.springframework.cloud.dataflow.server.controller.support.TaskExecutionAwareTaskDefinition;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorer;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.service.impl.TaskServiceUtils;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;

/**
 * {@link org.springframework.hateoas.server.RepresentationModelAssembler} implementation that converts
 * {@link TaskDefinition}s to {@link TaskDefinitionResource}s.
 *
 * @author Ilayaperumal Gopinathan
 * @author Evgeniy Bezdomnikov
 * @author Glenn Renfro
 * @author Chris Bono
 */
public class DefaultTaskDefinitionAssembler<R extends TaskDefinitionResource> extends
		RepresentationModelAssemblerSupport<TaskExecutionAwareTaskDefinition, R> {

	private static final Logger logger = LoggerFactory.getLogger(DefaultTaskDefinitionAssembler.class);

	private final TaskExecutionService taskExecutionService;

	private final TaskJobService taskJobService;

	private final DataflowTaskExplorer taskExplorer;

	private final TaskSanitizer taskSanitizer = new TaskSanitizer();

	private boolean enableManifest;

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();


	public DefaultTaskDefinitionAssembler(
			TaskExecutionService taskExecutionService,
			boolean enableManifest,
			Class<R> classType,
			TaskJobService taskJobService,
			DataflowTaskExplorer taskExplorer) {
		super(TaskDefinitionController.class, classType);
		this.taskExecutionService = taskExecutionService;
		this.enableManifest = enableManifest;
		this.taskJobService = taskJobService;
		this.taskExplorer = taskExplorer;
	}

	TaskDefinitionResource updateTaskExecutionResource(
			TaskExecutionAwareTaskDefinition taskExecutionAwareTaskDefinition,
			TaskDefinitionResource taskDefinitionResource, boolean manifest) {

		TaskExecution taskExecution = this.sanitizeTaskExecutionArguments(taskExecutionAwareTaskDefinition.getLatestTaskExecution());
		TaskManifest taskManifest = null;
		if (manifest) {
			taskManifest = this.taskExecutionService.findTaskManifestById(taskExecution.getExecutionId());
			taskManifest = this.taskSanitizer.sanitizeTaskManifest(taskManifest);
		}
		TaskJobExecution composedTaskJobExecution = null;
		if (taskExecution != null && taskDefinitionResource.isComposed()) {
			Set<Long> jobExecutionIds = this.taskExplorer.getJobExecutionIdsByTaskExecutionId(taskExecution.getExecutionId());
			if(jobExecutionIds != null && jobExecutionIds.size() > 0) {
				try {
					composedTaskJobExecution = this.taskJobService.getJobExecution(jobExecutionIds.toArray(new Long[0])[0]);
				} catch (NoSuchJobExecutionException noSuchJobExecutionException) {
					logger.warn("Job Execution for Task Execution {} could not be found.",
							taskExecution.getExecutionId());
				}
			}
		}
		TaskExecutionResource taskExecutionResource = (manifest && taskManifest != null) ?
				new TaskExecutionResource(taskExecution, taskManifest, composedTaskJobExecution) :
				new TaskExecutionResource(taskExecution, composedTaskJobExecution);
		taskDefinitionResource.setLastTaskExecution(taskExecutionResource);
		return taskDefinitionResource;
	}
	private TaskExecution sanitizeTaskExecutionArguments(TaskExecution taskExecution) {
		List<String> args = taskExecution.getArguments().stream()
				.map(this.argumentSanitizer::sanitize).collect(Collectors.toList());
		taskExecution.setArguments(args);
		return taskExecution;
	}
	@Override
	public R toModel(TaskExecutionAwareTaskDefinition taskExecutionAwareTaskDefinition) {
		return createModelWithId(taskExecutionAwareTaskDefinition.getTaskDefinition().getName(),
				taskExecutionAwareTaskDefinition);
	}

	@Override
	public R instantiateModel(
			TaskExecutionAwareTaskDefinition taskExecutionAwareTaskDefinition) {
		boolean composed = TaskServiceUtils
				.isComposedTaskDefinition(taskExecutionAwareTaskDefinition.getTaskDefinition().getDslText());
		TaskDefinitionResource taskDefinitionResource = new TaskDefinitionResource(
				taskExecutionAwareTaskDefinition.getTaskDefinition().getName(),
				argumentSanitizer.sanitizeTaskDsl(taskExecutionAwareTaskDefinition.getTaskDefinition()),
				taskExecutionAwareTaskDefinition.getTaskDefinition().getDescription());
		taskDefinitionResource.setComposed(composed);
		if (taskExecutionAwareTaskDefinition.getLatestTaskExecution() != null) {
			updateTaskExecutionResource(taskExecutionAwareTaskDefinition, taskDefinitionResource,
					this.isEnableManifest());
		}
		return (R) taskDefinitionResource;
	}

	/**
	 * Returns if the TaskExecution needs to be updated with the task manifest.
	 *
	 * @return the boolean value of to enable setting the manifest
	 */
	public boolean isEnableManifest() {
		return enableManifest;
	}

	/**
	 * Set the flag to indicate whether the task manifest needs to be updated for the TaskExecution.
	 *
	 * @param enableManifest the boolean value of to enable setting the manifest
	 */
	public void setEnableManifest(boolean enableManifest) {
		this.enableManifest = enableManifest;
	}
}
