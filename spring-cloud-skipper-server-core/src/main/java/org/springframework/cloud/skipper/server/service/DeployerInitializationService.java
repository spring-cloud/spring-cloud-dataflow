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
package org.springframework.cloud.skipper.server.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.skipper.domain.Platform;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

/**
 * Populates the DeployerRepository with AppDeployer instances
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Donovan Muller
 */
public class DeployerInitializationService {

	private final Logger logger = LoggerFactory
			.getLogger(DeployerInitializationService.class);

	private DeployerRepository deployerRepository;

	private final List<Platform> platforms;

	public DeployerInitializationService(DeployerRepository deployerRepository, List<Platform> platforms) {
		this.deployerRepository = deployerRepository;
		this.platforms = platforms;
	}

	@EventListener
	@Transactional
	public void initialize(ApplicationReadyEvent event) {
		platforms.forEach(platform -> {
			platform.getDeployers().forEach(deployer -> {
				this.deployerRepository.save(deployer);
				logger.info(String.format(
						"Added '%s' platform account '%s' into deployer repository.",
						platform.getName(),
						deployer.getName()));
			});
		});
	}
}
