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
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.cloud.skipper.domain.skipperpackage.DeployProperties;
import org.springframework.cloud.skipper.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.repository.ReleaseRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@Service
public class ReleaseService {

	private final PackageMetadataRepository packageMetadataRepository;

	private final ReleaseRepository releaseRepository;

	private final PackageService packageService;

	private final ReleaseManager releaseManager;

	@Autowired
	public ReleaseService(PackageMetadataRepository packageMetadataRepository,
			ReleaseRepository releaseRepository,
			PackageService packageService,
			ReleaseManager releaseManager) {
		this.packageMetadataRepository = packageMetadataRepository;
		this.releaseRepository = releaseRepository;
		this.packageService = packageService;
		this.releaseManager = releaseManager;
	}

	/**
	 * Downloads the package metadata and package zip file specified by the given Id and
	 * deploys the package on the target platform.
	 * @param id of the package
	 * @param deployProperties contains the name of the release, the platfrom to deploy
	 * to, and configuration values to replace in the package template.
	 * @return the Release object associated with this installation
	 */
	public Release deploy(String id, DeployProperties deployProperties) {
		Assert.notNull(deployProperties, "Install Properties can not be null");
		PackageMetadata packageMetadata = this.packageMetadataRepository.findOne(id);
		this.packageService.downloadPackage(packageMetadata);
		Package packageToInstall = this.packageService.loadPackage(packageMetadata);
		Release release = createInitialRelease(deployProperties, packageToInstall);
		return deploy(release);
	}

	private Release deploy(Release release) {
		Properties model = mergeConfigValues(release.getConfigValues(), release.getPkg().getConfigValues());
		// Render yaml resources
		String manifest = createManifest(release.getPkg(), model);
		release.setManifest(manifest);
		// Deployment
		return this.releaseManager.deploy(release);
	}

	public Release undeploy(String releaseName, Integer version) {
		Assert.notNull(releaseName, "Release name must not be null");
		Release release = getRelease(releaseName, version);
		return this.releaseManager.undeploy(release);
	}

	public Release status(String releaseName, Integer version) {
		return status(getRelease(releaseName, version));
	}

	public Release status(Release release) {
		return this.releaseManager.status(release);
	}

	public Release getLatestRelease(String releaseName) {
		return this.releaseRepository.findLatestRelease(releaseName);
	}

	public Release getRelease(String releaseName, Integer version) {
		Release release;
		if (version == null) {
			release = this.releaseRepository.findLatestRelease(releaseName);
		}
		else {
			release = this.releaseRepository.findByNameAndVersion(releaseName, version);
		}
		return release;
	}

	public Release update(String packageId, DeployProperties deployProperties) {
		Release oldRelease = getLatestRelease(deployProperties.getReleaseName());
		Release newRelease = createNewRelease(packageId, oldRelease.getVersion() + 1, deployProperties);
		return update(oldRelease, newRelease);
	}

	public Release createNewRelease(String packageId, Integer newVersion, DeployProperties deployProperties) {
		Assert.notNull(deployProperties, "Deploy Properties can not be null");
		PackageMetadata packageMetadata = this.packageMetadataRepository.findOne(packageId);
		this.packageService.downloadPackage(packageMetadata);
		Package packageToInstall = this.packageService.loadPackage(packageMetadata);
		packageToInstall.getMetadata().setId(packageMetadata.getId());
		Release release = new Release();
		release.setName(deployProperties.getReleaseName());
		release.setPlatformName(deployProperties.getPlatformName());
		release.setConfigValues(deployProperties.getConfigValues());
		release.setPkg(packageToInstall);
		release.setVersion(newVersion);
		Info info = new Info();
		info.setFirstDeployed(new Date());
		info.setLastDeployed(new Date());
		Status status = new Status();
		status.setStatusCode(StatusCode.UNKNOWN);
		info.setStatus(status);
		info.setDescription("Update deploy underway");
		release.setInfo(info);
		return release;
	}

	public Release update(Release existingRelease, Release replacingRelease) {
		Assert.notNull(existingRelease, "Existing Release must not be null");
		Assert.notNull(replacingRelease, "Replacing Release must not be null");
		deploy(replacingRelease);
		this.releaseManager.undeploy(existingRelease);
		return replacingRelease;
	}

	public Release rollback(String releaseName, int rollbackVersion) {
		Assert.notNull(releaseName, "Release name must not be null");
		Release releaseToRollback = getRelease(releaseName, rollbackVersion);
		Release currentRelease = getLatestRelease(releaseName);
		update(currentRelease, releaseToRollback);
		return releaseToRollback;
	}

	/**
	 * Iterate overall the template files, replacing placeholders with model values. One
	 * string is returned that contain all the YAML of multiple files using YAML file
	 * delimiter.
	 * @param packageToDeploy The top level package that contains all templates where
	 * placeholders are to be replaced
	 * @param model The placeholder values.
	 * @return A YAML string containing all the templates with replaced values.
	 */
	public String createManifest(Package packageToDeploy, Properties model) {

		// Aggregate all valid manifests into one big doc.
		StringBuilder sb = new StringBuilder();
		// Top level templates.
		List<Template> templates = packageToDeploy.getTemplates();
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
	 * Merge the properties, derived from YAML format, contained in
	 * commandLineConfigValues and templateConfigValue, giving preference to
	 * commandLineConfigValues. Assumes that the YAML is stored as "raw" data in the
	 * ConfigValues object. If the "raw" data is empty or null, an empty property object
	 * is returned.
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
	 * Return a Properties object given a String that contains YAML. The Properties
	 * created by this factory have nested paths for hierarchical objects. All exposed
	 * values are of type {@code String}</b> for access through the common
	 * {@link Properties#getProperty} method. See YamlPropertiesFactoryBean for more
	 * information.
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

	private Release createInitialRelease(DeployProperties deployProperties, Package packageToInstall) {
		Release release = new Release();
		release.setName(deployProperties.getReleaseName());
		release.setPlatformName(deployProperties.getPlatformName());
		release.setConfigValues(deployProperties.getConfigValues());
		release.setPkg(packageToInstall);
		release.setVersion(1);
		Info info = new Info();
		info.setFirstDeployed(new Date());
		info.setLastDeployed(new Date());
		Status status = new Status();
		status.setStatusCode(StatusCode.UNKNOWN);
		info.setStatus(status);
		info.setDescription("Initial deploy underway");
		release.setInfo(info);
		return release;
	}
}
