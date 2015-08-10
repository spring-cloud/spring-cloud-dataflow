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
import org.springframework.cloud.data.module.deployer.cloudfoundry.CloudFoundryModuleDeployer;
import org.springframework.cloud.data.module.deployer.lattice.ReceptorModuleDeployer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for cloud profiles (cloud, lattice)
 *
 * @author Mark Fisher
 * @author Thomas Risberg
 */
@Profile("cloud")
public class CloudConfiguration {

	@Profile("lattice")
	protected static class LatticeConfig {

		@Bean
		public ModuleDeployer moduleDeployer() {
			return new ReceptorModuleDeployer();
		}
	}

	@Profile("!lattice")
	protected static class CloudFoundryConfig {

		@Bean
		public ModuleDeployer moduleDeployer() {
			return new CloudFoundryModuleDeployer();
		}
	}
}
