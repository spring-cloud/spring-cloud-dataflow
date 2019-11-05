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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.EntityManager;

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
import org.springframework.cloud.dataflow.server.controller.support.TaskExecutionControllerDeleteAction;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.CannotDeleteNonParentTaskExecutionException;
import org.springframework.cloud.dataflow.server.repository.DataflowJobExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionManifestRepository;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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

	protected final DataflowTaskExecutionDao dataflowTaskExecutionDao;

	protected final DataflowJobExecutionDao dataflowJobExecutionDao;

	protected final TaskExecutionManifestRepository taskExecutionManifestRepository;

	private SchedulerService schedulerService;

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	private final EntityManager entityManager;

	public DefaultTaskDeleteService(TaskExplorer taskExplorer, LauncherRepository launcherRepository,
			TaskDefinitionRepository taskDefinitionRepository,
			TaskDeploymentRepository taskDeploymentRepository,
			AuditRecordService auditRecordService,
			DataflowTaskExecutionDao dataflowTaskExecutionDao,
			DataflowJobExecutionDao dataflowJobExecutionDao,
			TaskExecutionManifestRepository taskExecutionManifestRepository,
			EntityManager entityManager,
			SchedulerService schedulerService) {
		Assert.notNull(taskExplorer, "TaskExplorer must not be null");
		Assert.notNull(launcherRepository, "LauncherRepository must not be null");
		Assert.notNull(taskDefinitionRepository, "TaskDefinitionRepository must not be null");
		Assert.notNull(taskDeploymentRepository, "TaskDeploymentRepository must not be null");
		Assert.notNull(auditRecordService, "AuditRecordService must not be null");
		Assert.notNull(dataflowTaskExecutionDao, "DataflowTaskExecutionDao must not be null");
		Assert.notNull(dataflowJobExecutionDao, "DataflowJobExecutionDao must not be null");
		Assert.notNull(taskExecutionManifestRepository, "TaskExecutionManifestRepository must not be null");
		this.taskExplorer = taskExplorer;
		this.launcherRepository = launcherRepository;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.taskDeploymentRepository = taskDeploymentRepository;
		this.auditRecordService = auditRecordService;
		this.dataflowTaskExecutionDao = dataflowTaskExecutionDao;
		this.dataflowJobExecutionDao = dataflowJobExecutionDao;
		this.taskExecutionManifestRepository = taskExecutionManifestRepository;
		this.schedulerService = schedulerService;
		this.entityManager = entityManager;
	}

	@Override
	public void cleanupExecution(long id) {
		TaskExecution taskExecution = taskExplorer.getTaskExecution(id);
		Assert.notNull(taskExecution, "There was no task execution with id " + id);
		String launchId = taskExecution.getExternalExecutionId();
		Assert.hasLength(launchId, "The TaskExecution for id " + id + " did not have an externalExecutionId");
		TaskDeployment taskDeployment = this.taskDeploymentRepository.findByTaskDeploymentId(launchId);
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
	@Transactional
	public void cleanupExecutions(Set<TaskExecutionControllerDeleteAction> actionsAsSet, Set<Long> ids) {
		final SortedSet<Long> nonExistingTaskExecutions = new TreeSet<>();
		final SortedSet<Long> nonParentTaskExecutions = new TreeSet<>();

		for (Long id : ids) {
			final TaskExecution taskExecution = this.taskExplorer.getTaskExecution(id);
			if (taskExecution == null) {
				nonExistingTaskExecutions.add(id);
			}
			else {
				final Long parentExecutionId = taskExecution.getParentExecutionId();

				if (parentExecutionId != null) {
					nonParentTaskExecutions.add(parentExecutionId);
				}
			}
		}

		if (!nonExistingTaskExecutions.isEmpty()) {
			if (nonExistingTaskExecutions.size() == 1) {
				throw new NoSuchTaskExecutionException(nonExistingTaskExecutions.first());
			}
			else {
				throw new NoSuchTaskExecutionException(nonExistingTaskExecutions);
			}
		}

		if (actionsAsSet.contains(TaskExecutionControllerDeleteAction.CLEANUP)) {
			for (Long id : ids) {
				this.cleanupExecution(id);
			}
		}

		if (!nonParentTaskExecutions.isEmpty()) {
			if (nonParentTaskExecutions.size() == 1) {
				throw new CannotDeleteNonParentTaskExecutionException(nonParentTaskExecutions.first());
			}
			else {
				throw new CannotDeleteNonParentTaskExecutionException(nonParentTaskExecutions);
			}
		}

		if (actionsAsSet.contains(TaskExecutionControllerDeleteAction.REMOVE_DATA)) {
			this.deleteTaskExecutions(ids);
		}
	}

	@Override
	@Transactional
	public void deleteTaskExecutions(Set<Long> taskExecutionIds) {
		Assert.notEmpty(taskExecutionIds, "You must provide at least 1 task execution id.");

		final Set<Long> taskExecutionIdsWithChildren = new HashSet<>(taskExecutionIds);

		final Set<Long> childTaskExecutionIds = dataflowTaskExecutionDao.findChildTaskExecutionIds(taskExecutionIds);
		logger.info("Found {} child task execution ids: {}.", childTaskExecutionIds.size(), StringUtils.collectionToCommaDelimitedString(childTaskExecutionIds));
		taskExecutionIdsWithChildren.addAll(childTaskExecutionIds);

		final Map<String, Object> auditData = new LinkedHashMap<>();
		auditData.put("Deleted Task Executions", taskExecutionIdsWithChildren.size());

		logger.info("Deleting {} task executions.", taskExecutionIdsWithChildren.size());

		// Retrieve Related Job Executions

		final Set<Long> jobExecutionIds = new HashSet<>();

		for (Long taskExecutionId : taskExecutionIdsWithChildren) {
			jobExecutionIds.addAll(taskExplorer.getJobExecutionIdsByTaskExecutionId(taskExecutionId));
		}

		logger.info("There are {} associated job executions.", jobExecutionIds.size());

		// Remove Batch Related Data if needed
		auditData.put("Deleted # of Job Executions", jobExecutionIds.size());
		auditData.put("Deleted Job Execution IDs", StringUtils.collectionToDelimitedString(jobExecutionIds, ", "));

		if (!jobExecutionIds.isEmpty()) {
			final Set<Long> stepExecutionIds = dataflowJobExecutionDao.findStepExecutionIds(jobExecutionIds);

			final int numberOfDeletedBatchStepExecutionContextRows;
			if (!stepExecutionIds.isEmpty()) {
				numberOfDeletedBatchStepExecutionContextRows = dataflowJobExecutionDao.deleteBatchStepExecutionContextByStepExecutionIds(stepExecutionIds);
			}
			else {
				numberOfDeletedBatchStepExecutionContextRows = 0;
			}

			final int numberOfDeletedBatchStepExecutionRows = dataflowJobExecutionDao.deleteBatchStepExecutionsByJobExecutionIds(jobExecutionIds);
			final int numberOfDeletedBatchJobExecutionContextRows = dataflowJobExecutionDao.deleteBatchJobExecutionContextByJobExecutionIds(jobExecutionIds);
			final int numberOfDeletedBatchJobExecutionParamRows = dataflowJobExecutionDao.deleteBatchJobExecutionParamsByJobExecutionIds(jobExecutionIds);
			final int numberOfDeletedBatchJobExecutionRows = dataflowJobExecutionDao.deleteBatchJobExecutionByJobExecutionIds(jobExecutionIds);
			final int numberOfDeletedUnusedBatchJobInstanceRows = dataflowJobExecutionDao.deleteUnusedBatchJobInstances();

			logger.info("Deleted the following Batch Job Execution related data for {} Job Executions.\n" +
					"Batch Step Execution Context Rows: {}\n" +
					"Batch Step Executions Rows:        {}\n" +
					"Batch Job Execution Context Rows:  {}\n" +
					"Batch Job Execution Param Rows:    {}\n" +
					"Batch Job Execution Rows:          {}\n" +
					"Batch Job Instance Rows:           {}.",
					jobExecutionIds.size(),
					numberOfDeletedBatchStepExecutionContextRows,
					numberOfDeletedBatchStepExecutionRows,
					numberOfDeletedBatchJobExecutionContextRows,
					numberOfDeletedBatchJobExecutionParamRows,
					numberOfDeletedBatchJobExecutionRows,
					numberOfDeletedUnusedBatchJobInstanceRows
					);

			auditData.put("Batch Step Execution Context", numberOfDeletedBatchStepExecutionContextRows);
			auditData.put("Batch Step Executions", numberOfDeletedBatchStepExecutionRows);
			auditData.put("Batch Job Execution Context Rows", numberOfDeletedBatchJobExecutionContextRows);
			auditData.put("Batch Job Execution Params", numberOfDeletedBatchJobExecutionParamRows);
			auditData.put("Batch Job Executions", numberOfDeletedBatchJobExecutionRows);
			auditData.put("Batch Job Instance Rows", numberOfDeletedUnusedBatchJobInstanceRows);
		}

		// Delete Task Related Data

		final int numberOfDeletedTaskExecutionParamRows = dataflowTaskExecutionDao.deleteTaskExecutionParamsByTaskExecutionIds(taskExecutionIdsWithChildren);
		final int numberOfDeletedTaskTaskBatchRelationshipRows = dataflowTaskExecutionDao.deleteTaskTaskBatchRelationshipsByTaskExecutionIds(taskExecutionIdsWithChildren);

		int numberOfDeletedTaskExecutionManifestRows = 0;
		for (Long taskExecutionId : taskExecutionIdsWithChildren) {
			numberOfDeletedTaskExecutionManifestRows += this.taskExecutionManifestRepository.deleteTaskExecutionManifestByTaskExecutionId(taskExecutionId);
		}

		this.entityManager.flush();

		final int numberOfDeletedTaskExecutionRows = dataflowTaskExecutionDao.deleteTaskExecutionsByTaskExecutionIds(taskExecutionIdsWithChildren);

		auditData.put("Deleted # of Task Executions", numberOfDeletedTaskExecutionRows);
		auditData.put("Deleted Task Execution IDs", StringUtils.collectionToDelimitedString(taskExecutionIdsWithChildren, ", "));

		logger.info("Deleted the following Task Execution related data for {} Task Executions:\n" +
				"Task Execution Param Rows:    {}\n" +
				"Task Batch Relationship Rows: {}\n" +
				"Task Manifest Rows:           {}\n" +
				"Task Execution Rows:          {}.",
				taskExecutionIdsWithChildren.size(),
				numberOfDeletedTaskExecutionParamRows,
				numberOfDeletedTaskTaskBatchRelationshipRows,
				numberOfDeletedTaskExecutionManifestRows,
				numberOfDeletedTaskExecutionRows
				);

		// Populate Audit Record

		auditRecordService.populateAndSaveAuditRecordUsingMapData(
				AuditOperationType.TASK, AuditActionType.DELETE,
				taskExecutionIdsWithChildren.size() + " Task Execution Delete(s)", auditData);
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
		if (this.schedulerService != null) {
			schedulerService.unscheduleForTaskDefinition(taskDefinition.getTaskName());
		}
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
