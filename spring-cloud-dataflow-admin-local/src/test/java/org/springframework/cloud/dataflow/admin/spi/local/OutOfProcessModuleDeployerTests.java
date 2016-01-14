/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.admin.spi.local;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.cloud.dataflow.module.deployer.test.AbstractModuleDeployerTests;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

/**
 * Integration Tests for {@link OutOfProcessModuleDeployer}.
 *
 * @author Eric Bottard
 */
@SpringApplicationConfiguration(classes = OutOfProcessModuleDeployerTests.Config.class)
public class OutOfProcessModuleDeployerTests extends AbstractModuleDeployerTests {

	@Test
	@Override
	@Ignore("https://github.com/spring-cloud/spring-cloud-dataflow/pull/302")
	public void testRedeploy() {
		super.testRedeploy();
	}

	@Configuration
	@EnableConfigurationProperties(OutOfProcessModuleDeployerProperties.class)
	public static class Config {

		@Bean
		public ModuleDeployer moduleDeployer() {
			return new OutOfProcessModuleDeployer();
		}
	}
}
