/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.dataflow.server.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Mark Pollack
 */
@Component
public class LauncherInitializationService {

	private final Logger logger = LoggerFactory
			.getLogger(LauncherInitializationService.class);
	private final List<TaskPlatform> platforms;
	private LauncherRepository launcherRepository;

	public LauncherInitializationService(LauncherRepository launcherRepository, List<TaskPlatform> platforms) {
		this.launcherRepository = launcherRepository;
		this.platforms = platforms;
	}

	@EventListener
	@Transactional
	public void initialize(ApplicationReadyEvent event) {
		if (singleDeployerExists()) {
			for (TaskPlatform platform : this.platforms) {
				if (platform.getLaunchers().size() == 1) {
					List<Launcher> updatedLaunchers = new ArrayList<>();
					List<Launcher> launchers = platform.getLaunchers();

					Launcher existingLauncher = launchers.get(0);
					if (existingLauncher.getName() != "default") {
						Launcher defaultLauncher = new Launcher("default",
								existingLauncher.getType(), existingLauncher.getTaskLauncher());
						defaultLauncher.setDescription(existingLauncher.getDescription());
						updatedLaunchers.add(defaultLauncher);
					}
					updatedLaunchers.addAll(launchers);
					platform.setLaunchers(updatedLaunchers);
				}
			}
		}
		this.platforms.forEach(platform -> {
			platform.getLaunchers().forEach(launcher -> {
				this.launcherRepository.save(launcher);
				logger.info(String.format(
						"Added '%s' platform account '%s' into Task Launcher repository.",
						platform.getName(),
						launcher.getName()));
			});
		});
	}

	private boolean singleDeployerExists() {
		int deployersCount = 0;
		for (TaskPlatform platform : this.platforms) {
			deployersCount = deployersCount + platform.getLaunchers().size();
		}
		return (deployersCount > 1) ? false : true;
	}
}
