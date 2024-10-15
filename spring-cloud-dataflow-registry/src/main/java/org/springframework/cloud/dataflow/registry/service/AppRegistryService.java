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
package org.springframework.cloud.dataflow.registry.service;

import java.net.URI;
import java.util.List;

import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

/**
 * @author Christian Tzolov
 * @author Chris Schaefer
 * @author Ilayaperumal Gopinathan
 */
public interface AppRegistryService {

	/**
	 * @param name application name
	 * @param type application version
	 * @return the default application for this name:type pair. Returns null if no name:type
	 * application exists or if none of the name:type applications is set as default.
	 */
	AppRegistration getDefaultApp(String name, ApplicationType type);

	/**
	 * Validate given registration with given uri and version. Validation
	 * will fail if given uri logically impossible to use with further
	 * expected logic with using versions.
	 *
	 * @param registration app registration
	 * @param uri uri of the registration
	 * @param version version of the registration
	 */
	void validate(AppRegistration registration, String uri, String version);

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
	 * Deletes all provided {@link AppRegistration}'s.
	 * @param appRegistrations the {@link AppRegistration}s that will be deleted
	 */
	void deleteAll(Iterable<AppRegistration> appRegistrations);

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
	 * @return returns all {@link AppRegistration} versions for given name and type. Uses the
	 * pagination.
	 */
	Page<AppRegistration> findAllByTypeAndNameIsLike(ApplicationType type, String name, Pageable pageable);


	/**
	 * @param type appliation type
	 * @param name application name
	 * @param pageable Pagination information
	 * @return returns the {@link AppRegistration}s that have the default version set to `true` and matches the given name and type. Uses the
	 * pagination.
	 */
	Page<AppRegistration> findAllByTypeAndNameIsLikeAndDefaultVersionIsTrue(ApplicationType type, String name, Pageable pageable);

	/**
	 * Checks if an application with such name and type exists and is set as default.
	 * @param name applicaiton name
	 * @param type applicaiton type
	 * @return true if a default application with this name and type exists.
	 */
	boolean appExist(String name, ApplicationType type);

	/**
	 * @return returns all {@link AppRegistration}'s including multiple version for the same
	 * application.
	 */
	List<AppRegistration> findAll();

	/**
	 * @param name application name
	 * @param type application typ
	 * @return the application with those name and type and default version
	 */
	AppRegistration find(String name, ApplicationType type);

	/**
	 * @param name application name
	 * @param type application type
	 * @param version application version
	 * @return the application with those name and type and default version
	 */
	default AppRegistration find(String name, ApplicationType type, String version) {
		throw new UnsupportedOperationException("version is not supported in Classic mode");
	}

	/**
	 * Import bulk of applications from input load files
	 * @param overwrite if set to true this command will override and existing application
	 *     with same name:type:version If set to false operation will throw an exception in
	 *     the application already exists.
	 * @param resources list of input load files
	 * @return list of application being imported
	 */
	List<AppRegistration> importAll(boolean overwrite, Resource... resources);

	/**
	 * Converts application's URI into Spring resource object. Supports File:, Http:, Maven:
	 * and Docker: schemas
	 * @param appRegistration the application registration
	 * @return Returns {@link Resource} instance that corresponds to application's URI
	 */
	Resource getAppResource(AppRegistration appRegistration);

	/**
	 * Converts application's metadata URI into Spring resource object. Supports File:, Http:,
	 * Maven: and Docker: schemas
	 * @param appRegistration the application registration
	 * @return Returns {@link Resource} instance that corresponds to application's metdata URI
	 */
	Resource getAppMetadataResource(AppRegistration appRegistration);

	/**
	 * Save an {@link AppRegistration} instance.
	 * @param app appRegistration to save
	 * @return the saved appRegistration
	 */
	AppRegistration save(AppRegistration app);

	/**
	 * @param resource to retrieve the version for
	 * @return Returns the version for the provided resource
	 */
	String getResourceVersion(Resource resource);

	/**
	 * Returns a string representing the resource with version subtracted
	 * @param resource to be represented as string.
	 * @return String representation of the resource.
	 */
	String getResourceWithoutVersion(Resource resource);

	/**
	 * Returns the version for the given resource URI string.
	 * @param uriString String representation of the resource URI
	 * @return the resource version
	 */
	String getResourceVersion(String uriString);

	/**
	 * Returns all app registrations based on various optional parameters.
	 * @param type application type
	 * @param name application name
	 * @param version application version
	 * @param defaultVersion application default version
	 * @param pageable Pagination information
	 * @return returns all {@link AppRegistration} versions for given name and type. Uses the
	 * pagination.
	 */
	Page<AppRegistration> findAllByTypeAndNameIsLikeAndVersionAndDefaultVersion(@Nullable ApplicationType type,
			@Nullable String name, @Nullable String version, boolean defaultVersion, Pageable pageable);
}
