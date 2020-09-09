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

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for Releases
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@RepositoryRestResource(path = "releases", collectionResourceRel = "releases")
@Transactional
@SuppressWarnings("unchecked")
public interface ReleaseRepository extends KeyValueRepository<Release, Long>, ReleaseRepositoryCustom {

	@Override
	@RestResource(exported = false)
	Release save(Release release);

	@Override
	@RestResource(exported = false)
	void deleteById(Long id);

	@Override
	@RestResource(exported = false)
	void delete(Release release);

	@Override
	@RestResource(exported = false)
	void deleteAll();

	@Transactional(readOnly = true)
	@RestResource(exported = false)
	List<Release> findByNameOrderByVersionDesc(@Param("name") String name);

	@Transactional(readOnly = true)
	List<Release> findByNameIgnoreCaseContainingOrderByNameAscVersionDesc(@Param("name") String name);

	@Transactional(readOnly = true)
	@RestResource(exported = false)
	List<Release> findByNameAndVersionBetweenOrderByNameAscVersionDesc(@Param("name") String name,
			@Param("from") int fromVersion, @Param("to") int toVersion);

	@Transactional(readOnly = true)
	@RestResource(exported = false)
	Release findTopByNameOrderByVersionDesc(@Param("name") String name);

	@Transactional(readOnly = true)
	@RestResource(exported = false)
	Release findTopByNameAndInfoStatusStatusCodeNotOrderByVersionDesc(@Param("name") String name,
		@Param("statusCode") StatusCode statusCode);

	@Transactional(readOnly = true)
	@RestResource(exported = false)
	List<Release> findByNameIgnoreCaseContaining(@Param("name") String name);

	/**
	 * Return all releases that are associated with the provided package and repository id in decending
	 * version order.
	 *
	 * @param repositoryId the Id of the repository where the package is located
	 * @param packageMetadataId the id of the package metadata
	 * @return @return releases associated with the provided package and repository id
	 */
	@Transactional(readOnly = true)
	@RestResource(exported = false)
	List<Release> findByRepositoryIdAndPackageMetadataIdOrderByNameAscVersionDesc(Long repositoryId, Long packageMetadataId);
}
