/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.repository.support;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * Utility for initializing the Definition Repository's datasource. If a single
 * {@link DataSource} is available in the current context, and functionality is enabled
 * (as it is by default), this will initialize the database.
 * <p>
 * By default, initialization of the database can be disabled by configuring the property
 * <code>spring.cloud.dataflow.rdbms.initialize.enable</code> to false.
 *
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */

public final class DataflowRdbmsInitializer implements InitializingBean {

	private static final Log logger = LogFactory.getLog(DataflowRdbmsInitializer.class);

	private static final String DEFAULT_SCHEMA_LOCATION = "classpath:schema-@@platform@@-@@suffix@@.sql";

	private static final String COMMON_SCHEMA_SUFFIX = "common";

	private static final String STREAMS_SCHEMA_SUFFIX = "streams";

	private static final String TASKS_SCHEMA_SUFFIX = "tasks";

	private static final String DEPLOYMENT_SCHEMA_SUFFIX = "deployment";

	/**
	 * Path to the SQL file to use to initialize the database schema.
	 */
	private static String schema = DEFAULT_SCHEMA_LOCATION;

	private final FeaturesProperties featuresProperties;

	private DataSource dataSource;

	private ResourceLoader resourceLoader;

	@Value("${" + DataFlowPropertyKeys.PREFIX + "rdbms.initialize.enable:true}")
	private boolean definitionInitializationEnable;

	public DataflowRdbmsInitializer(FeaturesProperties featuresProperties) {
		this.featuresProperties = featuresProperties;
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
			String commonSchemaLocation = schemaLocation;
			commonSchemaLocation = commonSchemaLocation.replace("@@suffix@@", COMMON_SCHEMA_SUFFIX);
			logger.info(String.format("Adding dataflow schema %s for %s database", commonSchemaLocation, platform));
			populator.addScript(resourceLoader.getResource(commonSchemaLocation));
			if (featuresProperties.isStreamsEnabled()) {
				String streamsSchemaLocation = schemaLocation;
				streamsSchemaLocation = streamsSchemaLocation.replace("@@suffix@@", STREAMS_SCHEMA_SUFFIX);
				logger.info(
						String.format("Adding dataflow schema %s for %s database", streamsSchemaLocation, platform));
				populator.addScript(resourceLoader.getResource(streamsSchemaLocation));
			}
			if (featuresProperties.isTasksEnabled()) {
				String tasksSchemaLocation = schemaLocation;
				tasksSchemaLocation = tasksSchemaLocation.replace("@@suffix@@", TASKS_SCHEMA_SUFFIX);
				logger.info(String.format("Adding dataflow schema %s for %s database", tasksSchemaLocation, platform));
				populator.addScript(resourceLoader.getResource(tasksSchemaLocation));
			}
			if (featuresProperties.isStreamsEnabled() || featuresProperties.isTasksEnabled()) {
				String deploymentSchemaLocation = schemaLocation;
				deploymentSchemaLocation = deploymentSchemaLocation.replace("@@suffix@@", DEPLOYMENT_SCHEMA_SUFFIX);
				logger.info(
						String.format("Adding dataflow schema %s for %s database", deploymentSchemaLocation, platform));
				populator.addScript(resourceLoader.getResource(deploymentSchemaLocation));
			}
			populator.setContinueOnError(true);
			logger.debug(String.format("Initializing dataflow schema for %s database", platform));
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
