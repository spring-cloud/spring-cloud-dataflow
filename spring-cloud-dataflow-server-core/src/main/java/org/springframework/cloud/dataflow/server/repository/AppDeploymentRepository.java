/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.repository;

import org.springframework.stereotype.Repository;

/**
 * Interface for repository working with an {@link AppDeploymentKey} backmapping.
 *
 * @author Janne Valkealahti
 */
@Repository
public interface AppDeploymentRepository extends org.springframework.data.repository.Repository<AppDeploymentKey, String> {

	/**
	 * Saves a given {@link AppDeploymentKey} with an associated identifier.
	 *
	 * @param key the app deployment key
	 * @param id the identifier
	 * @return the saved key
	 */
	AppDeploymentKey save(AppDeploymentKey key, String id);

	/**
	 * Find an identifier by its key.
	 *
	 * @param key the app deployment key
	 * @return the identifier
	 */
	String findOne(AppDeploymentKey key);
}
