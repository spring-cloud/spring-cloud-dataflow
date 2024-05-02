/*
 * Copyright 2023-2024 the original author or authors.
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
package org.springframework.cloud.dataflow.server.db.migration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorer;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.server.controller.support.TaskExecutionControllerDeleteAction;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.single.DataFlowServerApplication;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provides for testing some basic database schema and JPA tests to catch potential issues with specific databases early.
 *
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = DataFlowServerApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
			"spring.jpa.hibernate.ddl-auto=none",
			"logging.level.org.flywaydb=debug"
		}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ExtendWith(OutputCaptureExtension.class)
public abstract class AbstractSmokeTest {

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private DataflowTaskExplorer taskExplorer;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private TaskDeleteService taskDeleteService;

	private List<Long> executionIds = new ArrayList<>();

	@Test
	void streamCreation() {
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		tx.execute(status -> {
			StreamDefinition streamDefinition = new StreamDefinition("timelogger", "time | log");
			streamDefinition = streamDefinitionRepository.save(streamDefinition);
			Optional<StreamDefinition> loaded = streamDefinitionRepository.findById(streamDefinition.getName());
			assertThat(loaded).isPresent();
			assertThat(loaded.get().getDslText()).isEqualTo("time | log");
			return true;
		});
	}

	@Test
	void taskCreation() {
		long originalCount = this.taskExplorer.getTaskExecutionCount();
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		tx.execute(status -> {
			TaskExecution taskExecution = taskRepository.createTaskExecution("test_task");
			executionIds.add(taskExecution.getExecutionId());
			assertThat(taskExecution.getExecutionId()).isGreaterThan(0L);
			return true;
		});
		long expectedNewCount = originalCount + 1;
		assertThat(taskExplorer.getTaskExecutionCount()).isEqualTo(expectedNewCount);
		List<TaskExecution> taskExecutions = taskExplorer.findAll(Pageable.ofSize(100)).getContent();
		assertThat(taskExecutions)
				.hasSize((int)expectedNewCount)
				.allSatisfy((taskExecution) -> assertThat(taskExecution.getExecutionId()).isNotEqualTo(0L));
	}

	//TODO: Boot3x followup Due to some changes the SQL being tested for is not being outputted by SCDF logs
	//Not sure if this is because dataflow should be in debug or the print was removed as a part of the migration.
	@Disabled
	@Test
	void shouldListJobExecutionsUsingPerformantRowNumberQuery(
			CapturedOutput output,
			@Autowired TaskJobService taskJobService,
			@Autowired TaskExecutionDao taskExecutionDao,
			@Autowired TaskBatchDao taskBatchDao) throws NoSuchJobExecutionException {
		Page<TaskJobExecution> jobExecutions = taskJobService.listJobExecutionsWithStepCount(Pageable.ofSize(100));
		int originalCount = jobExecutions.getContent().size();
		JobExecutionTestUtils testUtils = new JobExecutionTestUtils(taskExecutionDao, taskBatchDao);
		TaskExecution execution1 = testUtils.createSampleJob("job1", 1, BatchStatus.STARTED, new JobParameters());
		executionIds.add(execution1.getExecutionId());
		TaskExecution execution2 = testUtils.createSampleJob("job2", 3, BatchStatus.COMPLETED, new JobParameters());
		executionIds.add(execution2.getExecutionId());

		// Get all executions and ensure the count and that the row number function was (or not) used
		jobExecutions = taskJobService.listJobExecutionsWithStepCount(Pageable.ofSize(100));
		assertThat(jobExecutions).hasSize(originalCount + 4);
		String expectedSqlFragment = (this.supportsRowNumberFunction()) ?
				"as STEP_COUNT, ROW_NUMBER() OVER (PARTITION" :
				"as STEP_COUNT FROM BATCH_JOB_INSTANCE";
		Awaitility.waitAtMost(Duration.ofSeconds(5))
				.untilAsserted(() -> assertThat(output).contains(expectedSqlFragment));

		// Verify that paging works as well
		jobExecutions = taskJobService.listJobExecutionsWithStepCount(Pageable.ofSize(2).withPage(0));
		assertThat(jobExecutions).hasSize(2);
		jobExecutions = taskJobService.listJobExecutionsWithStepCount(Pageable.ofSize(2).withPage(1));
		assertThat(jobExecutions).hasSize(2);
	}

	@AfterEach
	void cleanupAfterTest() {
		Set<TaskExecutionControllerDeleteAction> actions = new HashSet<>();
		actions.add(TaskExecutionControllerDeleteAction.CLEANUP);
		actions.add(TaskExecutionControllerDeleteAction.REMOVE_DATA);
		this.taskDeleteService.cleanupExecutions(actions, new HashSet<>(executionIds));
	}

	protected boolean supportsRowNumberFunction() {
		return true;
	}
}
