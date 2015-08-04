/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.data.rest.config;

import org.springframework.cloud.data.module.deployer.ModuleDeployer;
import org.springframework.cloud.data.module.deployer.lattice.ReceptorModuleDeployer;
import org.springframework.cloud.data.module.deployer.local.LocalModuleDeployer;
import org.springframework.cloud.data.rest.controller.StreamController;
import org.springframework.cloud.stream.module.launcher.ModuleLauncher;
import org.springframework.cloud.stream.module.launcher.ModuleLauncherConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
@Configuration
public class AdminConfiguration {

	@Bean
	public StreamController streamController(ModuleDeployer moduleDeployer) {
		return new StreamController(moduleDeployer);
	}

	@Configuration
	@Profile("!cloud")
	@Import(ModuleLauncherConfiguration.class)
	protected static class LocalConfig {

		@Bean
		public ModuleDeployer moduleDeployer(ModuleLauncher moduleLauncher) {
			return new LocalModuleDeployer(moduleLauncher);
		}
	}

	@Configuration
	@Profile("cloud")
	protected static class CloudConfig {

		@Bean
		public ModuleDeployer moduleDeployer() {
			return new ReceptorModuleDeployer();
		}
	}

}
