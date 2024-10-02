/*
 * Copyright 2016-2022 the original author or authors.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDeployment;
import org.springframework.cloud.dataflow.core.database.support.DatabaseType;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.cloud.dataflow.server.controller.support.TaskExecutionControllerDeleteAction;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.DataflowJobExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
 * @author Corneil du Plessis
 * @author Joe O'Brien
 */
public class DefaultTaskDeleteService implements TaskDeleteService {

	private static final Logger logger = LoggerFactory.getLogger(DefaultTaskDeleteService.class);

	private static final int SQL_SERVER_CHUNK_SIZE = 2098;

	private static final int ORACLE_SERVER_CHUNK_SIZE = 998;

	/**
	 * Used to read TaskExecutions.
	 */
	private final DataflowTaskExplorer taskExplorer;

	private final LauncherRepository launcherRepository;

	private final TaskDefinitionRepository taskDefinitionRepository;

	private final TaskDeploymentRepository taskDeploymentRepository;

	protected final AuditRecordService auditRecordService;

	protected final DataflowTaskExecutionDao dataflowTaskExecutionDao;

	protected final DataflowJobExecutionDao dataflowJobExecutionDao;

	protected final DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao;

	private final SchedulerService schedulerService;

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	private final int taskDeleteChunkSize;

	private final DataSource dataSource;

	public DefaultTaskDeleteService(
			DataflowTaskExplorer taskExplorer,
			LauncherRepository launcherRepository,
			TaskDefinitionRepository taskDefinitionRepository,
			TaskDeploymentRepository taskDeploymentRepository,
			AuditRecordService auditRecordService,
			DataflowTaskExecutionDao dataflowTaskExecutionDao,
			DataflowJobExecutionDao dataflowJobExecutionDao,
			DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao,
			SchedulerService schedulerService,
			TaskConfigurationProperties taskConfigurationProperties,
			DataSource dataSource
	) {
		Assert.notNull(taskExplorer, "TaskExplorer must not be null");
		Assert.notNull(launcherRepository, "LauncherRepository must not be null");
		Assert.notNull(taskDefinitionRepository, "TaskDefinitionRepository must not be null");
		Assert.notNull(taskDeploymentRepository, "TaskDeploymentRepository must not be null");
		Assert.notNull(auditRecordService, "AuditRecordService must not be null");
		Assert.notNull(dataflowTaskExecutionDao, "DataflowTaskExecutionDao must not be null");
		Assert.notNull(dataflowJobExecutionDao, "DataflowJobExecutionDao must not be null");
		Assert.notNull(dataflowTaskExecutionMetadataDao, "DataflowTaskExecutionMetadataDao must not be null");
		Assert.notNull(taskConfigurationProperties, "TaskConfigurationProperties must not be null");
		Assert.notNull(dataSource, "DataSource must not be null");

		this.taskExplorer = taskExplorer;
		this.launcherRepository = launcherRepository;
		this.taskDefinitionRepository = taskDefinitionRepository;
		this.taskDeploymentRepository = taskDeploymentRepository;
		this.auditRecordService = auditRecordService;
		this.dataflowTaskExecutionDao = dataflowTaskExecutionDao;
		this.dataflowJobExecutionDao = dataflowJobExecutionDao;
		this.dataflowTaskExecutionMetadataDao = dataflowTaskExecutionMetadataDao;
		this.schedulerService = schedulerService;
		this.taskDeleteChunkSize = taskConfigurationProperties.getExecutionDeleteChunkSize();
		this.dataSource = dataSource;
	}

	@Override
	@Transactional
	public void cleanupExecution(long id) {
		performCleanupExecution(id);
	}

	private void performCleanupExecution(long id) {
		TaskExecution taskExecution = taskExplorer.getTaskExecution(id);
		Assert.notNull(taskExecution, "There was no task execution with id " + id);
		String launchId = taskExecution.getExternalExecutionId();
		if (!StringUtils.hasText(launchId)) {
			logger.warn("Did not find External execution ID for taskName = [{}], taskId = [{}].  Nothing to clean up.", taskExecution.getTaskName(), id);
			return;
		}
		TaskDeployment taskDeployment = this.taskDeploymentRepository.findByTaskDeploymentId(launchId);
		if (taskDeployment == null) {
			logger.warn("Did not find TaskDeployment for taskName = [{}], taskId = [{}].  Nothing to clean up.", taskExecution.getTaskName(), id);
			return;
		}
		Launcher launcher = launcherRepository.findByName(taskDeployment.getPlatformName());
		if (launcher != null) {
			TaskLauncher taskLauncher = launcher.getTaskLauncher();
			taskLauncher.cleanup(launchId);
		} else {
			logger.info("Could clean up execution for task id " + id + ". Did not find a task platform named " + taskDeployment.getPlatformName());
		}
	}

	@Override
	@Transactional
	public void cleanupExecutions(Set<TaskExecutionControllerDeleteAction> actionsAsSet, String taskName, boolean completed) {
		cleanupExecutions(actionsAsSet, taskName, completed, null);
	}

	@Override
	@Transactional
	public void cleanupExecutions(Set<TaskExecutionControllerDeleteAction> actionsAsSet, String taskName, boolean completed, Integer days) {
		List<TaskExecution> tasks;
		if (days != null) {
			tasks = this.taskExplorer.findTaskExecutionsBeforeEndTime(taskName, TaskServicesDateUtils.numDaysAgoFromLocalMidnightToday(days));
		} else {
			tasks = this.taskExplorer.findTaskExecutions(taskName, completed);
		}
		final Set<TaskExecution> parentExecutions = new HashSet<>();
		final Set<TaskExecution> childExecutions = new HashSet<>();
		boolean removeData = actionsAsSet.contains(TaskExecutionControllerDeleteAction.REMOVE_DATA);
		boolean cleanUp = actionsAsSet.contains(TaskExecutionControllerDeleteAction.CLEANUP);
		for (TaskExecution taskExecution : tasks) {
			if (taskExecution.getParentExecutionId() == null) {
				parentExecutions.add(taskExecution);
			} else {
				childExecutions.add(taskExecution);
			}
		}
		if (cleanUp) {
			for (TaskExecution taskExecution : tasks) {
				this.performCleanupExecution(taskExecution.getExecutionId());
			}
		}

		if (removeData) {
			if (!childExecutions.isEmpty()) {
				deleteTaskExecutions(childExecutions);
			}
			if (!parentExecutions.isEmpty()) {
				SortedSet<Long> parentIds = parentExecutions
					.stream()
					.map(TaskExecution::getExecutionId)
					.collect(Collectors.toCollection(TreeSet::new));
				List<TaskExecution> children = this.taskExplorer.findChildTaskExecutions(parentIds);
				SortedSet<Long> childIds = children
					.stream()
					.map(TaskExecution::getExecutionId)
					.collect(Collectors.toCollection(TreeSet::new));
				if(childIds.size() > 0) {
					this.performDeleteTaskExecutions(childIds);
				}
				if(parentIds.size() > 0) {
					this.performDeleteTaskExecutions(parentIds);
				}
			}
		}
	}

	private void deleteTaskExecutions(Collection<TaskExecution> taskExecutions) {
		List<TaskExecution> executions = taskExecutions.stream()
				.collect(Collectors.toList());
			SortedSet<Long> executionIds = executions
					.stream()
					.map(TaskExecution::getExecutionId)
					.collect(Collectors.toCollection(TreeSet::new));
			this.performDeleteTaskExecutions(executionIds);
	}

	@Override
	@Transactional
	public void cleanupExecutions(Set<TaskExecutionControllerDeleteAction> actionsAsSet, Set<Long> ids) {
		performCleanupExecutions(actionsAsSet, ids);
	}

	private void performCleanupExecutions(Set<TaskExecutionControllerDeleteAction> actionsAsSet, Set<Long> ids) {
		final SortedSet<Long> nonExistingTaskExecutions = new TreeSet<>();
		final SortedSet<Long> parentExecutions = new TreeSet<>();
		final SortedSet<Long> childExecutions = new TreeSet<>();
		boolean removeData = actionsAsSet.contains(TaskExecutionControllerDeleteAction.REMOVE_DATA);
		boolean cleanUp = actionsAsSet.contains(TaskExecutionControllerDeleteAction.CLEANUP);
		for (Long id : ids) {
			final TaskExecution taskExecution = this.taskExplorer.getTaskExecution(id);
			if (taskExecution == null) {
				nonExistingTaskExecutions.add(id);
			} else if (taskExecution.getParentExecutionId() == null) {
				parentExecutions.add(taskExecution.getExecutionId());
			} else {
				childExecutions.add(taskExecution.getExecutionId());
			}
		}
		if (!nonExistingTaskExecutions.isEmpty()) {
			if (nonExistingTaskExecutions.size() == 1) {
				throw new NoSuchTaskExecutionException(nonExistingTaskExecutions.first());
			} else {
				throw new NoSuchTaskExecutionException(nonExistingTaskExecutions);
			}
		}

		if (cleanUp) {
			for (Long id : ids) {
				this.performCleanupExecution(id);
			}
		}

		if (removeData) {
			if (!childExecutions.isEmpty()) {
				this.performDeleteTaskExecutions(childExecutions);
			}
			if (!parentExecutions.isEmpty()) {
				List<TaskExecution> children = this.taskExplorer.findChildTaskExecutions(parentExecutions);
				if (!children.isEmpty()) {
					this.deleteTaskExecutions(children);
				}
				this.performDeleteTaskExecutions(parentExecutions);
			}
		}
	}

	@Override
	@Transactional
	public void deleteTaskExecutions(Set<Long> taskExecutionIds) {
		performDeleteTaskExecutions(taskExecutionIds);
	}

	@Override
	public void deleteTaskExecutions(String taskName, boolean onlyCompleted) {
		List<TaskExecution> taskExecutions = this.taskExplorer.findTaskExecutions(taskName, onlyCompleted);

			Set<Long> executionIds = taskExecutions
					.stream()
					.map(TaskExecution::getExecutionId)
					.collect(Collectors.toCollection(TreeSet::new));
			performDeleteTaskExecutions(executionIds);
	}

	private void performDeleteTaskExecutions(Set<Long> taskExecutionIds) {
		logger.info("performDeleteTaskExecutions:{}", taskExecutionIds);
		Assert.notEmpty(taskExecutionIds, "You must provide at least 1 task execution id.");

		final Set<Long> taskExecutionIdsWithChildren = new HashSet<>(taskExecutionIds);
		final Set<Long> childTaskExecutionIds = dataflowTaskExecutionDao.findChildTaskExecutionIds(taskExecutionIds);
		logger.info("Found {} child task execution ids: {}.",
				childTaskExecutionIds.size(),
				StringUtils.collectionToCommaDelimitedString(childTaskExecutionIds));
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

		int chunkSize = getTaskExecutionDeleteChunkSize(this.dataSource);

		if (!jobExecutionIds.isEmpty()) {
			deleteRelatedJobAndStepExecutions(jobExecutionIds, auditData, chunkSize);
		}

		// Delete Task Related Data

		auditData.put("Deleted # of Task Executions", taskExecutionIdsWithChildren.size());
		auditData.put("Deleted Task Execution IDs", StringUtils.collectionToDelimitedString(taskExecutionIdsWithChildren, ", "));

		final AtomicInteger numberOfDeletedTaskExecutionParamRows = new AtomicInteger(0);
		final AtomicInteger numberOfDeletedTaskTaskBatchRelationshipRows = new AtomicInteger(0);
		final AtomicInteger numberOfDeletedTaskManifestRows = new AtomicInteger(0);
		final AtomicInteger numberOfDeletedTaskExecutionRows = new AtomicInteger(0);

		if (chunkSize <= 0) {
			numberOfDeletedTaskExecutionParamRows.addAndGet(dataflowTaskExecutionDao.deleteTaskExecutionParamsByTaskExecutionIds(taskExecutionIdsWithChildren));
			numberOfDeletedTaskTaskBatchRelationshipRows.addAndGet(dataflowTaskExecutionDao.deleteTaskTaskBatchRelationshipsByTaskExecutionIds(
					taskExecutionIdsWithChildren));
			numberOfDeletedTaskManifestRows.addAndGet(dataflowTaskExecutionMetadataDao.deleteManifestsByTaskExecutionIds(taskExecutionIdsWithChildren));
			numberOfDeletedTaskExecutionRows.addAndGet(dataflowTaskExecutionDao.deleteTaskExecutionsByTaskExecutionIds(taskExecutionIdsWithChildren));
		} else {
			split(taskExecutionIdsWithChildren, chunkSize).forEach(taskExecutionIdSubsetList -> {
				Set<Long> taskExecutionIdSubset = new HashSet<>(taskExecutionIdSubsetList);
				numberOfDeletedTaskExecutionParamRows.addAndGet(dataflowTaskExecutionDao.deleteTaskExecutionParamsByTaskExecutionIds(taskExecutionIdSubset));
				numberOfDeletedTaskTaskBatchRelationshipRows.addAndGet(dataflowTaskExecutionDao.deleteTaskTaskBatchRelationshipsByTaskExecutionIds(
						taskExecutionIdSubset));
				numberOfDeletedTaskManifestRows.addAndGet(dataflowTaskExecutionMetadataDao.deleteManifestsByTaskExecutionIds(taskExecutionIdSubset));
				numberOfDeletedTaskExecutionRows.addAndGet(dataflowTaskExecutionDao.deleteTaskExecutionsByTaskExecutionIds(taskExecutionIdSubset));
			});
		}

		logger.info("""
						Deleted the following Task Execution related data for {} Task Executions:
						Task Execution Param Rows:    {}
						Task Batch Relationship Rows: {}
						Task Manifest Rows:           {}
						Task Execution Rows:          {}.""",
				taskExecutionIdsWithChildren.size(),
				numberOfDeletedTaskExecutionParamRows,
				numberOfDeletedTaskTaskBatchRelationshipRows,
				numberOfDeletedTaskManifestRows,
				numberOfDeletedTaskExecutionRows
		);

		// Populate Audit Record

		auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.TASK,
				AuditActionType.DELETE,
				taskExecutionIdsWithChildren.size() + " Task Execution Delete(s)",
				auditData,
				null);
	}

	private void deleteRelatedJobAndStepExecutions(Set<Long> jobExecutionIds, Map<String, Object> auditData, int chunkSize) {

		final Set<Long> stepExecutionIds = findStepExecutionIds(jobExecutionIds, chunkSize);

		final AtomicInteger numberOfDeletedBatchStepExecutionContextRows = new AtomicInteger(0);
		if (!stepExecutionIds.isEmpty()) {
			deleteBatchStepExecutionContextByStepExecutionIds(stepExecutionIds, chunkSize, numberOfDeletedBatchStepExecutionContextRows);
		}
		deleteStepAndJobExecutionsByJobExecutionId(jobExecutionIds, chunkSize, auditData, numberOfDeletedBatchStepExecutionContextRows);

	}

	private Set<Long> findStepExecutionIds(Set<Long> jobExecutionIds, int chunkSize) {
		final Set<Long> stepExecutionIds = ConcurrentHashMap.newKeySet();
		if (chunkSize <= 0) {
			stepExecutionIds.addAll(dataflowJobExecutionDao.findStepExecutionIds(jobExecutionIds));
		} else {
			split(jobExecutionIds, chunkSize).forEach(jobExecutionIdSubsetList -> {
				Set<Long> jobExecutionIdSubset = new HashSet<>(jobExecutionIdSubsetList);
				stepExecutionIds.addAll(dataflowJobExecutionDao.findStepExecutionIds(jobExecutionIdSubset));
			});
		}

		return stepExecutionIds;
	}

	private void deleteBatchStepExecutionContextByStepExecutionIds(
			Set<Long> stepExecutionIds,
			int chunkSize,
			AtomicInteger numberOfDeletedBatchStepExecutionContextRows) {
		if (chunkSize <= 0) {
			numberOfDeletedBatchStepExecutionContextRows.addAndGet(dataflowJobExecutionDao.deleteBatchStepExecutionContextByStepExecutionIds(stepExecutionIds));

		} else {
			split(stepExecutionIds, chunkSize).forEach(stepExecutionIdSubsetList -> {
				Set<Long> stepExecutionIdSubset = new HashSet<>(stepExecutionIdSubsetList);
				numberOfDeletedBatchStepExecutionContextRows.addAndGet(dataflowJobExecutionDao.deleteBatchStepExecutionContextByStepExecutionIds(
						stepExecutionIdSubset));
			});
		}
	}

	private void deleteStepAndJobExecutionsByJobExecutionId(
			Set<Long> jobExecutionIds,
			int chunkSize,
			Map<String, Object> auditData,
			AtomicInteger numberOfDeletedBatchStepExecutionContextRows) {
		final AtomicInteger numberOfDeletedBatchStepExecutionRows = new AtomicInteger(0);
		final AtomicInteger numberOfDeletedBatchJobExecutionContextRows = new AtomicInteger(0);
		final AtomicInteger numberOfDeletedBatchJobExecutionParamRows = new AtomicInteger(0);
		final AtomicInteger numberOfDeletedBatchJobExecutionRows = new AtomicInteger(0);

		if (chunkSize <= 0) {
			numberOfDeletedBatchStepExecutionRows.addAndGet(dataflowJobExecutionDao.deleteBatchStepExecutionsByJobExecutionIds(jobExecutionIds));
			numberOfDeletedBatchJobExecutionContextRows.addAndGet(dataflowJobExecutionDao.deleteBatchJobExecutionContextByJobExecutionIds(jobExecutionIds));
			numberOfDeletedBatchJobExecutionParamRows.addAndGet(dataflowJobExecutionDao.deleteBatchJobExecutionParamsByJobExecutionIds(jobExecutionIds));
			numberOfDeletedBatchJobExecutionRows.addAndGet(dataflowJobExecutionDao.deleteBatchJobExecutionByJobExecutionIds(jobExecutionIds));
		} else {
			split(jobExecutionIds, chunkSize).forEach(jobExecutionIdSubsetList -> {
				Set<Long> jobExecutionIdSubset = new HashSet<>(jobExecutionIdSubsetList);
				numberOfDeletedBatchStepExecutionRows.addAndGet(dataflowJobExecutionDao.deleteBatchStepExecutionsByJobExecutionIds(jobExecutionIdSubset));
				numberOfDeletedBatchJobExecutionContextRows.addAndGet(dataflowJobExecutionDao.deleteBatchJobExecutionContextByJobExecutionIds(
						jobExecutionIdSubset));
				numberOfDeletedBatchJobExecutionParamRows.addAndGet(dataflowJobExecutionDao.deleteBatchJobExecutionParamsByJobExecutionIds(jobExecutionIdSubset));
				numberOfDeletedBatchJobExecutionRows.addAndGet(dataflowJobExecutionDao.deleteBatchJobExecutionByJobExecutionIds(jobExecutionIdSubset));
			});
		}

		final int numberOfDeletedUnusedBatchJobInstanceRows = dataflowJobExecutionDao.deleteUnusedBatchJobInstances();

		logger.info("Deleted the following Batch Job Execution related data for {} Job Executions.\n" + "Batch Step Execution Context Rows: {}\n" + "Batch Step Executions Rows:        {}\n" + "Batch Job Execution Context Rows:  {}\n" + "Batch Job Execution Param Rows:    {}\n" + "Batch Job Execution Rows:          {}\n" + "Batch Job Instance Rows:           {}.",
				jobExecutionIds.size(),
				numberOfDeletedBatchStepExecutionContextRows,
				numberOfDeletedBatchStepExecutionRows,
				numberOfDeletedBatchJobExecutionContextRows,
				numberOfDeletedBatchJobExecutionParamRows,
				numberOfDeletedBatchJobExecutionRows,
				numberOfDeletedUnusedBatchJobInstanceRows);

		auditData.put("Batch Step Execution Context", numberOfDeletedBatchStepExecutionContextRows);
		auditData.put("Batch Step Executions", numberOfDeletedBatchStepExecutionRows);
		auditData.put("Batch Job Execution Context Rows", numberOfDeletedBatchJobExecutionContextRows);
		auditData.put("Batch Job Execution Params", numberOfDeletedBatchJobExecutionParamRows);
		auditData.put("Batch Job Executions", numberOfDeletedBatchJobExecutionRows);
		auditData.put("Batch Job Instance Rows", numberOfDeletedUnusedBatchJobInstanceRows);
	}

	/**
	 * Determines the maximum chunk size for a given database type.  If {@code taskDeleteChunkSize} is
	 * greater than zero this overrides the chunk size for the specific database type.
	 * If the database type has no fixed number of maximum elements allowed in the {@code IN} clause
	 * then zero is returned.
	 *
	 * @param dataSource the datasource used by data flow.
	 * @return the chunk size to be used for deleting task executions.
	 */
	private int getTaskExecutionDeleteChunkSize(DataSource dataSource) {
		int result = this.taskDeleteChunkSize;
		if (this.taskDeleteChunkSize < 1) {
			try {
				DatabaseType databaseType = DatabaseType.fromMetaData(dataSource);
				String name = databaseType.name();
				if (name.equals("SQLSERVER")) {
					result = SQL_SERVER_CHUNK_SIZE;
				}
				if (name.startsWith("ORACLE")) {
					result = ORACLE_SERVER_CHUNK_SIZE;
				}
			} catch (MetaDataAccessException mdae) {
				logger.warn("Unable to retrieve metadata for database when deleting task executions", mdae);
			}
		}
		return result;
	}

	static <T> Collection<List<T>> split(Collection<T> input, int max) {
		final AtomicInteger count = new AtomicInteger(0);
		return input.stream().collect(Collectors.groupingBy(s -> count.getAndIncrement() / max)).values();
	}

	@Override
	public void deleteTaskDefinition(String name) {
		TaskDefinition taskDefinition = this.taskDefinitionRepository.findById(name).orElseThrow(() -> new NoSuchTaskDefinitionException(name));

		deleteTaskDefinition(taskDefinition);

		auditRecordService.populateAndSaveAuditRecord(AuditOperationType.TASK,
				AuditActionType.DELETE,
				taskDefinition.getTaskName(),
				this.argumentSanitizer.sanitizeTaskDsl(taskDefinition),
				null);
	}

	@Override
	public void deleteTaskDefinition(String name, boolean cleanup) {
		if (cleanup) {
				Set<Long> taskExecutionIds = dataflowTaskExecutionDao.getTaskExecutionIdsByTaskName(name);
				final Set<TaskExecutionControllerDeleteAction> actionsAsSet = new HashSet<>();
				actionsAsSet.add(TaskExecutionControllerDeleteAction.CLEANUP);
				actionsAsSet.add(TaskExecutionControllerDeleteAction.REMOVE_DATA);
				if (!taskExecutionIds.isEmpty()) {
					performCleanupExecutions(actionsAsSet, taskExecutionIds);
				}
		}
		this.deleteTaskDefinition(name);
	}

	@Override
	public void deleteAll() {
		Iterable<TaskDefinition> allTaskDefinition = this.taskDefinitionRepository.findAll();

		for (TaskDefinition taskDefinition : allTaskDefinition) {
			deleteTaskDefinition(taskDefinition);

			auditRecordService.populateAndSaveAuditRecord(AuditOperationType.TASK,
					AuditActionType.DELETE,
					taskDefinition.getTaskName(),
					this.argumentSanitizer.sanitizeTaskDsl(taskDefinition),
					null);
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
				try {
					destroyChildTask(childTaskPrefix + childName);
				} catch (ObjectOptimisticLockingFailureException e) {
					logger.warn("Attempted delete on a child task that is currently being deleted");
				}
			});
		}
		// destroy normal task or composed parent task
		try {
			destroyPrimaryTask(taskDefinition.getTaskName());
		} catch (ObjectOptimisticLockingFailureException e) {
			logger.warn("Attempted delete on task {} that is currently being deleted", taskDefinition.getTaskName());
		}
	}

	private void destroyPrimaryTask(String name) {
		TaskDefinition taskDefinition = taskDefinitionRepository.findById(name).orElseThrow(() -> new NoSuchTaskDefinitionException(name));
		destroyTask(taskDefinition);
	}

	private void destroyChildTask(String name) {
		Optional<TaskDefinition> taskDefinition = taskDefinitionRepository.findById(name);
		taskDefinition.ifPresent(this::destroyTask);
	}

	private void destroyTask(TaskDefinition taskDefinition) {
		taskDefinitionRepository.deleteById(taskDefinition.getName());
		TaskDeployment taskDeployment = this.taskDeploymentRepository.findTopByTaskDefinitionNameOrderByCreatedOnAsc(taskDefinition.getTaskName());
		if (taskDeployment != null) {
			Launcher launcher = launcherRepository.findByName(taskDeployment.getPlatformName());
			if (launcher != null) {
				TaskLauncher taskLauncher = launcher.getTaskLauncher();
				taskLauncher.destroy(taskDefinition.getName());
			}
		} else {
			if (!findAndDeleteTaskResourcesAcrossPlatforms(taskDefinition)) {
				logger.info("TaskLauncher.destroy not invoked for task " + taskDefinition.getTaskName() + ". Did not find a previously launched task to destroy.");
			}
		}
	}

	private boolean findAndDeleteTaskResourcesAcrossPlatforms(TaskDefinition taskDefinition) {
		boolean result = false;
		for (Launcher launcher : launcherRepository.findAll()) {
			try {
				launcher.getTaskLauncher().destroy(taskDefinition.getName());
				logger.info("Deleted task app resources for {} in platform {}", taskDefinition.getName(), launcher.getName());
				result = true;
			} catch (Exception ex) {
				logger.info("Attempted delete of app resources for {} but none found on platform {}.", taskDefinition.getName(), launcher.getName());
			}
		}
		return result;
	}
}
