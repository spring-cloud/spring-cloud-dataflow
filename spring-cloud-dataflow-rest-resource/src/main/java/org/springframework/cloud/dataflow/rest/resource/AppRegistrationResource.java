/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.resource;

import java.util.Set;

import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;

/**
 * Rest resource for an app registration.
 *
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
public class AppRegistrationResource extends RepresentationModel<AppRegistrationResource> {

	/**
	 * App name.
	 */
	private String name;

	/**
	 * App type.
	 */
	private String type;

	/**
	 * URI for app resource, such as {@code maven://groupId:artifactId:version}.
	 */
	private String uri;

	/**
	 * URI for app metaData
	 */
	private String metaDataUri;
	/**
	 * App version.
	 */
	private String version;

	/**
	 * Is default app version for all (name, type) applications
	 */
	private Boolean defaultVersion;

	/**
	 * All the registered versions for this app.
	 */
	private Set<String> versions;

	/**
	 * The label name of the application.
	 */
	private String label;

	/**
	 * Default constructor for serialization frameworks.
	 */
	protected AppRegistrationResource() {
	}

	public AppRegistrationResource(String name, String type, String uri) {
		this(name, type, null, uri, null, false);
	}

	/**
	 * Construct a {@code AppRegistrationResource}.
	 *
	 * @param name app name
	 * @param type app type
	 * @param version app version
	 * @param uri uri for app resource
	 * @param defaultVersion is this application selected to the be default version in DSL
	 */
	public AppRegistrationResource(String name, String type, String version, String uri, String metaDataUri, Boolean defaultVersion) {
		this.name = name;
		this.type = type;
		this.version = version;
		this.uri = uri;
		this.metaDataUri = metaDataUri;
		this.defaultVersion = defaultVersion;
	}

	/**
	 * Construct a {@code AppRegistrationResource}.
	 *
	 * @param name app name
	 * @param type app type
	 * @param version app version
	 * @param uri uri for app resource
	 * @param metaDataUri uri for app metadata
	 * @param defaultVersion is this application selected to the be default version in DSL
	 * @param versions all the registered versions of this application
	 */
	public AppRegistrationResource(String name, String type, String version, String uri, String metaDataUri, Boolean defaultVersion, Set<String> versions) {
		this.name = name;
		this.type = type;
		this.version = version;
		this.uri = uri;
		this.metaDataUri = metaDataUri;
		this.defaultVersion = defaultVersion;
		this.versions = versions;
	}

	/**
	 * Construct a {@code AppRegistrationResource}.
	 *
	 * @param name app name
	 * @param type app type
	 * @param version app version
	 * @param uri uri for app resource
	 * @param metaDataUri uri for app metadata
	 * @param defaultVersion is this application selected to the be default version in DSL
	 * @param versions all the registered versions of this application
	 * @param label the label name of the application
	 */
	public AppRegistrationResource(String name, String type, String version, String uri, String metaDataUri, Boolean defaultVersion, Set<String> versions, String label) {
		this.name = name;
		this.type = type;
		this.version = version;
		this.uri = uri;
		this.metaDataUri = metaDataUri;
		this.defaultVersion = defaultVersion;
		this.versions = versions;
		this.label = label;
	}

	/**
	 * @return the name of the app
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return type type of the app
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return type URI for the app resource
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * @return version of the app
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return if this app selected to be the default
	 */
	public Boolean getDefaultVersion() {
		return defaultVersion;
	}

	/**
	 * @return all the available versions of the app
	 */
	public Set<String> getVersions() {
		return this.versions;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getMetaDataUri() {
		return metaDataUri;
	}

	/**
	 * Dedicated subclass to workaround type erasure.
	 */
	public static class Page extends PagedModel<AppRegistrationResource> {
	}

}
