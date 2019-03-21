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

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.MetricExportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2AutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.dataflow.autoconfigure.local.LocalDataFlowServerAutoConfiguration;
import org.springframework.cloud.deployer.spi.local.LocalDeployerAutoConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.util.SocketUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Gunnar Hillert
 *
 */
@RestController
@SpringBootApplication(excludeName = {
		"org.springframework.cloud.dataflow.shell.autoconfigure.BaseShellAutoConfiguration" }, exclude = {
				DataSourceAutoConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				IntegrationAutoConfiguration.class,
				JmxAutoConfiguration.class,
				LdapAutoConfiguration.class,
				LocalDataFlowServerAutoConfiguration.class,
				LocalDeployerAutoConfiguration.class,
				MetricExportAutoConfiguration.class,
				OAuth2AutoConfiguration.class,
				RedisAutoConfiguration.class,
				RedisRepositoriesAutoConfiguration.class,
				SecurityAutoConfiguration.class
		})
public class FakeMetricsCollector {

	private static final Log LOGGER = LogFactory.getLog(FakeMetricsCollector.class);

	@Value(value = "classpath:/org/springframework/cloud/dataflow/server/local/metrics/metrics-collector-sample-response.json")
	private Resource metricsCollectorResponseResource;

	public static void main(String[] args) {
		int port = SocketUtils.findAvailableTcpPort();
		LOGGER.info("Setting Fake Metrics Collector port to " + port);
		new SpringApplicationBuilder(FakeMetricsCollector.class)
				.properties("fakeMetricsCollector.port:" + port)
				.properties("logging.level.org.springframework.boot=debug")
				.build()
				.run("--spring.config.location=classpath:/org/springframework/cloud/dataflow/server/local/metrics/fakeMetricsCollectorConfig.yml");
	}

	@RequestMapping("/collector/metrics/streams")
	public String streamMetrics() throws IOException {
		return org.springframework.util.StreamUtils.copyToString(metricsCollectorResponseResource.getInputStream(),
				Charset.defaultCharset());
	}

}
