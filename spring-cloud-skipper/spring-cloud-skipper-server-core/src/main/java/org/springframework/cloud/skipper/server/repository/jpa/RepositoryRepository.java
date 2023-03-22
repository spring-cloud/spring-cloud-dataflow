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
package org.springframework.cloud.skipper.server.repository.jpa;

import java.util.List;

import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data Repository for {@link Repository} objects.
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@RepositoryRestResource(path = "repositories", collectionResourceRel = "repositories")
@Transactional
public interface RepositoryRepository extends KeyValueRepository<Repository, Long> {

	Repository findByName(@Param("name") String name);

	/**
	 * Get all the repositories with their repository order in descending order.
	 *
	 * @return the list of repositories
	 */
	@RestResource(exported = false)
	List<Repository> findAllByOrderByRepoOrderDesc();

	@Override
	@RestResource(exported = false)
	Repository save(Repository repository);

	@Override
	@RestResource(exported = false)
	void deleteById(Long id);

	@Override
	@RestResource(exported = false)
	void delete(Repository deployer);

	@Override
	@RestResource(exported = false)
	void deleteAll();

}
