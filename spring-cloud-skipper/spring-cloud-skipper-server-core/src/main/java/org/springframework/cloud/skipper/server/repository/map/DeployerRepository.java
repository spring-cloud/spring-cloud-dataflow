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
package org.springframework.cloud.skipper.server.repository.map;

import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for Deployers
 * @author Mark Pollack
 */
@RepositoryRestResource
@Transactional
@SuppressWarnings("unchecked")
public interface DeployerRepository extends KeyValueRepository<Deployer, String>, DeployerRepositoryCustom {

	Deployer findByName(String name);

	@Override
	@RestResource(exported = false)
	Deployer save(Deployer deployer);

	@Override
	@RestResource(exported = false)
	void deleteById(String s);

	@Override
	@RestResource(exported = false)
	void delete(Deployer deployer);

	@Override
	@RestResource(exported = false)
	void deleteAll();
}
