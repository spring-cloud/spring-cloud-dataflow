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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

/**
 * Autostart an embedded H2 database server.
 *
 * @author Michael Wirth
 * @author Corneil du Plessis
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Server.class)
@ConditionalOnProperty(name = "spring.dataflow.embedded.database.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnExpression("'${spring.datasource.url:#{null}}'.startsWith('jdbc:h2:tcp://localhost')")
public class H2ServerConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(H2ServerConfiguration.class);

	private static final Pattern JDBC_URL_PATTERN = Pattern.compile("^jdbc:h2:tcp://localhost:(?<port>\\d+)");

	@Bean
	public H2ServerBeanFactoryPostProcessor h2ServerBeanFactoryPostProcessor() {
		return new H2ServerBeanFactoryPostProcessor();
	}

	@Bean(destroyMethod = "stop")
	public Server h2TcpServer(@Value("${spring.datasource.url}") String dataSourceUrl) {
		logger.info("Starting H2 Server with URL: " + dataSourceUrl);

		Matcher matcher = JDBC_URL_PATTERN.matcher(dataSourceUrl);
		if (!matcher.find()) {
			throw new IllegalArgumentException(
					"DataSource URL '" + dataSourceUrl + "' does not match regex pattern: "
							+ JDBC_URL_PATTERN.pattern());
		}

		String port = matcher.group("port");
		try {
			return Server.createTcpServer("-ifNotExists", "-tcp",
					"-tcpAllowOthers", "-tcpPort", port).start();
		}
		catch (SQLException e) {
			throw new IllegalStateException(e);
		}

	}

	/**
	 * A {@link BeanFactoryPostProcessor} whose sole job is to ensure that the H2 server is up and running before any
	 * datasource initialization is attempted. It does this by requesting the H2Server bean which then in turn starts up
	 * the server.
	 */
	static class H2ServerBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			beanFactory.getBean("h2TcpServer");
		}
	}
}
