/*
 * Copyright 2017-2019 the original author or authors.
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

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import org.springframework.cloud.dataflow.common.persistence.type.DatabaseAwareLobUserType;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.util.StringUtils;

/**
 * The entity corresponds to Release of the package.
 *
 * @author Mark Pollack
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@Entity
@Table(name = "SkipperRelease", indexes = @Index(name = "idx_rel_name", columnList = "name"))
public class Release extends AbstractEntity {

	/**
	 * A short name, to associate with the release of this package.
	 */
	@NotNull
	private String name;

	private int version;

	@OneToOne(cascade = { CascadeType.ALL })
	@JoinColumn(foreignKey = @ForeignKey(name = "fk_release_info"))
	private Info info;

	@Transient
	private Package pkg;

	@JsonIgnore
	private Long packageMetadataId;

	@JsonIgnore
	private Long repositoryId;

	@Lob
	@Type(DatabaseAwareLobUserType.class)
	private String pkgJsonString;

	@Transient
	private ConfigValues configValues = new ConfigValues();

	@Lob
	@Type(DatabaseAwareLobUserType.class)
	private String configValuesString;

	@OneToOne(cascade = { CascadeType.ALL })
	@JoinColumn(foreignKey = @ForeignKey(name = "fk_release_manifest"))
	private Manifest manifest;

	private String platformName;

	public Release() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public Info getInfo() {
		return info;
	}

	public void setInfo(Info info) {
		this.info = info;
	}

	public Package getPkg() {
		return pkg;
	}

	public void setPkg(Package pkg) {
		this.pkg = pkg;
		this.packageMetadataId = pkg.getMetadata().getId();
		this.repositoryId = pkg.getMetadata().getRepositoryId();
		ObjectMapper mapper = new ObjectMapper();
		try {
			// Note that @JsonIgnore is on the package file byte array field.
			this.pkgJsonString = mapper.writeValueAsString(pkg);
		}
		catch (JsonProcessingException e) {
			throw new SkipperException("Error processing pkg json string", e);
		}
	}

	public Long getPackageMetadataId() {
		return packageMetadataId;
	}

	public void setPackageMetadataId(Long packageMetadataId) {
		this.packageMetadataId = packageMetadataId;
	}

	public Long getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(Long repositoryId) {
		this.repositoryId = repositoryId;
	}

	public ConfigValues getConfigValues() {
		if (configValues == null) {
			return new ConfigValues();
		}
		else {
			return configValues;
		}
	}

	public void setConfigValues(ConfigValues configValues) {
		this.configValues = configValues;
		if (configValues != null && StringUtils.hasText(configValues.getRaw())) {
			this.configValuesString = configValues.getRaw();
		}
	}

	public Manifest getManifest() {
		return manifest;
	}

	public void setManifest(Manifest manifest) {
		this.manifest = manifest;
	}

	public String getPlatformName() {
		return platformName;
	}

	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	@PostLoad
	public void afterLoad() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			this.pkg = mapper.readValue(this.pkgJsonString, Package.class);
			this.configValues = new ConfigValues();
			if (this.configValuesString != null && StringUtils.hasText(configValuesString)) {
				this.configValues.setRaw(this.configValuesString);
			}
		}
		catch (IOException e) {
			throw new SkipperException("Error processing config values", e);
		}
	}
}
