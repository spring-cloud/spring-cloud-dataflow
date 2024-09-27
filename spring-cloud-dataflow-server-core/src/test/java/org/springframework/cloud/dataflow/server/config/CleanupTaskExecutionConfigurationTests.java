/*
 * Copyright 2024 the original author or authors.
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

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.cloud.dataflow.server.config.CleanupTaskExecutionProperties.CLEANUP_TASK_EXECUTION_PROPS_PREFIX;

/**
 * @author Ganghun Cho
 */
public class CleanupTaskExecutionConfigurationTests {

	@Test
	public void enableCleanTaskExecutionScheduleTest() {
		ConfigurableApplicationContext context = applicationContext(
			String.format("--%s.enabled=true", CLEANUP_TASK_EXECUTION_PROPS_PREFIX));
		assertTrue(
			context.containsBean("org.springframework.cloud.dataflow.server.config.CleanupTaskExecutionConfiguration"));
	}

	@Test
	public void disableCleanTaskExecutionScheduleTest() {
		ConfigurableApplicationContext context = applicationContext(
			String.format("--%s.enabled=false", CLEANUP_TASK_EXECUTION_PROPS_PREFIX));
		assertFalse(
			context.containsBean("org.springframework.cloud.dataflow.server.config.CleanupTaskExecutionConfiguration"));
	}

	private ConfigurableApplicationContext applicationContext(String... args) {
		String[] commonArgs = {
			"--server.port=0",
			"--spring.main.allow-bean-definition-overriding=true",
			"--spring.autoconfigure.exclude=" +
				"org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration," +
				"org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration"
		};
		String[] allArgs = Stream.of(commonArgs, args)
			.flatMap(Stream::of)
			.toArray(String[]::new);
		return SpringApplication.run(EmptyDefaultTestApplication.class, allArgs);
	}
}
