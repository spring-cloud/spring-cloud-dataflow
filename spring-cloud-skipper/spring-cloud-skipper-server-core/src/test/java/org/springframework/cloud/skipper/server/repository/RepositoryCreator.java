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
package org.springframework.cloud.skipper.server.repository;

import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.server.repository.jpa.RepositoryRepository;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class RepositoryCreator {

	public static void createTwoRepositories(RepositoryRepository repositoryRepository) {
		Repository repository = new Repository();
		repository.setName("stable");
		repository.setUrl("https://www.example.com/skipper/repository/stable");
		repositoryRepository.save(repository);
		repository = new Repository();
		repository.setName("unstable");
		repository.setUrl("https://www.example.com/skipper/repository/unstable");
		repositoryRepository.save(repository);
	}

	public static void createRepository(RepositoryRepository repositoryRepository, String repoName,
			Integer repoOrder) {
		Repository repository = new Repository();
		repository.setName(repoName);
		repository.setUrl("https://www.example.com/skipper/repository/" + repoName);
		repositoryRepository.save(repository);
		repository.setRepoOrder(repoOrder);
		repositoryRepository.save(repository);
	}
}
