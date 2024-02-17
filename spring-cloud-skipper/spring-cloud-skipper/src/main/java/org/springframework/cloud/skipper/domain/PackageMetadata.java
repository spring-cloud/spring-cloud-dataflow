/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import org.springframework.cloud.dataflow.common.persistence.type.DatabaseAwareLobUserType;


/**
 * Metadata for the Package.
 *
 * @author Mark Pollack
 * @author Gunnar Hillert
 */
@Entity
@Table(name = "SkipperPackageMetadata", indexes = @Index(name = "idx_pkg_name", columnList = "name"))
public class PackageMetadata extends AbstractEntity {

	/**
	 * The Package Index spec version this file is based on.
	 * we enforce apiVersion during package creation.
	 */
	@NotNull
	private String apiVersion;

	/**
	 * Indicates the origin of the repository (free form text).
	 */
	private String origin;

	/**
	 * The repository ID this Package belongs to.
	 */
	@NotNull
	private Long repositoryId;

	/**
	 * The repository name this Package belongs to.
	 */
	@NotNull
	private String repositoryName;

	/**
	 * What type of package system is being used.
	 */
	@NotNull
	private String kind;

	/**
	 * The name of the package
	 */
	@NotNull
	private String name;

	/**
	 * The display name of the package
	 */
	private String displayName;

	/**
	 * The version of the package
	 */
	@NotNull
	private String version;

	/**
	 * Location to source code for this package.
	 */
	@Lob
	@Type(DatabaseAwareLobUserType.class)
	private String packageSourceUrl;

	/**
	 * The home page of the package
	 */
	@Lob
	@Type(DatabaseAwareLobUserType.class)
	private String packageHomeUrl;

	/**
	 * Package file.
	 */
	@JsonIgnore
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	// need to keep fk key max 30 char thus not using fk_package_metadata_package_file
	@JoinColumn(name = "packagefile_id", foreignKey = @ForeignKey(name = "fk_package_metadata_pfile"))
	private PackageFile packageFile;

	/**
	 * A comma separated list of tags to use for searching
	 */
	@Lob
	@Type(DatabaseAwareLobUserType.class)
	private String tags;

	/**
	 * Who is maintaining this package
	 */
	private String maintainer;

	/**
	 * Brief description of the package. The packages README.md will contain more information.
	 */
	@Lob
	@Type(DatabaseAwareLobUserType.class)
	private String description;

	/**
	 * Hash of package binary that will be downloaded using SHA256 hash algorithm.
	 */
	private String sha256;

	/**
	 * Url location of a icon.
	 */
	@Lob
	@Type(DatabaseAwareLobUserType.class)
	private String iconUrl;

	public PackageMetadata() {
	}

	public String getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
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

	@JsonIgnore
	public PackageFile getPackageFile() {
		return packageFile;
	}

	@JsonIgnore
	public void setPackageFile(PackageFile packageFile) {
		this.packageFile = packageFile;
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

	public Long getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(Long repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String getRepositoryName() {
		return repositoryName;
	}

	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PackageMetadata)) {
			return false;
		}

		PackageMetadata that = (PackageMetadata) o;

		if (repositoryId != null ? !repositoryId.equals(that.repositoryId) : that.repositoryId != null) {
			return false;
		}
		if (name != null ? !name.equals(that.name) : that.name != null) {
			return false;
		}
		return version != null ? version.equals(that.version) : that.version == null;
	}

	@Override
	public int hashCode() {
		int result = repositoryId != null ? repositoryId.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (version != null ? version.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "PackageMetadata{" +
				"id='" + getId() + '\'' +
				", apiVersion='" + apiVersion + '\'' +
				", origin='" + origin + '\'' +
				", repositoryName='" + repositoryName + '\'' +
				", kind='" + kind + '\'' +
				", name='" + name + '\'' +
				", version='" + version + '\'' +
				", packageSourceUrl='" + packageSourceUrl + '\'' +
				", packageHomeUrl='" + packageHomeUrl + '\'' +
				", tags='" + tags + '\'' +
				", maintainer='" + maintainer + '\'' +
				", description='" + description + '\'' +
				", sha256='" + sha256 + '\'' +
				", iconUrl='" + iconUrl + '\'' +
				'}';
	}
}
