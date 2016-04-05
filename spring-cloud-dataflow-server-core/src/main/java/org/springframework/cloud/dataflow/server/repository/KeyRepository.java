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

import java.io.Serializable;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * Interface for generic {@link Repository} which does a backmapping from key into
 * a identifier.
 *
 * @author Janne Valkealahti
 *
 * @param <T> the type of entity
 * @param <ID> the type of id
 */
@NoRepositoryBean
public interface KeyRepository<T, ID extends Serializable> extends Repository<T, ID> {

	/**
	 * Saves a given key with an associated identifier.
	 *
	 * @param <S> the key type
	 * @param key the key
	 * @param id the identifier
	 * @return the saved key
	 */
	<S extends T> S save(S key, ID id);

	/**
	 * Find a key by its identifier.
	 *
	 * @param key the key
	 * @return the identifier
	 */
	ID findOne(T key);
}
