package org.springframework.cloud.dataflow.server.repository;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;

public class TaskExecutionDaoContainer {
	private final Map<String, TaskExecutionDao> taskExecutionDaoContainer = new HashMap<>();

	public TaskExecutionDaoContainer(DataSource dataSource, SchemaService schemaService) {
		for(SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			TaskExecutionDaoFactoryBean factoryBean = new TaskExecutionDaoFactoryBean(dataSource, target.getTaskPrefix());
			try {
				this.taskExecutionDaoContainer.put(target.getName(), factoryBean.getObject());
			} catch (Throwable x) {
				throw  new RuntimeException("Exception creating TaskExecutionDao for " + target.getName(), x);
			}
		}
	}

	public TaskExecutionDao get(String schemaTarget) {
		if(schemaTarget == null) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		return taskExecutionDaoContainer.get(schemaTarget);
	}
}
