package org.springframework.cloud.dataflow.server.repository;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.batch.listener.support.JdbcTaskBatchDao;

public class TaskBatchDaoContainer {
	private final Map<String, TaskBatchDao> taskBatchDaoContainer = new HashMap<>();

	public TaskBatchDaoContainer(DataSource dataSource, SchemaService schemaService) {
		for(SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			taskBatchDaoContainer.put(target.getName(), new JdbcTaskBatchDao(dataSource, target.getTaskPrefix()));
		}
	}
	public TaskBatchDao get(String schemaTarget) {
		if(schemaTarget == null) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		return taskBatchDaoContainer.get(schemaTarget);
	}
}
