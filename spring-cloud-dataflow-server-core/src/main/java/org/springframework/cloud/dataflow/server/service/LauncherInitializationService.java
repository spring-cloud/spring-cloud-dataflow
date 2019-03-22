/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.dataflow.server.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.cloud.deployer.spi.local.LocalTaskLauncher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @author Mark Pollack
 */
@Component
public class LauncherInitializationService {

	private final Logger logger = LoggerFactory
			.getLogger(LauncherInitializationService.class);
	private final List<TaskPlatform> taskPlatforms;
	private LauncherRepository launcherRepository;

	public LauncherInitializationService(LauncherRepository launcherRepository, List<TaskPlatform> taskPlatforms) {
		this.launcherRepository = launcherRepository;
		this.taskPlatforms = taskPlatforms;
	}

	@EventListener
	@Transactional
	public void initialize(ApplicationReadyEvent event) {
		if (noTaskLauncherExists()) {
			LocalDeployerProperties localDeployerProperties = new LocalDeployerProperties();
			LocalTaskLauncher localTaskLauncher = new LocalTaskLauncher(localDeployerProperties);
			Launcher launcher = new Launcher("default", "local", localTaskLauncher);
			launcher.setDescription(prettyPrintLocalDeployerProperties(localDeployerProperties));
			List<Launcher> localLaunchers = new ArrayList<>();
			localLaunchers.add(launcher);
			for (TaskPlatform taskPlatform : taskPlatforms) {
				if (taskPlatform.getName().equalsIgnoreCase("Local")) {
					logger.info("Creating Local Task Launcher named 'default' since no Task Launchers configured.");
					taskPlatform.setLaunchers(localLaunchers);
				}
			}
		}

		this.taskPlatforms.forEach(platform -> {
			platform.getLaunchers().forEach(launcher -> {
				this.launcherRepository.save(launcher);
				logger.info(String.format(
						"Added '%s' platform account '%s' into Task Launcher repository.",
						platform.getName(),
						launcher.getName()));
			});
		});
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

	private boolean noTaskLauncherExists() {
		int taskLauncherCount = 0;
		for (TaskPlatform platform : this.taskPlatforms) {
			taskLauncherCount = taskLauncherCount + platform.getLaunchers().size();
		}
		return (taskLauncherCount == 0);
	}
}
