/*
 * Copyright 2015-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.completion.CompletionConfiguration;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.config.features.FeaturesConfiguration;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.config.web.WebConfiguration;
import org.springframework.cloud.dataflow.server.repository.support.DataflowRdbmsInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;

/**
 * Configuration for the Data Flow Server application context. This includes support for
 * the REST API framework configuration.
 *
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Patrick Peralta
 * @author Thomas Risberg
 * @author Janne Valkealahti
 * @author Glenn Renfro
 * @author Josh Long
 * @author Michael Minella
 * @author Gunnar Hillert
 */
@EnableHypermediaSupport(type = HAL)
@EnableSpringDataWebSupport
@Configuration
@Import({ CompletionConfiguration.class, FeaturesConfiguration.class, WebConfiguration.class,
		SecurityConfiguration.class })
@EnableConfigurationProperties({ BatchProperties.class, CommonApplicationProperties.class })
public class DataFlowServerConfiguration {

	@Bean
	public DataSourceTransactionManager transactionManager(DataSource dataSource) {
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean
	public DataflowRdbmsInitializer dataflowRdbmsInitializer(DataSource dataSource,
			FeaturesProperties featuresProperties) {
		DataflowRdbmsInitializer dataflowRdbmsInitializer = new DataflowRdbmsInitializer(featuresProperties);
		dataflowRdbmsInitializer.setDataSource(dataSource);
		return dataflowRdbmsInitializer;
	}
}
