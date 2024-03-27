/*
 * Copyright 2023 the original author or authors.
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.dataflow.aggregate.task.AggregateTaskExplorer;
import org.springframework.cloud.dataflow.aggregate.task.TaskRepositoryContainer;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.schema.service.impl.DefaultSchemaService;
import org.springframework.cloud.dataflow.server.controller.support.TaskExecutionControllerDeleteAction;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskBatchDaoContainer;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionDaoContainer;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.single.DataFlowServerApplication;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provides for testing some basic database schema and JPA tests to catch potential issues with specific databases early.
 *
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = DataFlowServerApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.jpa.hibernate.ddl-auto=none")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ExtendWith(OutputCaptureExtension.class)
@Testcontainers
public abstract class AbstractSmokeTest {

	@Autowired
	private SchemaService schemaService;

	@Autowired
	private TaskRepositoryContainer taskRepositoryContainer;

	@Autowired
	private AggregateTaskExplorer taskExplorer;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private TaskDeleteService taskDeleteService;

	private MultiValueMap<SchemaVersionTarget, Long> createdExecutionIdsBySchemaTarget = new LinkedMultiValueMap<>();

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
			for (SchemaVersionTarget schemaVersionTarget : schemaService.getTargets().getSchemas()) {
				TaskRepository taskRepository = this.taskRepositoryContainer.get(schemaVersionTarget.getName());
				TaskExecution taskExecution = taskRepository.createTaskExecution(schemaVersionTarget.getName() + "_test_task");
				createdExecutionIdsBySchemaTarget.add(schemaVersionTarget, taskExecution.getExecutionId());
				assertThat(taskExecution.getExecutionId()).isGreaterThan(0L);
			}
			return true;
		});
		long expectedNewCount = originalCount + 2;
		assertThat(taskExplorer.getTaskExecutionCount()).isEqualTo(expectedNewCount);
		List<AggregateTaskExecution> taskExecutions = taskExplorer.findAll(Pageable.ofSize(100), true).getContent();
		assertThat(taskExecutions)
				.hasSize((int)expectedNewCount)
				.allSatisfy((taskExecution) -> assertThat(taskExecution.getExecutionId()).isNotEqualTo(0L));
	}

	@ParameterizedTest
	@MethodSource("schemaVersionTargetsProvider")
	void shouldListJobExecutionsUsingPerformantRowNumberQuery(
			SchemaVersionTarget schemaVersionTarget,
			CapturedOutput output,
			@Autowired TaskJobService taskJobService,
			@Autowired TaskExecutionDaoContainer taskExecutionDaoContainer,
			@Autowired TaskBatchDaoContainer taskBatchDaoContainer) throws NoSuchJobExecutionException {
		Page<TaskJobExecution> jobExecutions = taskJobService.listJobExecutionsWithStepCount(Pageable.ofSize(100));
		int originalCount = jobExecutions.getContent().size();
		JobExecutionTestUtils testUtils = new JobExecutionTestUtils(taskExecutionDaoContainer, taskBatchDaoContainer);
		TaskExecution execution1 = testUtils.createSampleJob("job1", 1, BatchStatus.STARTED, new JobParameters(), schemaVersionTarget);
		createdExecutionIdsBySchemaTarget.add(schemaVersionTarget, execution1.getExecutionId());
		TaskExecution execution2 = testUtils.createSampleJob("job2", 3, BatchStatus.COMPLETED, new JobParameters(), schemaVersionTarget);
		createdExecutionIdsBySchemaTarget.add(schemaVersionTarget, execution2.getExecutionId());

		// Get all executions and ensure the count and that the row number function was (or not) used
		jobExecutions = taskJobService.listJobExecutionsWithStepCount(Pageable.ofSize(100));
		assertThat(jobExecutions).hasSize(originalCount + 4);
		String expectedSqlFragment = (this.supportsRowNumberFunction()) ?
				"as STEP_COUNT, ROW_NUMBER() OVER (PARTITION" :
				"as STEP_COUNT FROM AGGREGATE_JOB_INSTANCE";
		Awaitility.waitAtMost(Duration.ofSeconds(5))
				.untilAsserted(() -> assertThat(output).contains(expectedSqlFragment));

		// Verify that paging works as well
		jobExecutions = taskJobService.listJobExecutionsWithStepCount(Pageable.ofSize(2).withPage(0));
		assertThat(jobExecutions).hasSize(2);
		jobExecutions = taskJobService.listJobExecutionsWithStepCount(Pageable.ofSize(2).withPage(1));
		assertThat(jobExecutions).hasSize(2);
	}

	static Stream<SchemaVersionTarget> schemaVersionTargetsProvider() {
		return new DefaultSchemaService().getTargets().getSchemas().stream();
	}

	@AfterEach
	void cleanupAfterTest() {
		Set<TaskExecutionControllerDeleteAction> actions = new HashSet<>();
		actions.add(TaskExecutionControllerDeleteAction.CLEANUP);
		actions.add(TaskExecutionControllerDeleteAction.REMOVE_DATA);
		createdExecutionIdsBySchemaTarget.forEach((schemaTarget, executionIds) ->
				this.taskDeleteService.cleanupExecutions(actions, new HashSet<>(executionIds), schemaTarget.getName()));
	}

	protected boolean supportsRowNumberFunction() {
		return true;
	}
}
