package org.springframework.cloud.dataflow.server.repository;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.transaction.PlatformTransactionManager;

public class JobRepositoryContainer {
	private final Map<String, JobRepository> container = new HashMap<>();

	public JobRepositoryContainer(DataSource dataSource, PlatformTransactionManager transactionManager, SchemaService schemaService) {
		for (SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();
			factoryBean.setDataSource(dataSource);
			factoryBean.setTablePrefix(target.getBatchPrefix());
			factoryBean.setTransactionManager(transactionManager);

			try {
				factoryBean.afterPropertiesSet();
				container.put(target.getName(), factoryBean.getObject());
			} catch (Throwable x) {
				throw new RuntimeException("Exception creating JobRepository for:" + target.getName(), x);
			}
		}
	}

	public JobRepository get(String schemaTarget) {
		if(schemaTarget == null) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		return container.get(schemaTarget);
	}
}
