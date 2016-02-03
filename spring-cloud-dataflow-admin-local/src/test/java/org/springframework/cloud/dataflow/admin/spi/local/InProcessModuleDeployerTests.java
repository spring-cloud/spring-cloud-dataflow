/*
 * Copyright 2015-2016 the original author or authors.
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

import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.cloud.dataflow.module.deployer.test.AbstractModuleDeployerTests;
import org.springframework.cloud.stream.module.launcher.ModuleLauncher;
import org.springframework.cloud.stream.module.launcher.ModuleLauncherConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Integration tests for the {@link InProcessModuleDeployer}.
 *
 * @author Eric Bottard
 * @author Josh Long
 */
@SpringApplicationConfiguration(classes = InProcessModuleDeployerTests.Config.class)
public class InProcessModuleDeployerTests extends AbstractModuleDeployerTests {

	@Test
	@Ignore("Can't afford to kill this JVM")
	@Override
	public void testFailedDeployment() {
		super.testFailedDeployment();
	}

	@Test
	@Ignore("Can't really force-kill a module")
	@Override
	public void testDeployingStateCalculationAndCancel() {
		super.testDeployingStateCalculationAndCancel();
	}

	@Configuration
	@Import(ModuleLauncherConfiguration.class)
	public static class Config {

		@Bean
		ModuleDeployer moduleDeployer(ModuleLauncher moduleLauncher) {
			return new InProcessModuleDeployer(moduleLauncher);
		}

	}
}
