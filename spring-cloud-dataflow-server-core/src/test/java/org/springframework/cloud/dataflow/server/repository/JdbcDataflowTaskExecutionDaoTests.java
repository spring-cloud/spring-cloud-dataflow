/*
 * Copyright 2020-2023 the original author or authors.
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
package org.springframework.cloud.dataflow.server.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TaskServiceDependencies.class }, properties = {
		"spring.main.allow-bean-definition-overriding=true" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)

/**
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
public class JdbcDataflowTaskExecutionDaoTests {

	@Autowired
	private DataflowTaskExecutionDao dataflowTaskExecutionDao;

	@Autowired
	private TaskRepository taskRepository;
	@Autowired
	private AggregateExecutionSupport aggregateExecutionSupport;
	@Autowired
	private TaskDefinitionReader taskDefinitionReader;

	@Test
	@DirtiesContext
	public void testGetTaskExecutionIdsByTaskName() {
		String taskName = UUID.randomUUID().toString();
		List<AggregateTaskExecution> taskExecutions = createSampleTaskExecutions(taskName, 4);
		for (AggregateTaskExecution taskExecution : taskExecutions) {
			taskRepository.createTaskExecution(taskExecution.toTaskExecution());
		}
		assertThat(dataflowTaskExecutionDao).isNotNull();
		Set<Long> taskExecutionIds = dataflowTaskExecutionDao.getTaskExecutionIdsByTaskName(taskName);
		assertThat(taskExecutionIds.size()).isEqualTo(4);
	}

	@Test
	@DirtiesContext
	public void testGetAllTaskExecutionIds() {
		String taskName1 = UUID.randomUUID().toString();
		List<AggregateTaskExecution> taskExecutions = createSampleTaskExecutions(taskName1, 4);
		String taskName2 = UUID.randomUUID().toString();
		taskExecutions.addAll(createSampleTaskExecutions(taskName2, 2));
		for (AggregateTaskExecution aggregateTaskExecution : taskExecutions) {
			taskRepository.createTaskExecution(aggregateTaskExecution.toTaskExecution());
		}
		assertThat(dataflowTaskExecutionDao).isNotNull();
		assertThat(dataflowTaskExecutionDao.getAllTaskExecutionsCount(true, null)).isEqualTo(0);
		assertThat(dataflowTaskExecutionDao.getAllTaskExecutionIds(true, null).size()).isEqualTo(0);
		assertThat(dataflowTaskExecutionDao.getAllTaskExecutionsCount(false, null)).isEqualTo(6);
		assertThat(dataflowTaskExecutionDao.getAllTaskExecutionIds(false, null).size()).isEqualTo(6);
		assertThat(dataflowTaskExecutionDao.getAllTaskExecutionsCount(false, taskName1)).isEqualTo(4);
		assertThat(dataflowTaskExecutionDao.getAllTaskExecutionsCount(false, taskName2)).isEqualTo(2);
	}

	private List<AggregateTaskExecution> createSampleTaskExecutions(String taskName, int numExecutions) {
		SchemaVersionTarget schemaVersionTarget = aggregateExecutionSupport.findSchemaVersionTarget(taskName, taskDefinitionReader);
		Date startTime = new Date();
		String externalExecutionId = UUID.randomUUID().toString();
		Random randomGenerator = new Random();
		List<AggregateTaskExecution> taskExecutions = new ArrayList<>();
		for (int i = 0; i < numExecutions; i++) {
			long executionId = randomGenerator.nextLong();
			taskExecutions.add(new AggregateTaskExecution(executionId, null, taskName, startTime,
					null, null, new ArrayList<>(), null, externalExecutionId, schemaVersionTarget.getName(), null));
		}
		return taskExecutions;
	}

}
