package org.springframework.cloud.dataflow.server.db.migration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.aggregate.task.AggregateTaskExplorer;
import org.springframework.cloud.dataflow.aggregate.task.TaskRepositoryContainer;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.single.DataFlowServerApplication;
import org.springframework.cloud.task.repository.TaskRepository;

import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(classes = {DataFlowServerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractSmokeTest {
	@Autowired
	SchemaService schemaService;
	@Autowired
	TaskRepositoryContainer taskRepositoryContainer;
	@Autowired
	protected AggregateTaskExplorer taskExplorer;
	@Test
	public void testTaskCreation() {
		for(SchemaVersionTarget schemaVersionTarget : schemaService.getTargets().getSchemas()) {
			TaskRepository taskRepository = this.taskRepositoryContainer.get(schemaVersionTarget.getName());
			taskRepository.createTaskExecution(schemaVersionTarget.getName() + "_test_task");
		}
		assertThat(taskExplorer.getTaskExecutionCount()).isEqualTo(2);
	}
}
