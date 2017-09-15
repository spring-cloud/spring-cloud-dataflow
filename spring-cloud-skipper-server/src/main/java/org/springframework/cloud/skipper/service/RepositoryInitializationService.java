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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.skipper.config.SkipperServerProperties;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.repository.RepositoryRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Takes respository application configuration properties and udpates the Repository
 * database on application startup. Entries are only created if no existing Repository
 * with the same name exists.
 *
 * @author Mark Pollack
 */
@Service
public class RepositoryInitializationService {

	private final Logger logger = LoggerFactory.getLogger(RepositoryInitializationService.class);

	private final RepositoryRepository repositoryRepository;

	private final SkipperServerProperties skipperServerProperties;

	public final static String LOCAL_REPOSITORY_NAME = "local";

	@Autowired
	public RepositoryInitializationService(RepositoryRepository repositoryRepository,
			SkipperServerProperties skipperServerProperties) {
		this.repositoryRepository = repositoryRepository;
		this.skipperServerProperties = skipperServerProperties;
	}

	@EventListener
	public void initialize(ApplicationReadyEvent event) {
		List<Repository> configurationRepositories = skipperServerProperties.getPackageRepositories();
		addLocalRepository();
		for (Repository configurationRepository : configurationRepositories) {
			if (repositoryRepository.findByName(configurationRepository.getName()) == null) {
				logger.info("Initializing repository database with " + configurationRepository);
				repositoryRepository.save(configurationRepository);
			}
			else {
				logger.warn("Ignoring application configuration for " + configurationRepository +
						".  Repository name already in database.");
			}
		}

	}

	private void addLocalRepository() {
		Repository repository = new Repository();
		repository.setName(LOCAL_REPOSITORY_NAME);
		repository.setUrl("file://" + skipperServerProperties.getPackageDir());
		repositoryRepository.save(repository);
	}
}
