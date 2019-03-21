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

import java.util.ArrayList;
import java.util.List;

/**
 * Package is the installation unit that contains metadata, dependencies, configuration values and related attributes.
 *
 * @author Mark Pollack
 */
public class Package {

	// Metadata describing the package
	private PackageMetadata metadata;

	// The templates for this package
	private List<Template> templates = new ArrayList<>();

	// The packages that this package depends upon
	private List<Package> dependencies = new ArrayList<>();

	// The configuration data that this package depends on
	private ConfigValues configValues;

	// Miscellaneous files in a package, e.g. README, LICENSE, etc.
	private List<FileHolder> fileHolders = new ArrayList<>();

	public Package() {
	}

	public PackageMetadata getMetadata() {
		return metadata;
	}

	public void setMetadata(PackageMetadata metadata) {
		this.metadata = metadata;
	}

	public List<Template> getTemplates() {
		return templates;
	}

	public void setTemplates(List<Template> templates) {
		this.templates = templates;
	}

	public List<Package> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<Package> dependencies) {
		this.dependencies = dependencies;
	}

	public ConfigValues getConfigValues() {
		return configValues;
	}

	public void setConfigValues(ConfigValues configValues) {
		this.configValues = configValues;
	}

	public List<FileHolder> getFileHolders() {
		return fileHolders;
	}

	public void setFileHolders(List<FileHolder> fileHolders) {
		this.fileHolders = fileHolders;
	}
}
