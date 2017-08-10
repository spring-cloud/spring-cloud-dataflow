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
package org.springframework.cloud.skipper.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.samskivert.mustache.Mustache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.cloud.skipper.domain.*;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.repository.PackageMetadataRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Mark Pollack
 */
@Service
public class ReleaseService {

	private PackageMetadataRepository packageMetadataRepository;

	private PackageService packageService;

	private ReleaseManager releaseManager;

	@Autowired
	public ReleaseService(PackageMetadataRepository packageMetadataRepository,
			PackageService packageService,
			ReleaseManager releaseManager) {
		this.packageMetadataRepository = packageMetadataRepository;
		this.packageService = packageService;
		this.releaseManager = releaseManager;
	}

	/**
	 * Downloads the package metadata and package zip file specified by the given Id and installs the
	 * package on the target platform.
	 * @param id of the package
	 * @param installProperties contains the name of the release, the platfrom to deploy to, and configuration values
	 *                          to replace in the package template.
	 * @return the Release object associated with this installation
	 */
	public Release install(String id, InstallProperties installProperties) {
		Assert.notNull(installProperties,"Install Properties can not be null");
		PackageMetadata packageMetadata = packageMetadataRepository.findOne(id);
		packageService.downloadPackage(packageMetadata);
		Package packageToInstall = packageService.loadPackage(packageMetadata);
		Release release = createInitialRelease(installProperties, packageToInstall);

		return install(release);
	}

	private Release install(Release release) {
		Properties model = mergeConfigValues(release.getConfigValues(), release.getPkg().getConfigValues());
		// Render yaml resources
		String manifest = createManifest(release.getPkg(), model);
		release.setManifest(manifest);

		// Deployment
		releaseManager.deploy(release);

		return release;

	}

	/**
	 * Iterate overall the template files, replacing placeholders with model values. One
	 * string is returned that contain all the YAML of multiple files using YAML file
	 * delimiter.
	 * @param packageToInstall The top level package that contains all templates where
	 * placeholders are to be replaced
	 * @param model The placeholder values.
	 * @return A YAML string containing all the templates with replaced values.
	 */
	public String createManifest(Package packageToInstall, Properties model) {

		// Aggregate all valid manifests into one big doc.
		StringBuilder sb = new StringBuilder();
		// Top level templates.
		List<Template> templates = packageToInstall.getTemplates();
		if (templates != null) {
			for (Template template : templates) {
				String templateAsString = new String(template.getData());
				com.samskivert.mustache.Template mustacheTemplate = Mustache.compiler().compile(templateAsString);
				sb.append("\n---\n# Source: " + template.getName() + "\n");
				sb.append(mustacheTemplate.execute(model));
			}
		}

		// TODO package dependencies

		return sb.toString();
	}

	/**
	 * Merge the properties, derived from YAML format, contained in commandLineConfigValues
	 * and templateConfigValue, giving preference to commandLineConfigValues. Assumes that the
	 * YAML is stored as "raw" data in the ConfigValues object. If the "raw" data is empty or
	 * null, an empty property object is returned.
	 *
	 * @param templateConfigValue YAML data defined in the template.yaml file
	 * @param commandLineConfigValues YAML data passed at the application runtime
	 * @return A Properties object that is the merger of both ConfigValue objects,
	 * commandLineConfig values override values in templateConfig.
	 */
	public Properties mergeConfigValues(ConfigValues templateConfigValue, ConfigValues commandLineConfigValues) {
		Properties commandLineOverrideProperties;
		if (commandLineConfigValues == null) {
			commandLineOverrideProperties = new Properties();
		}
		else {
			commandLineOverrideProperties = convertYamlToProperties(commandLineConfigValues.getRaw());
		}
		Properties templateVariables;
		if (templateConfigValue == null) {
			templateVariables = new Properties();
		}
		else {
			templateVariables = convertYamlToProperties(templateConfigValue.getRaw());
		}

		Properties model = new Properties();
		model.putAll(templateVariables);
		model.putAll(commandLineOverrideProperties);
		return model;
	}

	/**
	 * Return a Properties object given a String that contains YAML. The Properties created by
	 * this factory have nested paths for hierarchical objects. All exposed values are of type
	 * {@code String}</b> for access through the common {@link Properties#getProperty} method.
	 * See YamlPropertiesFactoryBean for more information.
	 * @param yamlString String that contains YAML
	 * @return properties object containing contents of YAML file
	 */
	public Properties convertYamlToProperties(String yamlString) {
		YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();

		Properties values;
		if (StringUtils.hasText(yamlString)) {
			try (InputStream is = new ByteArrayInputStream(yamlString.getBytes())) {
				yaml.setResources(new InputStreamResource(is));
				yaml.afterPropertiesSet();
				values = yaml.getObject();
			}
			catch (Exception e) {
				throw new IllegalArgumentException(
						"Could not convert YAML to properties object from string " + yamlString, e);
			}
		}
		else {
			values = new Properties();
		}
		return values;

	}

	private Release createInitialRelease(InstallProperties installProperties, Package packageToInstall) {
		Release release = new Release();
		release.setName(installProperties.getReleaseName());
		release.setPlatformName(installProperties.getPlatformName());
		release.setConfigValues(installProperties.getConfigValues());
		release.setPkg(packageToInstall);

		release.setVersion(1);
		Info info = new Info();
		info.setFirstDeployed(new Date());
		info.setLastDeployed(new Date());
		Status status = new Status();
		status.setStatusCode(StatusCode.UNKNOWN);
		info.setStatus(status);
		info.setDescription("Initial install underway");
		release.setInfo(info);
		return release;
	}
}
