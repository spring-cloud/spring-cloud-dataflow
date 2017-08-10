/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.deployer.spi.local.LocalAppDeployer;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.cloud.skipper.config.CloudFoundryPlatformProperties;
import org.springframework.cloud.skipper.config.LocalPlatformProperties;
import org.springframework.cloud.skipper.deployer.Deployer;
import org.springframework.cloud.skipper.repository.DeployerRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Populates the DeployerRepository with AppDeployer instances
 * @author Mark Pollack
 */
@Service
public class DeployerInitializationService {

	private final Logger logger = LoggerFactory.getLogger(DeployerInitializationService.class);

	private LocalPlatformProperties localPlatformProperties;

	private CloudFoundryPlatformProperties cloudFoundryPlatformProperties;

	private DeployerRepository deployerRepository;

	@Autowired
	public DeployerInitializationService(DeployerRepository deployerRepository,
			LocalPlatformProperties localPlatformProperties,
			CloudFoundryPlatformProperties cloudFoundryPlatformProperties) {
		this.deployerRepository = deployerRepository;
		this.localPlatformProperties = localPlatformProperties;
		this.cloudFoundryPlatformProperties = cloudFoundryPlatformProperties;
	}

	@EventListener
	public void initialize(ApplicationReadyEvent event) {
		createAndSaveLocalAppDeployers();
		// TODO create and save CF Deployers.
	}

	protected void createAndSaveLocalAppDeployers() {
		Map<String, LocalDeployerProperties> localDeployerPropertiesMap = localPlatformProperties.getAccounts();
		for (Map.Entry<String, LocalDeployerProperties> localDeployerPropertiesEntry : localDeployerPropertiesMap
				.entrySet()) {
			LocalAppDeployer localAppDeployer = new LocalAppDeployer(localDeployerPropertiesEntry.getValue());
			Deployer deployer = new Deployer(localDeployerPropertiesEntry.getKey(), "local", localAppDeployer);
			deployerRepository.save(deployer);
		}
	}
}
