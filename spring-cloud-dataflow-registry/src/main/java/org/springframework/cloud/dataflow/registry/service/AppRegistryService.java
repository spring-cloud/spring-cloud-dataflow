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
package org.springframework.cloud.dataflow.registry.service;

import java.net.URI;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.AppRegistryCommon;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author Christian Tzolov
 */
public interface AppRegistryService extends AppRegistryCommon {

	/**
	 * @param name application name
	 * @param type application version
	 * @return the default application for this name:type pair. Returns null if no name:type
	 * application exists or if none of the name:type applications is set as default.
	 */
	AppRegistration getDefaultApp(String name, ApplicationType type);

	/**
	 * Set an application with name, type and version as the default for all name:type
	 * applications. The previous default name:type application is set to non-default.
	 * @param name application name
	 * @param type application type
	 * @param version application version.
	 */
	void setDefaultApp(String name, ApplicationType type, String version);

	/**
	 * Saves a new {@link AppRegistration} identified by its name, type, version and URIs.
	 *
	 * @param name Name of the AppRegistration to save
	 * @param type Type of the AppRegistration to save
	 * @param version Version of the AppRegistration to save
	 * @param uri Resource uri of the AppRegistration to save
	 * @param metadataUri metadata of the AppRegistration to save
	 * @return the saved AppRegistration
	 */
	AppRegistration save(String name, ApplicationType type, String version, URI uri, URI metadataUri);

	/**
	 * Deletes an {@link AppRegistration}. If the {@link AppRegistration} does not exist, a
	 * {@link NoSuchAppRegistrationException} will be thrown.
	 *
	 * @param name Name of the AppRegistration to delete
	 * @param type Type of the AppRegistration to delete
	 * @param version Version of the AppRegistration to delete
	 */
	void delete(String name, ApplicationType type, String version);

	/**
	 * Checks if an {@link AppRegistration} with this name, type and version exists.
	 * @param name application name
	 * @param type application type
	 * @param version application version.
	 * @return true if the AppRegistration exists, false otherwise.
	 */
	boolean appExist(String name, ApplicationType type, String version);

	/**
	 * @param pageable Pagination information
	 * @return returns all {@link AppRegistration}'s including multiple version for the same
	 * application. Uses the pagination.
	 */
	Page<AppRegistration> findAll(Pageable pageable);

	/**
	 * @param type appliation type
	 * @param name application name
	 * @param pageable Pagination information
	 * @return returns all {@link AppRegistration} versions for given name and type. Uses the pagination.
	 */
	Page<AppRegistration> findAllByTypeAndNameIsLike(ApplicationType type, String name, Pageable pageable);
}
