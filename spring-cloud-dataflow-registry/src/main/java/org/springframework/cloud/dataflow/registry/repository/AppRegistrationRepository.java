/*
 * Copyright 2018-2020 the original author or authors.
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
package org.springframework.cloud.dataflow.registry.repository;

import java.util.List;

import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository interface for managing the {@link AppRegistration} class.
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 */
@Transactional
public interface AppRegistrationRepository extends KeyValueRepository<AppRegistration, Long> {

	AppRegistration findAppRegistrationByNameAndTypeAndVersion(String name, ApplicationType type, String version);

	AppRegistration findAppRegistrationByNameAndTypeAndDefaultVersionIsTrue(String name, ApplicationType type);

	void deleteAppRegistrationByNameAndTypeAndVersion(String name, ApplicationType type, String version);

	Page<AppRegistration> findAllByTypeAndNameContainingIgnoreCase(ApplicationType type, String name, Pageable pageable);

	Page<AppRegistration> findAllByType(ApplicationType type, Pageable pageable);

	Page<AppRegistration> findAllByNameContainingIgnoreCase(String name, Pageable pageable);

	Page<AppRegistration> findAllByDefaultVersionIsTrue(Pageable pageable);

	Page<AppRegistration> findAllByTypeAndNameContainingIgnoreCaseAndDefaultVersionIsTrue(ApplicationType type, String name, Pageable pageable);

	Page<AppRegistration> findAllByTypeAndDefaultVersionIsTrue(ApplicationType type, Pageable pageable);

	Page<AppRegistration> findAllByNameContainingIgnoreCaseAndDefaultVersionIsTrue(String name, Pageable pageable);

	@Override
	<S extends AppRegistration> S save(S s);

	@Override
	List<AppRegistration> findAll();
}
