/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import java.util.Properties;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.DetailedAppRegistrationResource;
import org.springframework.hateoas.PagedResources;

/**
 * Interface defining operations available for application registrations.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Eric Bottard
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public interface AppRegistryOperations {

	/**
	 * Return a list of all application registrations.
	 *
	 * @return list of all application registrations
	 */
	PagedResources<AppRegistrationResource> list();

	/**
	 * Return a list of all application registrations for the given
	 * {@link ApplicationType}.
	 *
	 * @param type application type for which to return a list of registrations
	 * @return list of all application registrations for the given application type
	 */
	PagedResources<AppRegistrationResource> list(ApplicationType type);

	/**
	 * Retrieve information about an application registration.
	 *
	 * @param name name of application
	 * @param type application type
	 * @return detailed information about an application registration
	 */
	DetailedAppRegistrationResource info(String name, ApplicationType type);

	/**
	 * Register an application name and type with its Maven coordinates.
	 *
	 * @param name application name
	 * @param type application type
	 * @param uri URI for the application artifact
	 * @param metadataUri URI for the application metadata artifact
	 * @param force if {@code true}, overwrites a pre-existing registration
	 * @return the new app registration
	 */
	AppRegistrationResource register(String name, ApplicationType type, String uri, String metadataUri, boolean force);

	/**
	 * Unregister an application name and type.
	 *
	 * @param name application name
	 * @param type application type
	 */
	void unregister(String name, ApplicationType type);

	/**
	 * Register all applications listed in a properties file.
	 *
	 * @param uri URI for the properties file
	 * @param force if {@code true}, overwrites any pre-existing registrations
	 * @return the paged list of new app registrations
	 */
	PagedResources<AppRegistrationResource> importFromResource(String uri, boolean force);

	/**
	 * Register all applications provided as key/value pairs.
	 *
	 * @param apps the apps as key/value pairs where key is "type.name" and value is a URI
	 * @param force if {@code true}, overwrites any pre-existing registrations
	 * @return the paged list of new app registrations
	 */
	PagedResources<AppRegistrationResource> registerAll(Properties apps, boolean force);

}
