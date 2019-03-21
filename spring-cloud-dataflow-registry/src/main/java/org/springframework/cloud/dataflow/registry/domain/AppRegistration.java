/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.registry.domain;

import java.net.URI;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.util.Assert;

/**
 * Models the registration of applications.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Christian Tzolov
 * @author Vinicius Carvalho
 */
@Entity
@Table(name = "APP_REGISTRATION")
public class AppRegistration extends AbstractEntity implements Comparable<AppRegistration> {

	/**
	 * App name.
	 */
	private String name;

	/**
	 * App type.
	 */
	private ApplicationType type;

	/**
	 * App version.
	 */
	private String version;

	/**
	 * URI for the app resource.
	 */
	@Lob
	private URI uri;

	/**
	 * URI for the app metadata or {@literal null} if the app itself should be used as
	 * metadata source.
	 */
	@Lob
	private URI metadataUri;

	/**
	 * Is current default app version for a given (name, type) combination. Only one default
	 * per (name, type) pair is allowed
	 */
	private Boolean defaultVersion = false;

	public AppRegistration() {
	}

	/**
	 * Construct an {@code AppRegistration} object with empty version and metadata uri
	 *
	 * @param name app name
	 * @param type app type
	 * @param uri URI for the app resource
	 */
	public AppRegistration(String name, ApplicationType type, URI uri) {
		this(name, type, "", uri, null);
	}

	/**
	 * Construct an {@code AppRegistration} object with empty version
	 *
	 * @param name app name
	 * @param type app type
	 * @param uri URI for the app resource
	 * @param metadataUri URI for the app metadata resource
	 */
	public AppRegistration(String name, ApplicationType type, URI uri, URI metadataUri) {
		this(name, type, "", uri, metadataUri);
	}

	/**
	 * Construct an {@code AppRegistration} object.
	 *
	 * @param name app name
	 * @param type app type
	 * @param version app version
	 * @param uri URI for the app resource
	 * @param metadataUri URI for the app metadata resource
	 */
	public AppRegistration(String name, ApplicationType type, String version, URI uri, URI metadataUri) {
		Assert.hasText(name, "name is required");
		Assert.notNull(type, "type is required");
		Assert.notNull(version, "version is required");
		Assert.notNull(uri, "uri is required");
		this.name = name;
		this.type = type;
		this.version = version;
		this.uri = uri;
		this.metadataUri = metadataUri;
	}

	/**
	 * @return the name of the app
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the type of the app
	 */
	public ApplicationType getType() {
		return type;
	}

	public void setType(ApplicationType type) {
		this.type = type;
	}

	/**
	 * @return the version of the app
	 */
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * @return the URI of the app
	 */
	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public URI getMetadataUri() {
		return metadataUri;
	}

	public void setMetadataUri(URI metadataUri) {
		this.metadataUri = metadataUri;
	}

	public Boolean isDefaultVersion() {
		return defaultVersion;
	}

	public void setDefaultVersion(Boolean defaultVersion) {
		this.defaultVersion = defaultVersion;
	}

	@Override
	public String toString() {
		return "AppRegistration{" + "name='" + name + '\'' + ", type='" + type + '\'' + ", version='" + version + '\''
				+ ", uri=" + uri + ", metadataUri=" + metadataUri + '}';
	}

	@Override
	public int compareTo(AppRegistration that) {
		int i = this.type.compareTo(that.type);
		if (i == 0) {
			i = this.name.compareTo(that.name);
		}
		if (i == 0) {
			i = this.version.compareTo(that.version);
		}
		return i;
	}
}
