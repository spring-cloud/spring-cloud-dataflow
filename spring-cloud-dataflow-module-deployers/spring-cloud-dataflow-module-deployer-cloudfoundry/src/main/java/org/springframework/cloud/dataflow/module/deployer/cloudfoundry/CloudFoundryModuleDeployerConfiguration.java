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

package org.springframework.cloud.dataflow.module.deployer.cloudfoundry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires up all the necessary components for providing {@link ModuleDeployer}s targeting
 * Cloud Foundry.
 *
 * @author Eric Bottard
 */
@Configuration
@EnableConfigurationProperties(CloudFoundryModuleDeployerProperties.class)
public class CloudFoundryModuleDeployerConfiguration {

	@Autowired
	private CloudFoundryModuleDeployerProperties properties;

	@Bean
	public ModuleDeployer processModuleDeployer() {
		return new ApplicationModuleDeployer(properties);
	}

	@Bean
	public ModuleDeployer taskModuleDeployer() {
		return processModuleDeployer();
	}

}
