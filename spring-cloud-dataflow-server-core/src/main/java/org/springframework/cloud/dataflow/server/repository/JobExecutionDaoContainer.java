package org.springframework.cloud.dataflow.server.repository;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.batch.JdbcSearchableJobExecutionDao;
import org.springframework.cloud.dataflow.server.batch.SearchableJobExecutionDao;

public class JobExecutionDaoContainer {
	private final Map<String, SearchableJobExecutionDao> container = new HashMap<>();

	public JobExecutionDaoContainer(DataSource dataSource, SchemaService schemaService) {
		for (SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			JdbcSearchableJobExecutionDao jdbcSearchableJobExecutionDao = new JdbcSearchableJobExecutionDao();
			jdbcSearchableJobExecutionDao.setDataSource(dataSource);
			jdbcSearchableJobExecutionDao.setTablePrefix(target.getBatchPrefix());
			try {
				jdbcSearchableJobExecutionDao.afterPropertiesSet();
				container.put(target.getName(), jdbcSearchableJobExecutionDao);
			} catch (Throwable x) {
				throw new RuntimeException("Exception creating JdbcSearchableJobExecutionDao from:" + target.getName(), x);
			}
		}
	}

	public SearchableJobExecutionDao get(String schemaTarget) {
		if(schemaTarget == null) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		return container.get(schemaTarget);
	}
}
