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
package org.springframework.cloud.skipper.server.deployer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifest;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifestReader;
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

	public ReleaseAnalyzer(SpringCloudDeployerApplicationManifestReader applicationManifestReader) {
		this.applicationManifestReader = applicationManifestReader;
	}

	/**
	 * Analyze the existing release and the replacing release to determine the minimal number
	 * of changes that need to be made.
	 * @param existingRelease the release that is currently deployed
	 * @param replacingRelease the proposed release to be deployed that will replace the
	 * existing release.
	 * @return an analysis report describing the changes to make, if any.
	 */
	public ReleaseAnalysisReport analyze(Release existingRelease, Release replacingRelease) {

		// For now, assume single package with no deps or package with same number of deps
		List<? extends SpringCloudDeployerApplicationManifest> existingApplicationSpecList = this.applicationManifestReader
				.read(existingRelease
						.getManifest());
		List<? extends SpringCloudDeployerApplicationManifest> replacingApplicationSpecList = this.applicationManifestReader
				.read(replacingRelease
						.getManifest());

		if (existingRelease.getPkg().getDependencies().size() == replacingRelease.getPkg().getDependencies().size()) {
			if (existingRelease.getPkg().getDependencies().size() == 0) {
				logger.info("Existing Package and Upgrade Package both have no dependent packages.");
				return analyzeTopLevelPackagesOnly(existingApplicationSpecList,
						replacingApplicationSpecList,
						existingRelease, replacingRelease);
			}
			else {
				if (existingRelease.getPkg().getTemplates().size() == 0 &&
						replacingRelease.getPkg().getTemplates().size() == 0) {
					logger.info("Existing Package and Upgrade package both have no top level templates");
					return analyzeDependentPackagesOnly(existingApplicationSpecList,
							replacingApplicationSpecList,
							existingRelease, replacingRelease);
				}
				else {
					throw new SkipperException("Can not yet compare package with top level templates and dependencies");
				}
			}
		}
		else {
			throw new SkipperException(
					"Can not yet compare existing package and to be released packages with different sizes.");
		}
	}

	private ReleaseAnalysisReport analyzeDependentPackagesOnly(
			List<? extends SpringCloudDeployerApplicationManifest> existingApplicationSpecList,
			List<? extends SpringCloudDeployerApplicationManifest> replacingApplicationSpecList,
			Release existingRelease, Release replacingRelease) {
		List<String> appsToDelete = new ArrayList<>();
		StringBuilder diffMessagesBuilder = new StringBuilder();
		for (SpringCloudDeployerApplicationManifest existingApplicationManifest : existingApplicationSpecList) {
			SpringCloudDeployerApplicationManifest matchingReplacingApplicationManifest = findMatching(
					existingApplicationManifest.getApplicationName(),
					replacingApplicationSpecList);
			ReleaseDifference difference = compare(existingApplicationManifest,
					matchingReplacingApplicationManifest);
			if (!difference.areEqual()) {
				logger.info(
						"Dependent package difference found for " + existingApplicationManifest.getApplicationName());
				appsToDelete.add(existingApplicationManifest.getApplicationName());
				diffMessagesBuilder.append(difference.getDifferenceSummary());
				diffMessagesBuilder.append("\n");
			}
		}
		if (appsToDelete.size() != 0) {
			ReleaseDifference releaseDifference = new ReleaseDifference(false, diffMessagesBuilder.toString());
			return new ReleaseAnalysisReport(appsToDelete, releaseDifference, existingRelease, replacingRelease);
		}
		else {
			ReleaseDifference releaseDifference = new ReleaseDifference(true);
			return new ReleaseAnalysisReport(new ArrayList<>(), releaseDifference, existingRelease, replacingRelease);
		}
	}

	private ReleaseAnalysisReport analyzeTopLevelPackagesOnly(
			List<? extends SpringCloudDeployerApplicationManifest> existingApplicationSpecList,
			List<? extends SpringCloudDeployerApplicationManifest> replacingApplicationSpecList,
			Release existingRelease, Release replacingRelease) {
		ReleaseDifference difference = compare(existingApplicationSpecList.get(0),
				replacingApplicationSpecList.get(0));
		List<String> appsToDelete = new ArrayList<>();
		if (!difference.areEqual()) {
			logger.info(
					"Differences detected, upgrading app " + existingApplicationSpecList.get(0).getApplicationName());
			appsToDelete.add(existingApplicationSpecList.get(0).getApplicationName().trim());
		}
		return new ReleaseAnalysisReport(appsToDelete, difference, existingRelease, replacingRelease);
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

	private ReleaseDifference compare(SpringCloudDeployerApplicationManifest existingApplicationManifest,
			SpringCloudDeployerApplicationManifest replacingApplicationManifest) {

		// Fail fast for now...

		String existingResource = existingApplicationManifest.getSpec().getResource().trim();
		String replacingResource = replacingApplicationManifest.getSpec().getResource().trim();
		String existingResourceVersion = existingApplicationManifest.getSpec().getVersion();
		String replacingResourceVersion = replacingApplicationManifest.getSpec().getVersion();

		String existingResourceWithVersion = ResourceUtils.getResourceLocation(existingResource,
				existingResourceVersion);
		String replacingResourceWithVersion = ResourceUtils.getResourceLocation(replacingResource,
				replacingResourceVersion);
		if (!existingResourceWithVersion.equals(replacingResourceWithVersion)) {
			String difference = String.format("Existing resource =[%s], Replacing name=[%s]", existingResource,
					replacingResource);
			return new ReleaseDifference(false, difference);
		}

		// Compare SpringCloudDeployerApplicationManifest Properties
		if (existingApplicationManifest instanceof SpringCloudDeployerApplicationManifest
				&& replacingApplicationManifest instanceof SpringCloudDeployerApplicationManifest) {
			Map<String, String> existingApplicationProperties = ((SpringCloudDeployerApplicationManifest) existingApplicationManifest)
					.getSpec().getApplicationProperties();
			Map<String, String> replacingApplicationProperties = ((SpringCloudDeployerApplicationManifest) replacingApplicationManifest)
					.getSpec().getApplicationProperties();

			if (existingApplicationProperties == null) {
				existingApplicationProperties = new TreeMap<>();
			}
			if (replacingApplicationProperties == null) {
				replacingApplicationProperties = new TreeMap<>();
			}
			MapDifference<String, String> applicationPropertiesDifference = Maps
					.difference(existingApplicationProperties,
							replacingApplicationProperties);

			if (!applicationPropertiesDifference.areEqual()) {
				return getReleaseDifferenceForAppProps(applicationPropertiesDifference);
			}

			// Compare Deployment Properties
			Map<String, String> existingDeploymentProperties = ((SpringCloudDeployerApplicationManifest) existingApplicationManifest)
					.getSpec().getDeploymentProperties();
			Map<String, String> replacingDeploymentProperties = ((SpringCloudDeployerApplicationManifest) replacingApplicationManifest)
					.getSpec().getDeploymentProperties();

			if (existingDeploymentProperties == null) {
				existingDeploymentProperties = new TreeMap<>();
			}
			if (replacingDeploymentProperties == null) {
				replacingDeploymentProperties = new TreeMap<>();
			}
			// exclude deployer count from computing the difference
			existingDeploymentProperties.remove(AppDeployerReleaseManager.SPRING_CLOUD_DEPLOYER_COUNT);
			replacingDeploymentProperties.remove(AppDeployerReleaseManager.SPRING_CLOUD_DEPLOYER_COUNT);

			MapDifference<String, String> deploymentPropertiesDifference = Maps.difference(existingDeploymentProperties,
					replacingDeploymentProperties);

			if (!deploymentPropertiesDifference.areEqual()) {
				return getReleaseDifferenceForDeploymentProps(deploymentPropertiesDifference);
			}
		}

		return new ReleaseDifference(true);

	}

	private ReleaseDifference getReleaseDifferenceForDeploymentProps(
			MapDifference<String, String> deploymentPropertiesDifference) {
		StringBuffer differenceBuilder = new StringBuffer();
		Joiner.MapJoiner mapJoiner = Joiner.on(",").withKeyValueSeparator("=");
		differenceBuilder.append("\nDeployement Properties Differences (existing, replacing)\n");
		differenceBuilder.append("==================================\n");
		if (!deploymentPropertiesDifference.entriesDiffering().isEmpty()) {
			differenceBuilder.append(mapJoiner.join(deploymentPropertiesDifference.entriesDiffering()) + "\n");
		}
		if (!deploymentPropertiesDifference.entriesOnlyOnLeft().isEmpty()) {
			differenceBuilder.append("Only in existing app\n");
			differenceBuilder.append("========================\n");
			differenceBuilder.append(mapJoiner.join(deploymentPropertiesDifference.entriesOnlyOnLeft()) + "\n");
		}
		if (!deploymentPropertiesDifference.entriesOnlyOnRight().isEmpty()) {
			differenceBuilder.append("Only in replacing app\n");
			differenceBuilder.append("=====================\n");
			differenceBuilder.append(mapJoiner.join(deploymentPropertiesDifference.entriesOnlyOnRight()) + "\n");
		}
		if (!deploymentPropertiesDifference.entriesInCommon().isEmpty()) {
			differenceBuilder.append("Common Properites\n");
			differenceBuilder.append("=================\n");
			differenceBuilder.append(mapJoiner.join(deploymentPropertiesDifference.entriesInCommon()) + "\n");
		}
		return new ReleaseDifference(false, differenceBuilder.toString());
	}

	private ReleaseDifference getReleaseDifferenceForAppProps(
			MapDifference<String, String> applicationPropertiesDifference) {
		StringBuffer differenceBuilder = new StringBuffer();
		Joiner.MapJoiner mapJoiner = Joiner.on(",").withKeyValueSeparator("=");
		differenceBuilder.append("\nSpringCloudDeployerApplicationManifest Properties Differences\n");
		differenceBuilder.append("==================================\n");
		if (!applicationPropertiesDifference.entriesDiffering().isEmpty()) {
			differenceBuilder.append(mapJoiner.join(applicationPropertiesDifference.entriesDiffering()) + "\n");
		}
		if (!applicationPropertiesDifference.entriesOnlyOnLeft().isEmpty()) {
			differenceBuilder.append("Only in existing app\n");
			differenceBuilder.append("========================\n");
			differenceBuilder.append(mapJoiner.join(applicationPropertiesDifference.entriesOnlyOnLeft()) + "\n");
		}
		if (!applicationPropertiesDifference.entriesOnlyOnRight().isEmpty()) {
			differenceBuilder.append("Only in replacing app\n");
			differenceBuilder.append("=====================\n");
			differenceBuilder.append(mapJoiner.join(applicationPropertiesDifference.entriesOnlyOnRight()) + "\n");
		}
		if (!applicationPropertiesDifference.entriesInCommon().isEmpty()) {
			differenceBuilder.append("Common Properties\n");
			differenceBuilder.append("=================\n");
			differenceBuilder.append(mapJoiner.join(applicationPropertiesDifference.entriesInCommon()) + "\n");
		}
		return new ReleaseDifference(false, differenceBuilder.toString());
	}
}
