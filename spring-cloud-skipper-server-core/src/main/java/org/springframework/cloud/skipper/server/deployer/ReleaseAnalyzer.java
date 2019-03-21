/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.cloud.skipper.server.deployer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifest;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifestReader;
import org.springframework.cloud.skipper.domain.deployer.ApplicationManifestDifference;
import org.springframework.cloud.skipper.domain.deployer.ReleaseDifference;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Analyze the new release manifest and the previous one to determine the minimum number
 * of releases to install and delete when upgrading. This implementation currently only
 * supports detecting changes between the two packages that either a) both have a single
 * top level package template b) both have the same number of dependent packages and no
 * top level package template
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class ReleaseAnalyzer {

	private final SpringCloudDeployerApplicationManifestReader applicationManifestReader;
	private final Logger logger = LoggerFactory.getLogger(ReleaseAnalyzer.class);
	private final DelegatingResourceLoader delegatingResourceLoader;
	private ApplicationManifestDifferenceFactory applicationManifestDifferenceFactory = new ApplicationManifestDifferenceFactory();

	public ReleaseAnalyzer(SpringCloudDeployerApplicationManifestReader applicationManifestReader,
			DelegatingResourceLoader delegatingResourceLoader) {
		this.applicationManifestReader = applicationManifestReader;
		this.delegatingResourceLoader = delegatingResourceLoader;
	}

	/**
	 * Analyze the existing release and the replacing release to determine the minimal number
	 * of changes that need to be made.
	 * @param existingRelease the release that is currently deployed
	 * @param replacingRelease the proposed release to be deployed that will replace the
	 * existing release.
	 * @param isForceUpdate flag to indicate if the update is forced
	 * @param appNamesToUpdate the application names to force update
	 * @return an analysis report describing the changes to make, if any.
	 */
	public ReleaseAnalysisReport analyze(Release existingRelease, Release replacingRelease, boolean isForceUpdate,
			List<String> appNamesToUpdate) {
		// For now, assume single package with no deps or package with same number of deps
		List<? extends SpringCloudDeployerApplicationManifest> existingApplicationSpecList = this.applicationManifestReader
				.read(existingRelease.getManifest().getData());
		List<? extends SpringCloudDeployerApplicationManifest> replacingApplicationSpecList = this.applicationManifestReader
				.read(replacingRelease.getManifest().getData());
		if (existingRelease.getPkg().getDependencies().size() == replacingRelease.getPkg().getDependencies()
				.size()) {
			if (existingRelease.getPkg().getDependencies().size() == 0) {
				logger.info("Existing Package and Upgrade Package both have no dependent packages.");
				return analyzeTopLevelPackagesOnly(existingApplicationSpecList,
						replacingApplicationSpecList,
						existingRelease, replacingRelease, isForceUpdate, appNamesToUpdate);
			}
			else {
				if (existingRelease.getPkg().getTemplates().size() == 0 &&
						replacingRelease.getPkg().getTemplates().size() == 0) {
					logger.info("Existing Package and Upgrade package both have no top level templates");
					return analyzeDependentPackagesOnly(existingApplicationSpecList,
							replacingApplicationSpecList,
							existingRelease, replacingRelease, isForceUpdate, appNamesToUpdate);
				}
				else {
					throw new SkipperException(
							"Can not yet compare package with top level templates and dependencies");
				}
			}
		}
		else {
			throw new SkipperException(
					"Can not yet compare existing package and to be released packages with different sizes.");
		}
	}

	public List<String> getAllApplicationNames(Release release) {
		List<String> appNames = new ArrayList<>();
		List<? extends SpringCloudDeployerApplicationManifest> applicationSpecList = this.applicationManifestReader
				.read(release.getManifest().getData());
		if (release.getPkg().getDependencies().size() == 0) {
			appNames.add(applicationSpecList.get(0).getApplicationName());
		}
		else {
			for (SpringCloudDeployerApplicationManifest applicationManifestSpec : applicationSpecList) {
				appNames.add(applicationManifestSpec.getApplicationName());
			}
		}
		return appNames;
	}

	private ReleaseAnalysisReport analyzeDependentPackagesOnly(
			List<? extends SpringCloudDeployerApplicationManifest> existingApplicationSpecList,
			List<? extends SpringCloudDeployerApplicationManifest> replacingApplicationSpecList,
			Release existingRelease, Release replacingRelease, boolean isForceUpdate, List<String> appNamesToUpdate) {

		List<ApplicationManifestDifference> applicationManifestDifferences = new ArrayList<>();

		for (SpringCloudDeployerApplicationManifest existingApplicationManifest : existingApplicationSpecList) {
			String applicationName = existingApplicationManifest.getApplicationName();
			SpringCloudDeployerApplicationManifest matchingReplacingApplicationManifest = findMatching(
					applicationName, replacingApplicationSpecList);

			replacingResourceExistsAssertion(matchingReplacingApplicationManifest);

			ApplicationManifestDifference applicationManifestDifference = applicationManifestDifferenceFactory
					.createApplicationManifestDifference(applicationName,
							existingApplicationManifest,
							matchingReplacingApplicationManifest);
			applicationManifestDifferences.add(applicationManifestDifference);
		}

		return createReleaseAnalysisReport(existingRelease, replacingRelease, applicationManifestDifferences,
				isForceUpdate, appNamesToUpdate);

	}

	private void replacingResourceExistsAssertion(
			SpringCloudDeployerApplicationManifest matchingReplacingApplicationManifest) {
		String resourceName = matchingReplacingApplicationManifest.getSpec().getResource();
		String resourceVersion = matchingReplacingApplicationManifest.getSpec().getVersion();
		try {
			Resource resource = delegatingResourceLoader.getResource(
					AppDeploymentRequestFactory.getResourceLocation(resourceName, resourceVersion));
		}
		catch (Exception e) {
			throw new SkipperException(
					"Could not find Resource in replacing release name [" + resourceName
							+ "], version ["
							+ resourceVersion + "].",
					e);
		}
	}

	private ReleaseAnalysisReport analyzeTopLevelPackagesOnly(
			List<? extends SpringCloudDeployerApplicationManifest> existingApplicationSpecList,
			List<? extends SpringCloudDeployerApplicationManifest> replacingApplicationSpecList,
			Release existingRelease, Release replacingRelease, boolean isForceUpdate,
			List<String> appNamesToUpdate) {

		List<ApplicationManifestDifference> applicationManifestDifferences = new ArrayList<>();

		ApplicationManifestDifference applicationManifestDifference = applicationManifestDifferenceFactory
				.createApplicationManifestDifference(
						existingApplicationSpecList.get(0).getApplicationName(),
						existingApplicationSpecList.get(0),
						replacingApplicationSpecList.get(0));
		applicationManifestDifferences.add(applicationManifestDifference);

		return createReleaseAnalysisReport(existingRelease, replacingRelease, applicationManifestDifferences,
				isForceUpdate, appNamesToUpdate);
	}

	private ReleaseAnalysisReport createReleaseAnalysisReport(Release existingRelease,
			Release replacingRelease, List<ApplicationManifestDifference> applicationManifestDifferences,
			boolean isForceUpdate, List<String> appNamesToUpdate) {
		Set<String> appsToUpgrade = new LinkedHashSet<String>();
		ReleaseDifference releaseDifference = new ReleaseDifference();
		releaseDifference.setDifferences(applicationManifestDifferences);
		if (!releaseDifference.areEqual()) {
			logger.info("Differences detected between existing and replacing application manifests."
					+ "Upgrading applications = [" +
					StringUtils.collectionToCommaDelimitedString(releaseDifference.getChangedApplicationNames()) + "]");
			appsToUpgrade.addAll(releaseDifference.getChangedApplicationNames());
		}
		List<String> allApplicationNames = getAllApplicationNames(existingRelease);
		if (isForceUpdate && (appNamesToUpdate == null || appNamesToUpdate.isEmpty())) {
			appsToUpgrade.addAll(allApplicationNames);
		}
		// Set explicit app names from the client only if the update is enforced.
		if (isForceUpdate && appNamesToUpdate != null) {
			appsToUpgrade.addAll(appNamesToUpdate);
		}
		return new ReleaseAnalysisReport(new ArrayList(appsToUpgrade), releaseDifference, existingRelease,
				replacingRelease);
	}

	private SpringCloudDeployerApplicationManifest findMatching(String existingApplicationName,
			List<? extends SpringCloudDeployerApplicationManifest> replacingApplicationSpecList) {
		for (SpringCloudDeployerApplicationManifest replacingApplicationManifest : replacingApplicationSpecList) {
			if (replacingApplicationManifest.getApplicationName().equals(existingApplicationName)) {
				return replacingApplicationManifest;
			}
		}
		List<String> existingApplicationNames = replacingApplicationSpecList.stream()
				.map(SpringCloudDeployerApplicationManifest::getApplicationName)
				.collect(Collectors.toList());
		String exceptionMessage = String.format(
				"Did not find existing application name [%s] in list of replacing applications [%s].",
				existingApplicationName, StringUtils.collectionToCommaDelimitedString(existingApplicationNames));
		throw new SkipperException(exceptionMessage);
	}
}
