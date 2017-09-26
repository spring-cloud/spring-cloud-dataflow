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
package org.springframework.cloud.skipper.domain;

import java.io.IOException;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.PostLoad;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.util.StringUtils;

/**
 * @author Mark Pollack
 */
@Entity
public class Release extends AbstractEntity {

	/**
	 * A short name, to associate with the release of this package.
	 */
	@NotNull
	private String name;

	private int version;

	@OneToOne(cascade = { CascadeType.ALL })
	private Info info;

	@Transient
	private Package pkg;

	@Lob
	private String pkgJsonString;

	@Transient
	private ConfigValues configValues = new ConfigValues();

	@Lob
	private String configValuesString;

	//TODO Since we store the release manifest now in the DB, we don't need the ManifestStore unless we want to also
	//     store external, e.g git.
	@Lob
	private String manifest;

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
		//TODO Do we want to store the package file at this specific moment in time?
		this.pkg.getMetadata().setPackageFileBytes(null);
		ObjectMapper mapper = new ObjectMapper();
		try {
			this.pkgJsonString = mapper.writeValueAsString(pkg);
		}
		catch (JsonProcessingException e) {
			e.printStackTrace();
		}
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

	public String getManifest() {
		return manifest;
	}

	public void setManifest(String manifest) {
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
		try {
			this.pkg = mapper.readValue(this.pkgJsonString, Package.class);
			this.configValues = new ConfigValues();
			if (this.configValuesString != null && StringUtils.hasText(configValuesString)) {
				this.configValues.setRaw(this.configValuesString);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
