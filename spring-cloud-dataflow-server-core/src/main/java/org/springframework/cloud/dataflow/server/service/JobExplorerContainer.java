package org.springframework.cloud.dataflow.server.service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.controller.NoSuchSchemaTargetException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

public class JobExplorerContainer {
	private final Map<String, JobExplorer> container = new HashMap<>();

	public JobExplorerContainer(DataSource dataSource, SchemaService schemaService, PlatformTransactionManager platformTransactionManager) {
		for (SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			JobExplorerFactoryBean factoryBean = new JobExplorerFactoryBean();
			factoryBean.setDataSource(dataSource);
			factoryBean.setTablePrefix(target.getBatchPrefix());
			factoryBean.setTransactionManager(platformTransactionManager);
			try {
				factoryBean.afterPropertiesSet();
				container.put(target.getName(), factoryBean.getObject());
			} catch (Throwable x) {
				throw new RuntimeException("Exception creating JobExplorer for " + target.getName(), x);
			}
		}
	}

	public JobExplorer get(String schemaTarget) {
		if(!StringUtils.hasText(schemaTarget)) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		if(!container.containsKey(schemaTarget)) {
			throw new NoSuchSchemaTargetException(schemaTarget);
		}
		return container.get(schemaTarget);
	}
}
