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
package org.springframework.cloud.skipper.server.controller.docs;

import org.springframework.cloud.skipper.server.repository.jpa.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.jpa.PackageMetadataRepository;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.cloud.skipper.server.repository.jpa.RepositoryRepository;
import org.springframework.cloud.skipper.server.repository.map.DeployerRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.map.repository.config.EnableMapRepositories;

import static org.mockito.Mockito.mock;

/**
 * Configuration class for repositories.
 *
 * @author Ilayaperumal Gopinathan
 */
@Configuration
public class RepositoryConfiguration {

	@Configuration
	@Profile("!repository")
	public class RepositoryMocks {
		@Bean
		public RepositoryRepository repositoryRepository() {
			return mock(RepositoryRepository.class);
		}

		@Bean
		public PackageMetadataRepository packageMetadataRepository() {
			return mock(PackageMetadataRepository.class);
		}

		@Bean
		public ReleaseRepository releaseRepository() {
			return mock(ReleaseRepository.class);
		}

		@Bean
		public DeployerRepository deployerRepository() {
			return mock(DeployerRepository.class);
		}

		@Bean
		public AppDeployerDataRepository appDeployerDataRepository() {
			return mock(AppDeployerDataRepository.class);
		}
	}

	@Configuration
	@EnableMapRepositories(basePackages = "org.springframework.cloud.skipper.server.repository.map")
	@EnableJpaRepositories(basePackages = "org.springframework.cloud.skipper.server.repository.jpa")
	@Profile("repository")
	public class JPARepositoryConfiguration {
	}
}
