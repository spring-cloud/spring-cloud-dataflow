/*
 * Copyright 2021 the original author or authors.
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

import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

/**
 * Custom Repository interface for managing the {@link AppRegistration} class.
 * @author Siddhant Sorann
 */
public interface AppRegistrationRepositoryCustom {

	/**
	 * Function to find all app registrations based on various optional parameters using
	 * predicates.
	 * @param type application type.
	 * @param name application name.
	 * @param version application version.
	 * @param defaultVersion default version.
	 * @param pageable enumerates the data to be returned.
	 * @return paginated list of filtered app registrations.
	 */
	Page<AppRegistration> findAllByTypeAndNameIsLikeAndVersionAndDefaultVersion(
			@Nullable ApplicationType type, @Nullable String name, @Nullable String version, boolean defaultVersion,
			Pageable pageable);

}
