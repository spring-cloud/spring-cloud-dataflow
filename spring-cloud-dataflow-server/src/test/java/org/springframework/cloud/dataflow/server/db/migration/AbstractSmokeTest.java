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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.aggregate.task.AggregateTaskExplorer;
import org.springframework.cloud.dataflow.aggregate.task.TaskRepositoryContainer;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.single.DataFlowServerApplication;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provides for testing some basic database schema and JPA tests to catch potential issues with specific databases early.
 *
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = {DataFlowServerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
		"spring.jpa.hibernate.ddl-auto=validate"
})
public abstract class AbstractSmokeTest {
	private final static Logger logger = LoggerFactory.getLogger(AbstractSmokeTest.class);
	protected static JdbcDatabaseContainer<?> container;
	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", container::getJdbcUrl);
		registry.add("spring.datasource.username", container::getUsername);
		registry.add("spring.datasource.password", container::getPassword);
		registry.add("spring.datasource.driver-class-name", container::getDriverClassName);
	}
	@Autowired
	SchemaService schemaService;

	@Autowired
	TaskRepositoryContainer taskRepositoryContainer;

	@Autowired
	protected AggregateTaskExplorer taskExplorer;

	@Test
	public void testTaskCreation() {
		logger.info("started:{}", getClass().getSimpleName());
		for (SchemaVersionTarget schemaVersionTarget : schemaService.getTargets().getSchemas()) {
			TaskRepository taskRepository = this.taskRepositoryContainer.get(schemaVersionTarget.getName());
			taskRepository.createTaskExecution(schemaVersionTarget.getName() + "_test_task");
		}
		assertThat(taskExplorer.getTaskExecutionCount()).isEqualTo(2);
		Page<AggregateTaskExecution> page = taskExplorer.findAll(Pageable.ofSize(100));
		List<AggregateTaskExecution> content = page.getContent();
		assertThat(content.size()).isEqualTo(2);
		content.forEach(taskExecution -> {
			assertThat(taskExecution.getExecutionId()).isNotEqualTo(0L);
		});
		logger.info("completed:{}", getClass().getSimpleName());
	}
}
