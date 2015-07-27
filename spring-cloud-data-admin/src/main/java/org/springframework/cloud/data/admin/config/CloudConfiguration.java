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

package org.springframework.cloud.data.admin.config;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.redis.RedisAutoConfiguration;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.data.module.deployer.ModuleDeployer;
import org.springframework.cloud.data.module.deployer.cloudfoundry.CloudFoundryModuleDeployerConfiguration;
import org.springframework.cloud.data.module.deployer.lattice.LrpModuleDeployer;
import org.springframework.cloud.data.module.deployer.lattice.TaskModuleDeployer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Configuration for cloud profiles (cloud, lattice).
 *
 * @author Mark Fisher
 * @author Thomas Risberg
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@Profile("cloud")
public class CloudConfiguration {

	@AutoConfigureBefore(RedisAutoConfiguration.class)
	protected static class RedisConfig {

		@Bean
		public Cloud cloud() {
			return new CloudFactory().getCloud();
		}

		@Bean
		RedisConnectionFactory redisConnectionFactory(Cloud cloud) {
			return cloud.getSingletonServiceConnector(RedisConnectionFactory.class, null);
		}
	}

	@Profile("lattice")
	protected static class LatticeConfig {

		public ModuleDeployer processModuleDeployer() {
			return new LrpModuleDeployer();
		}

		@Bean
		public ModuleDeployer taskModuleDeployer() {
			return new TaskModuleDeployer();
		}
	}

	@Profile("!lattice")
	@Import(CloudFoundryModuleDeployerConfiguration.class)
	protected static class CloudFoundryConfig {

	}

}
