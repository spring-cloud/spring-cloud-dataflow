/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.server.service;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.batch.AllInOneExecutionContextSerializer;
import org.springframework.cloud.dataflow.server.batch.JobService;
import org.springframework.cloud.dataflow.server.batch.SimpleJobServiceFactoryBean;
import org.springframework.cloud.dataflow.server.controller.NoSuchSchemaTargetException;
import org.springframework.cloud.dataflow.server.repository.JobRepositoryContainer;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

/**
 * The container provides implementations of JobService for each SchemaTarget.
 *
 * @author Corneil du Plessis
 */
public class JobServiceContainer {
	private final static Logger logger = LoggerFactory.getLogger(JobServiceContainer.class);
	private final Map<String, JobService> container = new HashMap<>();

	public JobServiceContainer(
			DataSource dataSource,
			PlatformTransactionManager platformTransactionManager,
			SchemaService schemaService,
			JobRepositoryContainer jobRepositoryContainer,
			JobExplorerContainer jobExplorerContainer,
			Environment environment) {

		for(SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			SimpleJobServiceFactoryBean factoryBean = new SimpleJobServiceFactoryBean();
			factoryBean.setEnvironment(environment);
			factoryBean.setDataSource(dataSource);
			factoryBean.setTransactionManager(platformTransactionManager);
			factoryBean.setJobServiceContainer(this);
			factoryBean.setJobLauncher(new SimpleJobLauncher());
			factoryBean.setJobExplorer(jobExplorerContainer.get(target.getName()));
			factoryBean.setJobRepository(jobRepositoryContainer.get(target.getName()));
			factoryBean.setTablePrefix(target.getBatchPrefix());
			factoryBean.setTaskTablePrefix(target.getTaskPrefix());
			factoryBean.setAppBootSchemaVersionTarget(target);
			factoryBean.setSchemaService(schemaService);
			factoryBean.setSerializer(new AllInOneExecutionContextSerializer());
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
