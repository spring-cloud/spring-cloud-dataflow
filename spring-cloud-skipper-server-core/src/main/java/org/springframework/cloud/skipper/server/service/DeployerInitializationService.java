/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.cloud.skipper.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Platform;
import org.springframework.cloud.skipper.domain.deployer.ConfigurationMetadataPropertyEntity;
import org.springframework.cloud.skipper.server.deployer.metadata.DeployerConfigurationMetadataResolver;
import org.springframework.cloud.skipper.server.repository.map.DeployerRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Populates the DeployerRepository with AppDeployer instances
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Donovan Muller
 */
@Component
public class DeployerInitializationService {

	private final Logger logger = LoggerFactory
			.getLogger(DeployerInitializationService.class);
	private static final String KEY_PREFIX = "spring.cloud.deployer.";
	private final DeployerRepository deployerRepository;
	private final List<Platform> platforms;
	private final DeployerConfigurationMetadataResolver resolver;

	public DeployerInitializationService(DeployerRepository deployerRepository, List<Platform> platforms,
			DeployerConfigurationMetadataResolver resolver) {
		this.deployerRepository = deployerRepository;
		this.platforms = platforms;
		this.resolver = resolver;
	}

	@EventListener
	@Transactional
	public void initialize(ApplicationReadyEvent event) {
		List<ConfigurationMetadataProperty> metadataProperties = this.resolver.resolve();
		if (singleDeployerExists()) {
			for (Platform platform : this.platforms) {
				if (platform.getDeployers().size() == 1) {
					List<Deployer> updatedDeployers = new ArrayList<>();
					List<Deployer> deployers = platform.getDeployers();

					Deployer existingDeployer = deployers.get(0);
					List<ConfigurationMetadataPropertyEntity> options = createMetadataPropertyEntities(
							metadataProperties, existingDeployer.getType());
					existingDeployer.setOptions(options);
					if (!"default".equalsIgnoreCase(existingDeployer.getName())) {
						Deployer defaultDeployer = new Deployer("default",
								existingDeployer.getType(), existingDeployer.getAppDeployer());
						defaultDeployer.setDescription(existingDeployer.getDescription());
						updatedDeployers.add(defaultDeployer);
					}
					updatedDeployers.addAll(deployers);
					platform.setDeployers(updatedDeployers);
				}
			}
		}
		this.platforms.forEach(platform -> {
			platform.getDeployers().forEach(deployer -> {
				List<ConfigurationMetadataPropertyEntity> options = createMetadataPropertyEntities(metadataProperties,
						deployer.getType());
				deployer.setOptions(options);
				this.deployerRepository.save(deployer);
				logger.info(String.format(
						"Added '%s' platform account '%s' into deployer repository.",
						platform.getName(),
						deployer.getName()));
			});
		});
	}

	private List<ConfigurationMetadataPropertyEntity> createMetadataPropertyEntities(
			List<ConfigurationMetadataProperty> metadataProperties, String type) {
		String prefix = KEY_PREFIX + type;
		return metadataProperties.stream()
			.filter(p -> p.getId().startsWith(prefix))
			.map(ConfigurationMetadataPropertyEntity::new)
			.collect(Collectors.toList());
	}

	private boolean singleDeployerExists() {
		int deployersCount = 0;
		for (Platform platform : this.platforms) {
			deployersCount = deployersCount + platform.getDeployers().size();
		}
		return (deployersCount > 1) ? false : true;
	}
}
