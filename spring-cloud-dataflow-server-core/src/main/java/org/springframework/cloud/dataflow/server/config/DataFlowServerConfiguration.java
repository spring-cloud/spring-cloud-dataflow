/*
 * Copyright 2015-2019 the original author or authors.
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

import javax.servlet.Filter;
import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.completion.CompletionConfiguration;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.config.features.FeaturesConfiguration;
import org.springframework.cloud.dataflow.server.config.web.WebConfiguration;
import org.springframework.cloud.dataflow.server.db.migration.DataFlowFlywayConfigurationCustomizer;
import org.springframework.cloud.dataflow.server.repository.DataflowJobExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.JdbcDataflowJobExecutionDao;
import org.springframework.cloud.dataflow.server.repository.JdbcDataflowTaskExecutionDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.web.filter.ForwardedHeaderFilter;

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
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@EnableSpringDataWebSupport
@Configuration
@Import({ CompletionConfiguration.class, FeaturesConfiguration.class, WebConfiguration.class })
@EnableConfigurationProperties({ BatchProperties.class, CommonApplicationProperties.class })
public class DataFlowServerConfiguration {

	@Bean
	public DataFlowFlywayConfigurationCustomizer dataFlowFlywayConfigurationCustomizer() {
		return new DataFlowFlywayConfigurationCustomizer();
	}

	@Bean
	public Filter forwardedHeaderFilter() {
		return new ForwardedHeaderFilter();
	}

	@Bean
	DataflowTaskExecutionDao dataflowTaskExecutionDao(DataSource dataSource) {
		return new JdbcDataflowTaskExecutionDao(dataSource);
	}
	@Bean
	DataflowJobExecutionDao dataflowJobExecutionDao(DataSource dataSource) {
		return new JdbcDataflowJobExecutionDao(dataSource);
	}
}
