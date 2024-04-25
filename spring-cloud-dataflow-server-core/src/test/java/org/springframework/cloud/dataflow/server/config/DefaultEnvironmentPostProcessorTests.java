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

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for {@link DefaultEnvironmentPostProcessor}.
 *
 * @author Josh Long
 * @auhor Chris Bono
 */
public class DefaultEnvironmentPostProcessorTests {

	private static final String MANAGEMENT_CONTEXT_PATH = "management.contextPath";

	private static final String CONTRIBUTED_PATH = "/bar";

	@Test
	public void testDefaultsBeingContributedByServerModule() throws Exception {
		try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultTestApplication.class, "--server.port=0",
				"--spring.main.allow-bean-definition-overriding=true",
				"--spring.autoconfigure.exclude=org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration,org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration")) {
			String cp = ctx.getEnvironment().getProperty(MANAGEMENT_CONTEXT_PATH);
			assertEquals(CONTRIBUTED_PATH, cp);
		}
	}

	@Test
	public void testOverridingDefaultsWithAConfigFile() {
		try (ConfigurableApplicationContext ctx = SpringApplication.run(EmptyDefaultTestApplication.class,
				"--spring.config.name=test", "--server.port=0",
				"--spring.main.allow-bean-definition-overriding=true",
				"--spring.cloud.dataflow.server.profileapplicationlistener.ignore=true",
				"--spring.autoconfigure.exclude=org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration,org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration")) {
			String cp = ctx.getEnvironment().getProperty(MANAGEMENT_CONTEXT_PATH);
			assertEquals(cp, "/foo");
			assertNotNull(ctx.getEnvironment().getProperty("spring.flyway.locations[0]"));
		}
	}
}
