/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config;

import static org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType.HAL;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.h2.tools.Server;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.completion.CompletionConfiguration;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.config.features.FeaturesConfiguration;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.config.security.BasicAuthSecurityConfiguration;
import org.springframework.cloud.dataflow.server.config.security.FileAuthenticationConfiguration;
import org.springframework.cloud.dataflow.server.config.security.LdapAuthenticationConfiguration;
import org.springframework.cloud.dataflow.server.config.security.OAuthSecurityConfiguration;
import org.springframework.cloud.dataflow.server.config.web.WebConfiguration;
import org.springframework.cloud.dataflow.server.repository.support.DataflowRdbmsInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
@Import({CompletionConfiguration.class, FeaturesConfiguration.class, WebConfiguration.class,
		BasicAuthSecurityConfiguration.class, FileAuthenticationConfiguration.class,
		LdapAuthenticationConfiguration.class, OAuthSecurityConfiguration.class})
@EnableConfigurationProperties({BatchProperties.class, CommonApplicationProperties.class})
public class DataFlowServerConfiguration {

	@Configuration
	@ConditionalOnProperty(name = "spring.dataflow.embedded.database.enabled", havingValue = "true", matchIfMissing = true)
	@ConditionalOnExpression("#{'${spring.datasource.url:}'.startsWith('jdbc:h2:tcp://localhost:')}")
	public static class H2ServerConfiguration {

		private static final org.slf4j.Logger logger = LoggerFactory.getLogger(H2ServerConfiguration.class);

		@Value("${spring.datasource.url:#{null}}")
		private String dataSourceUrl;

		@Bean(destroyMethod = "stop")
		public Server initH2TCPServer() {
			Server server = null;
			logger.info("Starting H2 Server with URL: " + dataSourceUrl);
			try {
				server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort",
						getH2Port(dataSourceUrl)).start();
			} catch (SQLException e) {
				throw new IllegalStateException(e);
			}
			return server;
		}

		private String getH2Port(String url) {
			String[] tokens = StringUtils.tokenizeToStringArray(url, ":");
			Assert.isTrue(tokens.length >= 5, "URL not properly formatted");
			return tokens[4].substring(0, tokens[4].indexOf("/"));
		}

		@Bean
		@DependsOn("initH2TCPServer")
		public DataSourceTransactionManager transactionManagerForServer(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		@DependsOn("initH2TCPServer")
		public DataflowRdbmsInitializer dataflowRdbmsInitializer(DataSource dataSource, FeaturesProperties featuresProperties) {
			DataflowRdbmsInitializer dataflowRdbmsInitializer = new DataflowRdbmsInitializer(featuresProperties);
			dataflowRdbmsInitializer.setDataSource(dataSource);
			return dataflowRdbmsInitializer;
		}
	}

	@Configuration
	@ConditionalOnExpression("#{!'${spring.datasource.url:}'.startsWith('jdbc:h2:tcp://localhost:') || " +
			"('${spring.datasource.url:}'.startsWith('jdbc:h2:tcp://localhost:') &&" +
			"'${spring.dataflow.embedded.database.enabled}'.equals('false'))}")
	public static class NoH2ServerConfiguration {

		@Bean
		public DataSourceTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		public DataflowRdbmsInitializer dataflowRdbmsInitializer(DataSource dataSource, FeaturesProperties featuresProperties) {
			DataflowRdbmsInitializer dataflowRdbmsInitializer = new DataflowRdbmsInitializer(featuresProperties);
			dataflowRdbmsInitializer.setDataSource(dataSource);
			return dataflowRdbmsInitializer;
		}
	}
}
