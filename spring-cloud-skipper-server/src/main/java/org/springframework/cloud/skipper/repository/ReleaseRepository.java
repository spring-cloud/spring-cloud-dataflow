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
package org.springframework.cloud.skipper.repository;

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * @author Mark Pollack
 */
@RepositoryRestResource
public interface ReleaseRepository extends PagingAndSortingRepository<Release, String>, CustomReleaseRepository {

	@Override
	@RestResource(exported = false)
	Release save(Release release);

	@Override
	@RestResource(exported = false)
	void delete(String id);

	@Override
	@RestResource(exported = false)
	void delete(Release release);

	@Override
	@RestResource(exported = false)
	void deleteAll();
}
