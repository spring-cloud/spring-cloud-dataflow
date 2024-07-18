/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.cloud.skipper.server.service;

import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Manifest;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.deployer.ReleaseManager;
import org.springframework.cloud.skipper.server.deployer.ReleaseManagerFactory;
import org.springframework.cloud.skipper.server.repository.jpa.PackageMetadataRepository;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.cloud.skipper.server.util.ConfigValueUtils;
import org.springframework.cloud.skipper.server.util.ManifestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.representer.Representer;

/**
 * @author Mark Pollack
 * @author Chris Bono
 */
public class ReleaseReportService {

	private final PackageMetadataRepository packageMetadataRepository;

	private final ReleaseRepository releaseRepository;

	private final PackageService packageService;

	private final ReleaseManagerFactory releaseManagerFactory;

	public ReleaseReportService(PackageMetadataRepository packageMetadataRepository,
			ReleaseRepository releaseRepository,
			PackageService packageService,
			ReleaseManagerFactory releaseManagerFactory) {
		this.packageMetadataRepository = packageMetadataRepository;
		this.releaseRepository = releaseRepository;
		this.packageService = packageService;
		this.releaseManagerFactory = releaseManagerFactory;
	}

	/**
	 * Merges the configuration values for the replacing release, creates the manfiest, and
	 * creates the Report for the next stage of upgrading a Release.
	 *
	 * @param upgradeRequest containing the {@link UpgradeProperties} and
	 * {@link PackageIdentifier} for the update.
	 * @param rollbackRequest containing the rollback request if available
	 * @param initial the flag indicating this is initial report creation
	 * @return A report of what needs to change to bring the current release to the requested
	 * release
	 */
	@Transactional
	public ReleaseAnalysisReport createReport(UpgradeRequest upgradeRequest, RollbackRequest rollbackRequest,
			boolean initial) {
		Assert.notNull(upgradeRequest.getUpgradeProperties(), "UpgradeProperties can not be null");
		Assert.notNull(upgradeRequest.getPackageIdentifier(), "PackageIdentifier can not be null");
		UpgradeProperties upgradeProperties = upgradeRequest.getUpgradeProperties();
		Release existingRelease = this.releaseRepository.findLatestReleaseForUpdate(upgradeProperties.getReleaseName());
		Release latestRelease = this.releaseRepository.findLatestRelease(upgradeProperties.getReleaseName());
		PackageIdentifier packageIdentifier = upgradeRequest.getPackageIdentifier();
		PackageMetadata packageMetadata = this.packageMetadataRepository.findByNameAndOptionalVersionRequired(
				packageIdentifier.getPackageName(),
				packageIdentifier
						.getPackageVersion());

		// if we're about to save new release during this report, create
		// or restore replacing one.
		Release tempReplacingRelease = null;
		if (initial) {
			tempReplacingRelease = createReleaseForUpgrade(packageMetadata, latestRelease.getVersion() + 1,
					upgradeProperties, existingRelease.getPlatformName(), rollbackRequest);
		}
		else {
			tempReplacingRelease = this.releaseRepository.findByNameAndVersion(
					upgradeRequest.getUpgradeProperties().getReleaseName(), latestRelease.getVersion());
		}
		// Carry over customized config values from replacing release so updates are additive with property changes.
		Release replacingRelease = updateReplacingReleaseConfigValues(latestRelease, tempReplacingRelease);

		Map<String, Object> mergedReplacingReleaseModel = ConfigValueUtils.mergeConfigValues(replacingRelease.getPkg(),
				replacingRelease.getConfigValues());

		String manifestData = ManifestUtils.createManifest(replacingRelease.getPkg(), mergedReplacingReleaseModel);
		Manifest manifest = new Manifest();
		manifest.setData(manifestData);
		replacingRelease.setManifest(manifest);

		// TODO: should check both releases
		String kind = ManifestUtils.resolveKind(existingRelease.getManifest().getData());
		ReleaseManager releaseManager = this.releaseManagerFactory.getReleaseManager(kind);
		return releaseManager.createReport(existingRelease, replacingRelease, initial, upgradeRequest.isForce(),
				upgradeRequest.getAppNames());
	}

	private Release updateReplacingReleaseConfigValues(Release targetRelease, Release replacingRelease) {
		Map<String, Object> targetConfigValueMap = getConfigValuesAsMap(targetRelease.getConfigValues());
		Map<String, Object> replacingRelaseConfigValueMap = getConfigValuesAsMap(replacingRelease.getConfigValues());
		if (targetConfigValueMap != null && replacingRelaseConfigValueMap != null) {
			ConfigValueUtils.merge(targetConfigValueMap, replacingRelaseConfigValueMap);
			DumperOptions dumperOptions = new DumperOptions();
			dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
			dumperOptions.setPrettyFlow(true);
			Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(dumperOptions), dumperOptions);
			ConfigValues mergedConfigValues = new ConfigValues();
			mergedConfigValues.setRaw(yaml.dump(targetConfigValueMap));
			replacingRelease.setConfigValues(mergedConfigValues);
		}
		return replacingRelease;
	}

	private Map<String, Object> getConfigValuesAsMap(ConfigValues configValues) {
		LoaderOptions options = new LoaderOptions();
		Yaml yaml = new Yaml(new SafeConstructor(options));
		if (StringUtils.hasText(configValues.getRaw())) {
			Object data = yaml.load(configValues.getRaw());
			if (data instanceof Map) {
				return (Map<String, Object>) yaml.load(configValues.getRaw());
			}
			else {
				throw new SkipperException("Was expecting override values to produce a Map, instead got class = " +
						data.getClass() + "overrideValues.getRaw() = " + configValues.getRaw());
			}
		}
		return null;
	}

	private Release createReleaseForUpgrade(PackageMetadata packageMetadata, Integer newVersion,
			UpgradeProperties upgradeProperties, String platformName, RollbackRequest rollbackRequest) {
		Assert.notNull(upgradeProperties, "Upgrade Properties can not be null");
		Package packageToInstall = this.packageService.downloadPackage(packageMetadata);
		Release release = new Release();
		release.setName(upgradeProperties.getReleaseName());
		release.setPlatformName(platformName);
		release.setConfigValues(upgradeProperties.getConfigValues());
		release.setPkg(packageToInstall);
		release.setVersion(newVersion);
		// we simply differentiate between upgrade/rollback if we know there is a rollback request
		Info info = Info
				.createNewInfo(rollbackRequest == null ? "Upgrade install underway" : "Rollback install underway");
		release.setInfo(info);
		return release;
	}

}
