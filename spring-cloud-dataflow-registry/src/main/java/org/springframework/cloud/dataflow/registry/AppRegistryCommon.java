/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.dataflow.registry;

import java.util.List;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.core.io.Resource;

/**
 * Common interface (bridge) for the legacy {@link AppRegistry} and the new
 * {@link org.springframework.cloud.dataflow.registry.service.DefaultAppRegistryService}
 *
 * This interface bride is required to enable SCDF working both in classic or skipper
 * mode.
 *
 * @author Christian Tzolov
 */
public interface AppRegistryCommon {

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
	 * @param type application version
	 * @return the application with those name and type and default version
	 */
	default AppRegistration find(String name, ApplicationType type, String version) {
		throw new UnsupportedOperationException("version is not supported in Classic mode");
	}

	/**
	 * Import bulk of applications from input load files
	 * @param overwrite if set to true this command will override and existing application
	 * with same name:type:version If set to false operation will throw an exception in the
	 * application already exists.
	 * @param resources list of input load files
	 * @return list of application being imported
	 */
	List<AppRegistration> importAll(boolean overwrite, Resource... resources);

	/**
	 * Converts application's URI into Spring resource object. Supports File:, Http:, Maven:
	 * and Docker: schemas
	 * @param appRegistration
	 * @return Returns {@link Resource} instance that corresponds to application's URI
	 */
	Resource getAppResource(AppRegistration appRegistration);

	/**
	 * Converts application's metadata URI into Spring resource object. Supports File:, Http:,
	 * Maven: and Docker: schemas
	 * @param appRegistration
	 * @return Returns {@link Resource} instance that corresponds to application's metdata URI
	 */
	Resource getAppMetadataResource(AppRegistration appRegistration);

	/**
	 * Save an {@link AppRegistration} instance.
	 * @param app appRegistration to save
	 * @return the saved appRegistration
	 */
	AppRegistration save(AppRegistration app);
}
