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

package org.springframework.cloud.dataflow.server.repository;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.controller.NoSuchSchemaTargetException;
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
		if(!container.containsKey(schemaTarget)) {
			throw new NoSuchSchemaTargetException(schemaTarget);
		}
		return container.get(schemaTarget);
	}
}
