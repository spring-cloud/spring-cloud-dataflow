/*
 * Copyright 2015-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultEnvironmentPostProcessor}.
 *
 * @author Josh Long
 * @auhor Chris Bono
 * @author Corneil du Plessis
 */
class DefaultEnvironmentPostProcessorTests {

	private static final String MANAGEMENT_CONTEXT_PATH = "management.context-path";

	@Test
	void defaultsAreContributedByServerModule() {
		try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultTestApplication.class, "--server.port=0",
				"--spring.main.allow-bean-definition-overriding=true",
				"--spring.autoconfigure.exclude=org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration,org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration")) {

			// this one comes from <test>/resources/dataflow-server.yml
			assertThat(ctx.getEnvironment().getProperty(MANAGEMENT_CONTEXT_PATH)).isEqualTo("/foo");

			// this one comes from dataflow-server-defaults.yml (we use 'spring.flyway.enabled' as the indicator)
			assertThat(ctx.getEnvironment().getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
		}
	}

	@Test
	void defaultsCanBeOverridden() {
		try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultTestApplication.class,
				//"--spring.profiles.active=test",
				"--server.port=0",
				"--spring.config.name=test",
				"--spring.flyway.enabled=false",
				"--spring.main.allow-bean-definition-overriding=true",
				"--spring.cloud.dataflow.server.profileapplicationlistener.ignore=true",
				"--spring.autoconfigure.exclude=org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration,org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration")) {

			// this one comes from <test>/resources/test.yml and overrides the entry from <test>/resources/dataflow-server.yml
			assertThat(ctx.getEnvironment().getProperty(MANAGEMENT_CONTEXT_PATH)).isEqualTo("/bar");

			// sys props overrides this one from dataflow-server-defaults.yml
			assertThat(ctx.getEnvironment().getProperty("spring.flyway.enabled", Boolean.class)).isFalse();

		}
	}
}
