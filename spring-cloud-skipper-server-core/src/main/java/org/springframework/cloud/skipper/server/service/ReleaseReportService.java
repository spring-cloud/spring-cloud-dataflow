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
package org.springframework.cloud.skipper.server.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Manifest;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.deployer.ReleaseManager;
import org.springframework.cloud.skipper.server.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.cloud.skipper.server.util.ConfigValueUtils;
import org.springframework.cloud.skipper.server.util.ManifestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Mark Pollack
 */
public class ReleaseReportService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final PackageMetadataRepository packageMetadataRepository;

	private final ReleaseRepository releaseRepository;

	private final PackageService packageService;

	private final ReleaseManager releaseManager;

	public ReleaseReportService(PackageMetadataRepository packageMetadataRepository,
			ReleaseRepository releaseRepository,
			PackageService packageService,
			ReleaseManager releaseManager) {
		this.packageMetadataRepository = packageMetadataRepository;
		this.releaseRepository = releaseRepository;
		this.packageService = packageService;
		this.releaseManager = releaseManager;
	}

	/**
	 * Merges the configuration values for the replacing release, creates the manfiest, and
	 * creates the Report for the next stage of upgrading a Release.
	 * @param upgradeRequest containing the {@link UpgradeProperties} and
	 * {@link PackageIdentifier} for the update.
	 * @return A report of what needs to change to bring the current release to the requested
	 * release
	 */
	@Transactional
	public ReleaseAnalysisReport createReport(UpgradeRequest upgradeRequest) {
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
		Release replacingRelease = createReleaseForUpgrade(packageMetadata, latestRelease.getVersion() + 1,
				upgradeProperties,
				existingRelease.getPlatformName());
		Map<String, Object> model = ConfigValueUtils.mergeConfigValues(replacingRelease.getPkg(),
				replacingRelease.getConfigValues());
		String manifestData = ManifestUtils.createManifest(replacingRelease.getPkg(), model);
		Manifest manifest = new Manifest();
		manifest.setData(manifestData);
		replacingRelease.setManifest(manifest);
		return this.releaseManager.createReport(existingRelease, replacingRelease);
	}

	private Release createReleaseForUpgrade(PackageMetadata packageMetadata, Integer newVersion,
			UpgradeProperties upgradeProperties, String platformName) {
		Assert.notNull(upgradeProperties, "Upgrade Properties can not be null");
		Package packageToInstall = this.packageService.downloadPackage(packageMetadata);
		Release release = new Release();
		release.setName(upgradeProperties.getReleaseName());
		release.setPlatformName(platformName);
		release.setConfigValues(upgradeProperties.getConfigValues());
		release.setPkg(packageToInstall);
		release.setVersion(newVersion);
		Info info = Info.createNewInfo("Upgrade install underway");
		release.setInfo(info);
		return release;
	}

}
