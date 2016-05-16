/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.server.repository.support;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * Utility for initializing the Definition Repository's datasource.  If a single
 * {@link DataSource} is available in the current context, and functionality is enabled
 * (as it is by default), this will initialize the database.
 *
 * By default, initialization of the database can be disabled by configuring the property
 * <code>spring.cloud.dataflow.server.initialize.enable</code> to false.
 *
 * @author Glenn Renfro
 */

public final class DefinitionRepositoryInitializer implements InitializingBean {

	private static final Log logger = LogFactory.getLog(DefinitionRepositoryInitializer.class);

	private static final String DEFAULT_SCHEMA_LOCATION = "classpath:schema-@@platform@@.sql";

	/**
	 * Path to the SQL file to use to initialize the database schema.
	 */
	private static String schema = DEFAULT_SCHEMA_LOCATION;

	private DataSource dataSource;

	private ResourceLoader resourceLoader;

	@Value("${spring.cloud.dataflow.server.initialize.definition.repository.enable:true}")
	private boolean definitionInitializationEnable;

	public DefinitionRepositoryInitializer() {
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Autowired(required = false)
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (dataSource != null && definitionInitializationEnable) {
			String platform = getDatabaseType(dataSource);
			if ("hsql".equals(platform)) {
				platform = "hsqldb";
			}
			if ("postgres".equals(platform)) {
				platform = "postgresql";
			}
			if ("oracle".equals(platform)) {
				platform = "oracle10g";
			}
			ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
			String schemaLocation = schema;
			schemaLocation = schemaLocation.replace("@@platform@@", platform);
			populator.addScript(resourceLoader.getResource(schemaLocation));
			populator.setContinueOnError(true);
			logger.debug(String.format("Initializing definition schema for %s database",
					platform));
			DatabasePopulatorUtils.execute(populator, dataSource);
		}
	}

	private String getDatabaseType(DataSource dataSource) {
		try {
			return DatabaseType.fromMetaData(dataSource).toString().toLowerCase();
		}
		catch (MetaDataAccessException ex) {
			throw new IllegalStateException("Unable to detect database type", ex);
		}
	}
}
