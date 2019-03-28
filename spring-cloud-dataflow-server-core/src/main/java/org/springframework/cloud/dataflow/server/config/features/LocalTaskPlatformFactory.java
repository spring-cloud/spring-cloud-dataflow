/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config.features;

import java.util.Collections;

import org.springframework.cloud.dataflow.core.AbstractTaskPlatformFactory;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.cloud.deployer.spi.local.LocalTaskLauncher;
import org.springframework.util.StringUtils;

/**
 * @author David Turanski
 **/
public class LocalTaskPlatformFactory extends AbstractTaskPlatformFactory<LocalPlatformProperties> {

	private static final String PLATFORM_TYPE = "Local";

	public LocalTaskPlatformFactory(LocalPlatformProperties platformProperties) {
		super(platformProperties, PLATFORM_TYPE);
	}

	@Override
	public TaskPlatform createTaskPlatform() {
		TaskPlatform taskPlatform = super.createTaskPlatform();
		if (taskPlatform.getLaunchers().size() == 0) {
			taskPlatform.setLaunchers(Collections.singletonList(createDefaultLauncher()));
		}
		return taskPlatform;
	}

	@Override
	public Launcher createLauncher(String account) {
		LocalDeployerProperties deployerProperties = platformProperties.accountProperties(account);
		return doCreateLauncher(account, deployerProperties);
	}

	private Launcher createDefaultLauncher() {
		return doCreateLauncher("default", new LocalDeployerProperties());
	}

	private Launcher doCreateLauncher(String account, LocalDeployerProperties deployerProperties) {
		LocalTaskLauncher localTaskLauncher = new LocalTaskLauncher(deployerProperties);
		Launcher launcher = new Launcher(account, PLATFORM_TYPE, localTaskLauncher);
		launcher.setDescription(prettyPrintLocalDeployerProperties(deployerProperties));
		return launcher;
	}

	private String prettyPrintLocalDeployerProperties(LocalDeployerProperties localDeployerProperties) {
		StringBuilder builder = new StringBuilder();
		if (localDeployerProperties.getJavaOpts() != null) {
			builder.append("JavaOpts = [" + localDeployerProperties.getJavaOpts() + "], ");
		}
		builder.append("ShutdownTimeout = [" + localDeployerProperties.getShutdownTimeout() + "], ");
		builder.append("EnvVarsToInherit = ["
				+ StringUtils.arrayToCommaDelimitedString(localDeployerProperties.getEnvVarsToInherit()) + "], ");
		builder.append("JavaCmd = [" + localDeployerProperties.getJavaCmd() + "], ");
		builder.append("WorkingDirectoriesRoot = [" + localDeployerProperties.getWorkingDirectoriesRoot() + "], ");
		builder.append("DeleteFilesOnExit = [" + localDeployerProperties.isDeleteFilesOnExit() + "]");
		return builder.toString();
	}
}
