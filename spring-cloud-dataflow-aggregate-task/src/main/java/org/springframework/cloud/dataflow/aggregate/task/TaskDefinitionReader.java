package org.springframework.cloud.dataflow.aggregate.task;

import org.springframework.cloud.dataflow.core.TaskDefinition;

public interface TaskDefinitionReader {
	TaskDefinition findTaskDefinition(String taskName);
}
