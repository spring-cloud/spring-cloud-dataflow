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
package org.springframework.cloud.dataflow.server.config.web;

import java.sql.SQLException;

import javax.servlet.ServletContext;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Autostarts an embedded H2 database server.
 *
 * @author Michael Wirth
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Server.class)
@ConditionalOnWebApplication
public class H2ServerConfiguration
		implements ServletContextInitializer, ApplicationListener<ContextClosedEvent> {

	private static final Logger logger = LoggerFactory
			.getLogger(H2ServerConfiguration.class);

	@Value("${spring.datasource.url:#{null}}")
	private String dataSourceUrl;

	private Server server;

	private Server initH2TCPServer() {
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

	@Override
	public void onStartup(ServletContext servletContext) {
		if (StringUtils.hasText(dataSourceUrl)
				&& dataSourceUrl.startsWith("jdbc:h2:tcp://localhost:")) {
			logger.info("Start Embedded H2");
			initH2TCPServer();
		}
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		if (this.server != null) {
			this.server.stop();
			logger.info("Embedded H2 server stopped!");
		}
	}
}
