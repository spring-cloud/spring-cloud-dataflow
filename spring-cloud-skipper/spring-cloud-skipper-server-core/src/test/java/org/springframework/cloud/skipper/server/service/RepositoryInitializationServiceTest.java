/*
 * Copyright 2017 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.server.AbstractIntegrationTest;
import org.springframework.cloud.skipper.server.repository.jpa.PackageMetadataRepository;
import org.springframework.cloud.skipper.server.repository.jpa.RepositoryRepository;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Corneil du Plessis
 */
@ActiveProfiles("repo-test")
class RepositoryInitializationServiceTest extends AbstractIntegrationTest {

	@Autowired
	private RepositoryRepository repositoryRepository;

	@Autowired
	private PackageMetadataRepository packageMetadataRepository;

	@Test
	void initialize() throws Exception {
		assertThat(repositoryRepository.count()).isEqualTo(2);
		assertThat(repositoryRepository.findByName("test").getUrl()).isEqualTo("classpath:/repositories/binaries/test");
		// Note, this is a brittle assertion.
		assertThat(packageMetadataRepository.count()).isGreaterThan(5);
	}

}
