package org.springframework.cloud.dataflow.server.repository;

import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.core.TaskDefinition;

public class DefaultTaskDefinitionReader implements TaskDefinitionReader {
	private final TaskDefinitionRepository taskDefinitionRepository;

	public DefaultTaskDefinitionReader(TaskDefinitionRepository taskDefinitionRepository) {
		this.taskDefinitionRepository = taskDefinitionRepository;
	}

	@Override
	public TaskDefinition findTaskDefinition(String taskName) {
		return taskDefinitionRepository.findByTaskName(taskName);
	}
}
