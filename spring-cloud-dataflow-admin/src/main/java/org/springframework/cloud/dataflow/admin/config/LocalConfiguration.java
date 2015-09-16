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

package org.springframework.cloud.dataflow.admin.config;

import static org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase.*;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.cloud.dataflow.module.deployer.local.LocalModuleDeployer;
import org.springframework.cloud.stream.module.launcher.ModuleLauncher;
import org.springframework.cloud.stream.module.launcher.ModuleLauncherConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * Configuration used when no other special case activates. Creates deployers that
 * will deploy modules in-process.
 *
 * @author Eric Bottard
 */
@Configuration
@Conditional(LocalConfiguration.LocalCondition.class)
@Import(ModuleLauncherConfiguration.class)
public class LocalConfiguration {

	@Bean
	public ModuleDeployer processModuleDeployer(ModuleLauncher moduleLauncher) {
		return new LocalModuleDeployer(moduleLauncher);
	}

	@Bean
	public ModuleDeployer taskModuleDeployer(ModuleLauncher moduleLauncher) {
		return new LocalModuleDeployer(moduleLauncher);
	}

	/**
	 * Condition which is used to enable local deployer in the default case, that is when
	 * neither running <i>in</i> a cloud, <i>targeting</i> a cloud or running on yarn.
	 *
	 * @author Eric Bottard
	 */
	public static class LocalCondition extends NoneNestedConditions {

		public LocalCondition() {
			super(PARSE_CONFIGURATION);
		}

		@Profile("yarn")
		static class OnYarn {
		}

		@Profile("cloud")
		static class OnCloud {
		}

		@ConditionalOnProperty("cloudfoundry.apiEndpoint")
		static class TargetingCloudFoundry {
		}

		@ConditionalOnProperty("marathon.apiEndpoint")
		static class TargetingMarathon {

		}
	}

}
