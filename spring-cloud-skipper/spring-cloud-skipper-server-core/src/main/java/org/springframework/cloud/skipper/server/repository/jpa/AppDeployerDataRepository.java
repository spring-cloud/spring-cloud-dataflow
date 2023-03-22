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

import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores data related to the app deployment.
 * @author Mark Pollack
 */
@RepositoryRestResource(exported = false)
@Transactional
public interface AppDeployerDataRepository
		extends KeyValueRepository<AppDeployerData, Long>, AppDeployerDataRepositoryCustom {

	@Transactional(readOnly = true)
	AppDeployerData findByReleaseNameAndReleaseVersion(String releaseName, Integer releaseVersion);

}
