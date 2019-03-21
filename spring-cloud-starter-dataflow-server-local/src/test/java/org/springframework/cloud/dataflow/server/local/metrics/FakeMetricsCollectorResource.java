/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.dataflow.server.local.metrics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.ExternalResource;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.SocketUtils;

/**
 * Bootstraps a simple REST server, providing a fake Metrics Collector response.
 *
 * @author Gunnar Hillert
 */
public class FakeMetricsCollectorResource extends ExternalResource {

	private final Log LOGGER = LogFactory.getLog(FakeMetricsCollectorResource.class);

	private String originalServerPort;

	private int serverPort;

	private static final String FAKE_METRICS_COLLECTOR_PORT_PROPERTY = "fakeMetricsCollector.port";

	public FakeMetricsCollectorResource() {
		super();
	}

	private ConfigurableApplicationContext application;

	@Override
	protected void before() throws Throwable {

		originalServerPort = System.getProperty(FAKE_METRICS_COLLECTOR_PORT_PROPERTY);

		this.serverPort = SocketUtils.findAvailableTcpPort();

		LOGGER.info("Setting Fake Metrics Collector port to " + this.serverPort);

		System.setProperty(FAKE_METRICS_COLLECTOR_PORT_PROPERTY, String.valueOf(this.serverPort));

		this.application = new SpringApplicationBuilder(FakeMetricsCollector.class)
				.properties("logging.level.org.springframework.boot.autoconfigure.logging=debug")
				.build()
				.run("--spring.config.location=classpath:/org/springframework/cloud/dataflow/server/local/metrics/fakeMetricsCollectorConfig.yml");

	}

	@Override
	protected void after() {
		try {
			application.stop();
		}
		finally {
			if (this.originalServerPort != null) {
				System.setProperty(FAKE_METRICS_COLLECTOR_PORT_PROPERTY, originalServerPort);
			}
			else {
				System.clearProperty(FAKE_METRICS_COLLECTOR_PORT_PROPERTY);
			}
		}
	}

	public int getServerPort() {
		return serverPort;
	}

}
