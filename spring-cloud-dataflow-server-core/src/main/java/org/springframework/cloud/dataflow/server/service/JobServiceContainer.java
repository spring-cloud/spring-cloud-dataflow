package org.springframework.cloud.dataflow.server.service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.batch.JobService;
import org.springframework.cloud.dataflow.server.batch.SimpleJobServiceFactoryBean;
import org.springframework.cloud.dataflow.server.controller.NoSuchSchemaTargetException;
import org.springframework.cloud.dataflow.server.repository.JobRepositoryContainer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

public class JobServiceContainer {
	private final static Logger logger = LoggerFactory.getLogger(JobServiceContainer.class);
	private final Map<String, JobService> container = new HashMap<>();

	public JobServiceContainer(
			DataSource dataSource,
			PlatformTransactionManager platformTransactionManager,
			SchemaService schemaService,
			JobRepositoryContainer jobRepositoryContainer,
			JobExplorerContainer jobExplorerContainer
	) {

		for(SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			SimpleJobServiceFactoryBean factoryBean = new SimpleJobServiceFactoryBean();
			factoryBean.setDataSource(dataSource);
			factoryBean.setTransactionManager(platformTransactionManager);
			factoryBean.setJobServiceContainer(this);
			factoryBean.setJobLauncher(new SimpleJobLauncher());
			factoryBean.setJobExplorer(jobExplorerContainer.get(target.getName()));
			factoryBean.setJobRepository(jobRepositoryContainer.get(target.getName()));
			factoryBean.setTablePrefix(target.getBatchPrefix());
			factoryBean.setAppBootSchemaVersionTarget(target);
			factoryBean.setSchemaService(schemaService);
			try {
				factoryBean.afterPropertiesSet();
				container.put(target.getName(), factoryBean.getObject());
			} catch (Throwable x) {
				throw new RuntimeException("Exception creating JobService for "  + target.getName(), x);
			}
		}
	}
	public JobService get(String schemaTarget) {
		if(!StringUtils.hasText(schemaTarget)) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
			logger.info("get:default={}", schemaTarget);
		}
		if(!container.containsKey(schemaTarget)) {
			throw new NoSuchSchemaTargetException(schemaTarget);
		}
		return container.get(schemaTarget);
	}
}
