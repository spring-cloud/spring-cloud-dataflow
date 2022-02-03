/*
 * Copyright 2022-2022 the original author or authors.
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

import java.sql.SQLException;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Autostart an embedded H2 database server.
 *
 * @author Michael Wirth
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Server.class)
@ConditionalOnProperty(value = "spring.dataflow.embedded.database.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnExpression("'${spring.datasource.url:#{null}}'.startsWith('jdbc:h2:tcp://localhost:')")
public class H2ServerConfiguration {

	@Bean
	public H2ServerBeanFactoryPostProcessor h2ServerBeanFactoryPostProcessor(
			@Value("${spring.datasource.url}") String dataSourceUrl) {
		return new H2ServerBeanFactoryPostProcessor(dataSourceUrl);
	}

	static class H2ServerBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

		private static final Logger logger = LoggerFactory.getLogger(H2ServerBeanFactoryPostProcessor.class);

		final private String dataSourceUrl;

		private Server server;

		H2ServerBeanFactoryPostProcessor(String dataSourceUrl) {
			this.dataSourceUrl = dataSourceUrl;
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			if (beanFactory.containsBean("initH2TCPServer")) {
				logger.warn("H2 Server is already registered.");
			} else {
				beanFactory.registerSingleton("initH2TCPServer", initH2TCPServer(dataSourceUrl));
			}
		}

		public void close() {
			if (this.server != null) {
				this.server.stop();
				logger.info("Embedded H2 server stopped!");
			}
		}

		private Server initH2TCPServer(String dataSourceUrl) {
			logger.info("Starting H2 Server with URL: " + dataSourceUrl);
			try {
				this.server = Server.createTcpServer("-ifNotExists", "-tcp",
						"-tcpAllowOthers", "-tcpPort", getH2Port(dataSourceUrl)).start();
			}
			catch (SQLException e) {
				throw new IllegalStateException(e);
			}
			return server;
		}

		private String getH2Port(String url) {
			String[] tokens = StringUtils.tokenizeToStringArray(url, ":");
			Assert.isTrue(tokens.length >= 5, "URL not properly formatted");
			return tokens[4].substring(0, tokens[4].indexOf("/"));
		}
	}
}
