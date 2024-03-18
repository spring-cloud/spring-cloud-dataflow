package org.springframework.cloud.dataflow.composite.task;

import org.springframework.cloud.dataflow.core.TaskDefinition;

public interface TaskDefinitionReader {
	TaskDefinition findTaskDefinition(String taskName);
}
