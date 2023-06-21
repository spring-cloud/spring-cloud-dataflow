package org.springframework.cloud.dataflow.aggregate.task;

import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.task.repository.TaskExecution;

public interface AggregateExecutionSupport {
	AggregateTaskExecution from(TaskExecution execution, TaskDefinitionReader taskDefinitionReader);

	SchemaVersionTarget findSchemaVersionTarget(String taskName, TaskDefinitionReader taskDefinitionReader);

	AppRegistration findTaskAppRegistration(String registeredName);

	AggregateTaskExecution from(TaskExecution execution, String schemaTarget);
}
