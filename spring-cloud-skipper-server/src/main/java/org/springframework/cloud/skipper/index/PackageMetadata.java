/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.index;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

/**
 * @author Mark Pollack
 */
@Entity
public class PackageMetadata {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	/**
	 * The Package Index spec version this file is based on. Required
	 */
	@NotNull
	private String apiVersion;

	/**
	 * The repository ID this Package Index file belongs to. Required
	 */
	@NotNull
	private String origin;

	/**
	 * What type of package system is being used.
	 */
	private String kind;

	/**
	 * The name of the package
	 */
	private String name;

	/**
	 * The version of the package
	 */
	private String version;

	/**
	 * The version of the application in the package (assuming package maps to one app)
	 */
	private String appVersion;

	/**
	 * Location to source code for this package.
	 */
	private String packageSourceUrl;

	/**
	 * The home page of the package
	 */
	private String packageHomeUrl;

	/**
	 * A comma separated list of tags to use for searching
	 */
	private String tags;

	/**
	 * Who is maintaining this package
	 */
	private String maintainer;

	/**
	 * Brief description of the package. The packages README.md will contain more information.
	 * TODO - decide format.
	 */
	private String description;

	/**
	 * Hash of package binary that will be donwloaded using SHA256 hash algorithm.
	 */
	private String sha256;

	/**
	 * Url location of a icon. TODO: size specification
	 */
	private String iconUrl;

	public PackageMetadata() {
	}

	public String getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	public String getPackageSourceUrl() {
		return packageSourceUrl;
	}

	public void setPackageSourceUrl(String packageSourceUrl) {
		this.packageSourceUrl = packageSourceUrl;
	}

	public String getPackageHomeUrl() {
		return packageHomeUrl;
	}

	public void setPackageHomeUrl(String packageHomeUrl) {
		this.packageHomeUrl = packageHomeUrl;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getMaintainer() {
		return maintainer;
	}

	public void setMaintainer(String maintainer) {
		this.maintainer = maintainer;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSha256() {
		return sha256;
	}

	public void setSha256(String sha256) {
		this.sha256 = sha256;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}
}
