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
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.aggregate.task.AggregateTaskExplorer;
import org.springframework.cloud.dataflow.aggregate.task.TaskRepositoryContainer;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.single.DataFlowServerApplication;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
		properties = "spring.jpa.hibernate.ddl-auto=none")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
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

	@Autowired
	protected StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	protected PlatformTransactionManager transactionManager;

	@Test
	public void testTaskCreation() {
		assertThat(taskExplorer.getTaskExecutionCount()).isEqualTo(0);
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		tx.execute(status -> {
			for (SchemaVersionTarget schemaVersionTarget : schemaService.getTargets().getSchemas()) {
				TaskRepository taskRepository = this.taskRepositoryContainer.get(schemaVersionTarget.getName());
				TaskExecution taskExecution = taskRepository.createTaskExecution(schemaVersionTarget.getName() + "_test_task");
				assertThat(taskExecution.getExecutionId()).isGreaterThan(0L);
			}
			return true;
		});
		assertThat(taskExplorer.getTaskExecutionCount()).isEqualTo(2);
		List<AggregateTaskExecution> taskExecutions = taskExplorer.findAll(Pageable.ofSize(100)).getContent();
		assertThat(taskExecutions)
				.hasSize(2)
				.allSatisfy((taskExecution) -> assertThat(taskExecution.getExecutionId()).isNotEqualTo(0L));
	}

	@Test
	public void streamCreation() {
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
}
