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

package org.springframework.cloud.dataflow.deployer.local;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.cloud.dataflow.app.launcher.ModuleLauncher;
import org.springframework.cloud.dataflow.app.launcher.ModuleLauncherConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Creates deployers that will deploy modules in-process.
 *
 * @author Eric Bottard
 * @author Josh Long
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@ConditionalOnClass(ModuleDeployer.class)
public class LocalDeployerAutoConfiguration {

	static final String LOCAL_DEPLOYER_PREFIX = "deployer.local";

	@Configuration
	@ConditionalOnProperty(prefix = LOCAL_DEPLOYER_PREFIX, name = "out-of-process", havingValue = "false")
	@ConditionalOnMissingBean(ModuleDeployer.class)
	@Import(ModuleLauncherConfiguration.class)
	public static class InProcess {

		@Bean
		public ModuleDeployer processModuleDeployer(ModuleLauncher moduleLauncher) {
			return new InProcessModuleDeployer(moduleLauncher);
		}

		@Bean
		public ModuleDeployer taskModuleDeployer(ModuleLauncher moduleLauncher) {
			return processModuleDeployer(moduleLauncher);
		}
	}

	@Configuration
	@EnableConfigurationProperties(OutOfProcessModuleDeployerProperties.class)
	@ConditionalOnMissingBean(ModuleDeployer.class)
	@ConditionalOnProperty(prefix = LOCAL_DEPLOYER_PREFIX, name = "out-of-process", havingValue = "true", matchIfMissing = true)
	public static class OutOfProcess {

		@Bean
		public ModuleDeployer processModuleDeployer() throws Exception {
			return new OutOfProcessModuleDeployer();
		}

		@Bean
		public ModuleDeployer taskModuleDeployer() throws Exception {
			return processModuleDeployer();
		}
	}

}
