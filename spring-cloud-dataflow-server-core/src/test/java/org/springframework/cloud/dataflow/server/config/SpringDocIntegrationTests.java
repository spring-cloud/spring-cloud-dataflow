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

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that does a few coarse-grained sanity checks for the {@link SpringDocAutoConfiguration Springdoc
 * integration} with a running Spring Cloud Dataflow server.
 *
 * @author Chris Bono
 * @author Corneil du Plessis
 */
class SpringDocIntegrationTests {

	@Test
	void disabledByDefault() {
		try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultTestApplication.class,
				"--server.port=0",
				"--spring.main.allow-bean-definition-overriding=true",
				"--spring.autoconfigure.exclude=org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration" +
						",org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration")) {
			assertThat(ctx.containsBean("springDocWebSecurityCustomizer")).isFalse();
			assertThat(ctx.containsBean("springDocJsonDecodeFilterRegistration")).isFalse();
		}
	}

	@Test
	void disabledSpringDocAutoConfiguration() {
		try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultTestApplication.class,
				"--server.port=0",
				"--springdoc.api-docs.enabled=true",
				"--springdoc.swagger-ui.enabled=true",
				"--spring.main.allow-bean-definition-overriding=true",
				"--spring.autoconfigure.exclude=org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration" +
						",org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration" +
						",org.springframework.cloud.dataflow.server.config.SpringDocAutoConfiguration")) {
			assertThat(ctx.containsBean("springDocWebSecurityCustomizer")).isFalse();
			assertThat(ctx.containsBean("springDocJsonDecodeFilterRegistration")).isFalse();
		}
	}

	@Test
	void enabledWithDefaults() {
		try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultTestApplication.class,
				"--server.port=0",
				"--springdoc.api-docs.enabled=true",
				"--springdoc.swagger-ui.enabled=true",
				"--spring.main.allow-bean-definition-overriding=true",
				"--spring.autoconfigure.exclude=org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration" +
						",org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration")) {
			assertThat(ctx.containsBean("springDocWebSecurityCustomizer")).isTrue();
			assertThat(ctx.containsBean("springDocJsonDecodeFilterRegistration")).isTrue();
		}
	}
}
