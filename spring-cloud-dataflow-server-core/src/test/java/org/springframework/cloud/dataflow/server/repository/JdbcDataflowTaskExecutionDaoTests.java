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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.test.annotation.DirtiesContext;

/**
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = {TaskServiceDependencies.class}, properties = {
		"spring.main.allow-bean-definition-overriding=true"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class JdbcDataflowTaskExecutionDaoTests {

	@Autowired
	private DataflowTaskExecutionDao dataflowTaskExecutionDao;

	@Autowired
	private TaskRepository taskRepository;

	@Test
	@DirtiesContext
	void getTaskExecutionIdsByTaskName() {
		String taskName = UUID.randomUUID().toString();
		List<TaskExecution> taskExecutions = createSampleTaskExecutions(taskName, 4);
		taskExecutions.forEach(taskRepository::createTaskExecution);
		assertThat(dataflowTaskExecutionDao).isNotNull();
		assertThat(dataflowTaskExecutionDao.getTaskExecutionIdsByTaskName(taskName)).hasSize(4);
	}

	@Test
	@DirtiesContext
	void getAllTaskExecutionIds() {
		String taskName1 = UUID.randomUUID().toString();
		List<TaskExecution> taskExecutions = createSampleTaskExecutions(taskName1, 4);
		String taskName2 = UUID.randomUUID().toString();
		taskExecutions.addAll(createSampleTaskExecutions(taskName2, 2));
		taskExecutions.forEach(taskRepository::createTaskExecution);
		assertThat(dataflowTaskExecutionDao).isNotNull();
		assertThat(dataflowTaskExecutionDao.getAllTaskExecutionsCount(true, null)).isEqualTo(0);
		assertThat(dataflowTaskExecutionDao.getAllTaskExecutionIds(true, null)).isEmpty();
		assertThat(dataflowTaskExecutionDao.getAllTaskExecutionsCount(false, null)).isEqualTo(6);
		assertThat(dataflowTaskExecutionDao.getAllTaskExecutionIds(false, null)).hasSize(6);
		assertThat(dataflowTaskExecutionDao.getAllTaskExecutionsCount(false, taskName1)).isEqualTo(4);
		assertThat(dataflowTaskExecutionDao.getAllTaskExecutionsCount(false, taskName2)).isEqualTo(2);
	}

	private List<TaskExecution> createSampleTaskExecutions(String taskName, int numExecutions) {
		LocalDateTime startTime = LocalDateTime.now();
		String externalExecutionId = UUID.randomUUID().toString();
		Random randomGenerator = new Random();
		List<TaskExecution> taskExecutions = new ArrayList<>();
		for (int i = 0; i < numExecutions; i++) {
			long executionId = randomGenerator.nextLong();
			taskExecutions.add(new TaskExecution(executionId, null, taskName, startTime,
					null, null, new ArrayList<>(), null, externalExecutionId, null));
		}
		return taskExecutions;
	}

}
