package org.springframework.cloud.dataflow.aggregate.task;

import org.springframework.cloud.task.repository.TaskRepository;

public interface TaskRepositoryContainer {
	TaskRepository get(String schemaTarget);
}
