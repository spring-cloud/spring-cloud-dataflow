/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDeployment;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link TaskDeleteService} interface. Provide service
 * methods for Task deletion.
 *
 * @author Michael Minella
 * @author Marius Bogoevici
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Janne Valkealahti
 * @author Gunnar Hillert
 * @author Thomas Risberg
 * @author Ilayaperumal Gopinathan
 * @author Michael Wirth
 * @author David Turanski
 * @author Daniel Serleg
 */
public class DefaultTaskDeleteService implements TaskDeleteService {

	private static final Logger logger = LoggerFactory.getLogger(DefaultTaskDeleteService.class);

	/**
	 * Used to read TaskExecutions.
	 */
	private final TaskExplorer taskExplorer;

	private final LauncherRepository launcherRepository;

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final TaskDeploymentRepository taskDeploymentRepository;

	protected final AuditRecordService auditRecordService;

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	public DefaultTaskDeleteService(TaskExplorer taskExplorer, LauncherRepository launcherRepository,
			TaskDefinitionRepository taskDefinitionRepository,
			TaskDeploymentRepository taskDeploymentRepository,
			AuditRecordService auditRecordService) {
		Assert.notNull(taskExplorer, "TaskExplorer must not be null");
		Assert.notNull(launcherRepository, "LauncherRepository must not be null");
		Assert.notNull(taskDefinitionRepository, "TaskDefinitionRepository must not be null");
		Assert.notNull(taskDeploymentRepository, "TaskDeploymentRepository must not be null");
		Assert.notNull(auditRecordService, "AuditRecordService must not be null");

		this.taskExplorer = taskExplorer;
		this.launcherRepository = launcherRepository;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.taskDeploymentRepository = taskDeploymentRepository;
		this.auditRecordService = auditRecordService;
	}

	@Override
	public void cleanupExecution(long id) {
		TaskExecution taskExecution = taskExplorer.getTaskExecution(id);
		Assert.notNull(taskExecution, "There was no task execution with id " + id);
		String launchId = taskExecution.getExternalExecutionId();
		Assert.hasLength(launchId, "The TaskExecution for id " + id + " did not have an externalExecutionId");
		TaskDeployment taskDeployment = this.taskDeploymentRepository.findByTaskDeploymentId(String.valueOf(id));
		if (taskDeployment == null) {
			logger.warn(String.format("Did not find TaskDeployment for taskName = [%s], taskId = [%s].  Nothing to clean up.",
					taskExecution.getTaskName(), id));
			return;
		}
		Launcher launcher = launcherRepository.findByName(taskDeployment.getPlatformName());
		if (launcher != null) {
			TaskLauncher taskLauncher = launcher.getTaskLauncher();
			taskLauncher.cleanup(launchId);
		}
		else {
			logger.info(
					"Could clean up execution for task id " + id + ". Did not find a task platform named " +
							taskDeployment.getPlatformName());
		}
	}

	@Override
	public void deleteTaskDefinition(String name) {
		TaskDefinition taskDefinition = this.taskDefinitionRepository.findById(name)
				.orElseThrow(() -> new NoSuchTaskDefinitionException(name));

		deleteTaskDefinition(taskDefinition);

		auditRecordService.populateAndSaveAuditRecord(
				AuditOperationType.TASK, AuditActionType.DELETE,
				taskDefinition.getTaskName(), this.argumentSanitizer.sanitizeTaskDsl(taskDefinition));
	}

	@Override
	public void deleteAll() {
		Iterable<TaskDefinition> allTaskDefinition = this.taskDefinitionRepository.findAll();

		for (TaskDefinition taskDefinition : allTaskDefinition) {
			deleteTaskDefinition(taskDefinition);

			auditRecordService.populateAndSaveAuditRecord(
					AuditOperationType.TASK, AuditActionType.DELETE,
					taskDefinition.getTaskName(), this.argumentSanitizer.sanitizeTaskDsl(taskDefinition));
		}
	}

	private void deleteTaskDefinition(TaskDefinition taskDefinition) {
		TaskParser taskParser = new TaskParser(taskDefinition.getName(), taskDefinition.getDslText(), true, true);
		TaskNode taskNode = taskParser.parse();
		// if composed-task-runner definition then destroy all child tasks associated with it.
		if (taskNode.isComposed()) {
			String childTaskPrefix = TaskNode.getTaskPrefix(taskDefinition.getTaskName());
			// destroy composed child tasks
			taskNode.getTaskApps().forEach(task -> {
				String childName = task.getName();
				if (task.getLabel() != null) {
					childName = task.getLabel();
				}
				destroyChildTask(childTaskPrefix + childName);
			});
		}
		// destroy normal task or composed parent task
		destroyPrimaryTask(taskDefinition.getTaskName());
	}

	private void destroyPrimaryTask(String name) {
		TaskDefinition taskDefinition = taskDefinitionRepository.findById(name)
				.orElseThrow(() -> new NoSuchTaskDefinitionException(name));
		destroyTask(taskDefinition);
	}

	private void destroyChildTask(String name) {
		Optional<TaskDefinition> taskDefinition = taskDefinitionRepository.findById(name);
		taskDefinition.ifPresent(this::destroyTask);
	}

	private void destroyTask(TaskDefinition taskDefinition) {
		taskDefinitionRepository.deleteById(taskDefinition.getName());
		TaskDeployment taskDeployment =
				this.taskDeploymentRepository.findTopByTaskDefinitionNameOrderByCreatedOnAsc(taskDefinition.getTaskName());
		if (taskDeployment != null) {
			Launcher launcher = launcherRepository.findByName(taskDeployment.getPlatformName());
			if (launcher != null) {
				TaskLauncher taskLauncher = launcher.getTaskLauncher();
				taskLauncher.destroy(taskDefinition.getName());
			}
		}
		else {
			logger.info("TaskLauncher.destroy not invoked for task " +
					taskDefinition.getTaskName() + ". Did not find a previously launched task to destroy.");
		}
	}
}
