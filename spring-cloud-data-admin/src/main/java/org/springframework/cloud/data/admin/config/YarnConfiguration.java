/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.admin.config;

import org.springframework.cloud.data.module.deployer.ModuleDeployer;
import org.springframework.cloud.data.module.deployer.yarn.DefaultYarnCloudAppService;
import org.springframework.cloud.data.module.deployer.yarn.YarnCloudAppService;
import org.springframework.cloud.data.module.deployer.yarn.YarnCloudAppStateMachine;
import org.springframework.cloud.data.module.deployer.yarn.YarnModuleDeployer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for creating deployer bean for yarn if yarn
 * profile is activated.
 *
 * @author Janne Valkealahti
 *
 */
@Configuration
@Profile("yarn")
public class YarnConfiguration {

	@Bean
	public ModuleDeployer processModuleDeployer() throws Exception {
		return new YarnModuleDeployer(yarnCloudAppService(), yarnCloudAppStateMachine().buildStateMachine());
	}

	@Bean
	public ModuleDeployer taskModuleDeployer() throws Exception {
		// TODO: not yet supported but using same deployer for admin not to fail
		return new YarnModuleDeployer(yarnCloudAppService(), yarnCloudAppStateMachine().buildStateMachine(false));
	}

	@Bean
	public YarnCloudAppStateMachine yarnCloudAppStateMachine() throws Exception {
		return new YarnCloudAppStateMachine(yarnCloudAppService(), yarnModuleDeployerTaskExecutor());
	}

	@Bean
	public YarnCloudAppService yarnCloudAppService() {
		return new DefaultYarnCloudAppService();
	}

	@Bean
	public TaskExecutor yarnModuleDeployerTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(1);
		return taskExecutor;
	}

}
