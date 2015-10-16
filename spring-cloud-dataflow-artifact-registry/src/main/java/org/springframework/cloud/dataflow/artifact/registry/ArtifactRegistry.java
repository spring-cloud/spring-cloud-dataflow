/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.artifact.registry;


import java.util.List;

import org.springframework.cloud.dataflow.core.ArtifactType;

/**
 * {@code ArtifactRegistry} is used to manage artifact registrations.
 * This includes operations such as find, register, and delete.
 *
 * @see ArtifactRegistration
 *
 * @author Eric Bottard
 * @author David Turanski
 * @author Mark Fisher
 * @author Patrick Peralta
 */
public interface ArtifactRegistry {

	/**
	 * Look up the registration for an artifact by name and type. If
	 * a registration does not exist, {@code null} is returned.
	 *
	 * @param name the artifact name
	 * @param type the artifact type
	 *
	 * @return registration for an artifact, or {@code null} if not found
	 */
	ArtifactRegistration find(String name, ArtifactType type);

	/**
	 * Return all artifact registrations.
	 *
	 * @return all artifact registrations
	 */
	List<ArtifactRegistration> findAll();

	/**
	 * Save a new artifact registration. Pre-existing registrations
	 * with a given name and type will be overwritten.
	 *
	 * @param registration module/library registration to save
	 */
	void save(ArtifactRegistration registration);

	/**
	 * Unregister an artifact by name and type.
	 *
	 * @param name the artifact name
	 * @param type the artifact type
	 */
	void delete(String name, ArtifactType type);

}
