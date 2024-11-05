/*
 * Copyright 2018-2019 the original author or authors.
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

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.dataflow.core.ConfigurationMetadataPropertyEntity;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Mark Pollack
 * @author David Turanski
 */
@Component
public class LauncherInitializationService {

	private final Logger logger = LoggerFactory.getLogger(LauncherInitializationService.class);
	private final List<TaskPlatform> taskPlatforms;
	private LauncherRepository launcherRepository;
	private final DeployerConfigurationMetadataResolver resolver;
	private static final String KEY_PREFIX = "spring.cloud.deployer.";

	public LauncherInitializationService(LauncherRepository launcherRepository, List<TaskPlatform> taskPlatforms,
			DeployerConfigurationMetadataResolver resolver) {
		this.launcherRepository = launcherRepository;
		this.taskPlatforms = taskPlatforms;
		this.resolver = resolver;
	}

	@EventListener
	@Transactional
	public void initialize(ApplicationReadyEvent event) {
		List<ConfigurationMetadataProperty> metadataProperties = this.resolver.resolve();
		this.taskPlatforms.forEach(platform -> {
			platform.getLaunchers().forEach(launcher -> {
				List<ConfigurationMetadataPropertyEntity> options = createMetadataPropertyEntities(metadataProperties,
						launcher.getType());
				launcher.setOptions(options);
				this.launcherRepository.save(launcher);
				logger.info(
						"Added '{}' platform account '{}' into Task Launcher repository.",
						platform.getName(),
						launcher.getName());
			});
		});
	}

	private List<ConfigurationMetadataPropertyEntity> createMetadataPropertyEntities(
			List<ConfigurationMetadataProperty> metadataProperties, String type) {
		String prefix = KEY_PREFIX + type.toLowerCase(Locale.ROOT);
		return metadataProperties.stream()
			.filter(p -> p.getId().startsWith(prefix))
			.map(ConfigurationMetadataPropertyEntity::new)
			.collect(Collectors.toList());
	}
}
