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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.samskivert.mustache.Mustache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.domain.DeployProperties;
import org.springframework.cloud.skipper.domain.DeployRequest;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.cloud.skipper.domain.UpdateProperties;
import org.springframework.cloud.skipper.domain.UpdateRequest;
import org.springframework.cloud.skipper.index.PackageException;
import org.springframework.cloud.skipper.repository.DeployerRepository;
import org.springframework.cloud.skipper.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.repository.ReleaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Service responsible for the lifecycle of packages and releases, deploy/undeploy a
 * package, update/rollback a release, and get status on a release.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@Service
public class ReleaseService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final PackageMetadataRepository packageMetadataRepository;

	private final ReleaseRepository releaseRepository;

	private final PackageService packageService;

	private final ReleaseManager releaseManager;

	private final DeployerRepository deployerRepository;

	@Autowired
	public ReleaseService(PackageMetadataRepository packageMetadataRepository,
			ReleaseRepository releaseRepository,
			PackageService packageService,
			ReleaseManager releaseManager,
			DeployerRepository deployerRepository) {
		this.packageMetadataRepository = packageMetadataRepository;
		this.releaseRepository = releaseRepository;
		this.packageService = packageService;
		this.releaseManager = releaseManager;
		this.deployerRepository = deployerRepository;
	}

	/**
	 * Downloads the package metadata and package zip file specified by the given Id and
	 * deploys the package on the target platform.
	 * @param id of the package
	 * @param deployProperties contains the name of the release, the platfrom to deploy to,
	 * and configuration values to replace in the package template.
	 * @return the Release object associated with this deployment
	 * @throws PackageException if the package to deploy can not be found.
	 */
	public Release deploy(String id, DeployProperties deployProperties) {
		Assert.notNull(deployProperties, "Deploy properties can not be null");
		Assert.hasText(id, "Package id can not be null");
		PackageMetadata packageMetadata = this.packageMetadataRepository.findOne(id);
		if (packageMetadata == null) {
			throw new PackageException(String.format("Package with id='%s' can not be found.", id));
		}
		return deploy(packageMetadata, deployProperties);
	}

	/**
	 * Downloads the package metadata and package zip file specified by PackageIdentifier
	 * property of the DeploymentRequest. Deploys the package on the target platform.
	 * @param deployRequest the deploymentRequest
	 * @return the Release object associated with this deployment
	 */
	public Release deploy(DeployRequest deployRequest) {
		// TODO deployRequest validation.
		PackageIdentifier packageIdentifier = deployRequest.getPackageIdentifier();
		String packageName = packageIdentifier.getPackageName();
		String packageVersion = packageIdentifier.getPackageVersion();
		PackageMetadata packageMetadata;
		if (!StringUtils.hasText(packageVersion)) {
			List<PackageMetadata> packageMetadataList = this.packageMetadataRepository.findByName(packageName);
			if (packageMetadataList.size() == 1) {
				packageMetadata = packageMetadataList.get(0);
			}
			else if (packageMetadataList == null) {
				throw new PackageException("Can not find a package named " + packageName);
			}
			else {
				// TODO find latest version
				throw new PackageException("Package name " + packageName + " is not unique.  Finding latest version " +
						" not yet implemented");
			}
		}
		else {
			packageMetadata = this.packageMetadataRepository.findByNameAndVersion(packageName, packageVersion);
			if (packageMetadata == null) {
				throw new PackageException(String.format("Can not find package '%s', version '%s'",
						packageName, packageVersion));
			}
		}
		return deploy(packageMetadata.getId(), deployRequest.getDeployProperties());
	}

	protected Release deploy(PackageMetadata packageMetadata, DeployProperties deployProperties) {
		Assert.notNull(packageMetadata, "Can't download package, PackageMetadata is a null value.");
		Release release = createInitialRelease(deployProperties, this.packageService.downloadPackage(packageMetadata));
		return deploy(release);
	}

	protected Release deploy(Release release) {
		Map<String, Object> mergedMap = ConfigValueUtils.mergeConfigValues(release.getPkg(), release.getConfigValues());
		// Render yaml resources
		String manifest = createManifest(release.getPkg(), mergedMap);
		release.setManifest(manifest);
		// Deployment
		return this.releaseManager.deploy(release);
	}

	public Release undeploy(String releaseName) {
		Assert.notNull(releaseName, "Release name must not be null");
		Release release = this.releaseRepository.findLatestRelease(releaseName);
		return this.releaseManager.undeploy(release);
	}

	public Release status(String releaseName, Integer version) {
		return status(this.releaseRepository.findByNameAndVersion(releaseName, version));
	}

	public Release status(Release release) {
		return this.releaseManager.status(release);
	}

	public Release getLatestRelease(String releaseName) {
		return this.releaseRepository.findLatestRelease(releaseName);
	}

	public Release update(UpdateRequest updateRequest) {
		UpdateProperties updateProperties = updateRequest.getUpdateProperties();
		Release oldRelease = getLatestRelease(updateProperties.getReleaseName());
		PackageIdentifier packageIdentifier = updateRequest.getPackageIdentifier();
		// todo: search multi repository
		PackageMetadata packageMetadata = this.packageMetadataRepository
				.findByNameAndVersion(packageIdentifier.getPackageName(), packageIdentifier.getPackageVersion());
		Release newRelease = createReleaseForUpdate(packageMetadata, oldRelease.getVersion() + 1, updateProperties,
				oldRelease.getPlatformName());
		Map<String, Object> model = ConfigValueUtils.mergeConfigValues(newRelease.getPkg(),
				newRelease.getConfigValues());
		String manifest = createManifest(newRelease.getPkg(), model);
		newRelease.setManifest(manifest);
		return update(oldRelease, newRelease);
	}

	public Release createReleaseForUpdate(PackageMetadata packageMetadata, Integer newVersion,
			UpdateProperties deployProperties, String platformName) {
		Assert.notNull(deployProperties, "Deploy Properties can not be null");
		Package packageToInstall = this.packageService.downloadPackage(packageMetadata);
		Release release = new Release();
		release.setName(deployProperties.getReleaseName());
		release.setPlatformName(platformName);
		release.setConfigValues(deployProperties.getConfigValues());
		release.setPkg(packageToInstall);
		release.setVersion(newVersion);
		Info info = createNewInfo("Update deploy underway");
		release.setInfo(info);
		return release;
	}

	protected Info createNewInfo(String description) {
		Info info = new Info();
		info.setFirstDeployed(new Date());
		info.setLastDeployed(new Date());
		Status status = new Status();
		status.setStatusCode(StatusCode.UNKNOWN);
		info.setStatus(status);
		info.setDescription(description);
		return info;
	}

	protected Info createNewInfo() {
		return createNewInfo("Initial deploy underway");
	}

	public Release update(Release existingRelease, Release replacingRelease) {
		Assert.notNull(existingRelease, "Existing Release must not be null");
		Assert.notNull(replacingRelease, "Replacing Release must not be null");
		Release release = this.releaseManager.deploy(replacingRelease);
		// TODO UpdateStrategy (manfiestSave, healthCheck)
		this.releaseManager.undeploy(existingRelease);
		return status(release);
	}

	/**
	 * Rollback the release name to the specified version. If the version is 0, then rollback
	 * to the previous release.
	 *
	 * @param releaseName the name of the release
	 * @param rollbackVersion the version of the release to rollback to
	 * @return the Release
	 */
	public Release rollback(final String releaseName, final int rollbackVersion) {
		Assert.notNull(releaseName, "Release name must not be null");
		Assert.isTrue(rollbackVersion >= 0,
				"Rollback version can not be less than zero.  Value = " + rollbackVersion);

		Release currentRelease = this.releaseRepository.findLatestRelease(releaseName);
		Assert.notNull(currentRelease, "Could not find release = [" + releaseName + "]");

		int rollbackVersionToUse = rollbackVersion;
		if (rollbackVersion == 0) {
			rollbackVersionToUse = currentRelease.getVersion() - 1;
		}
		Assert.isTrue(rollbackVersionToUse != 0, "Can not rollback to before version 1");

		Release releaseToRollback = this.releaseRepository.findByNameAndVersion(releaseName, rollbackVersionToUse);
		Assert.notNull(releaseToRollback, "Could not find Release to rollback to [releaseName,releaseVersion] = ["
				+ releaseName + "," + rollbackVersionToUse + "]");

		logger.info("Rolling back releaseName={}.  Current version={}, Target version={}", releaseName,
				currentRelease.getVersion(), rollbackVersionToUse);

		Release newRelease = new Release();
		newRelease.setName(releaseName);
		newRelease.setPkg(releaseToRollback.getPkg());
		newRelease.setManifest(releaseToRollback.getManifest());
		newRelease.setVersion(currentRelease.getVersion() + 1);
		newRelease.setPlatformName(releaseToRollback.getPlatformName());
		// Do not set ConfigValues since the manifest from the previous release has already
		// resolved those...
		newRelease.setInfo(createNewInfo());

		return update(currentRelease, newRelease);
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
	public String createManifest(Package packageToDeploy, Map<String, Object> model) {

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

		for (Package pkg : packageToDeploy.getDependencies()) {
			String packageName = pkg.getMetadata().getName();
			Map<String, Object> modelForDependency;
			if (model.containsKey(packageName)) {
				modelForDependency = (Map<String, Object>) model.get(pkg.getMetadata().getName());
			}
			else {
				modelForDependency = new TreeMap<>();
			}
			sb.append(createManifest(pkg, modelForDependency));
		}

		return sb.toString();
	}

	protected Release createInitialRelease(DeployProperties deployProperties, Package packageToInstall) {
		Release release = new Release();
		release.setName(deployProperties.getReleaseName());
		release.setPlatformName(deployProperties.getPlatformName());
		release.setConfigValues(deployProperties.getConfigValues());
		release.setPkg(packageToInstall);
		release.setVersion(1);
		Info info = createNewInfo();
		release.setInfo(info);
		validateInitialRelease(release);
		return release;
	}

	/**
	 * Do up front checks before deploying
	 * @param release the initial release object this data provided by the end user.
	 */
	protected void validateInitialRelease(Release release) {
		this.deployerRepository.findByNameRequired(release.getPlatformName());
	}
}
