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
package org.springframework.cloud.skipper.deployer.cloudfoundry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.deployer.ApplicationManifestDifference;
import org.springframework.cloud.skipper.domain.deployer.ReleaseDifference;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.support.PropertiesDiff;
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
public class CloudFoundryReleaseAnalyzer {

	private final Logger logger = LoggerFactory.getLogger(CloudFoundryReleaseAnalyzer.class);

	private final CloudFoundryManifestApplicationDeployer cfManifestApplicationDeployer;

	public CloudFoundryReleaseAnalyzer(CloudFoundryManifestApplicationDeployer cfManifestApplicationDeployer) {
		this.cfManifestApplicationDeployer = cfManifestApplicationDeployer;
	}

	/**
	 * Analyze the existing release and the replacing release to determine the minimal number
	 * of changes that need to be made.
	 * @param existingRelease the release that is currently deployed
	 * @param replacingRelease the proposed release to be deployed that will replace the
	 * existing release.
	 * @param isForceUpdate flag to indicate if the update is forced
	 * @return an analysis report describing the changes to make, if any.
	 */
	public ReleaseAnalysisReport analyze(Release existingRelease, Release replacingRelease, boolean isForceUpdate) {
		List<ApplicationManifestDifference> applicationManifestDifferences = new ArrayList<>();
		ApplicationManifest existingApplicationManifest = this.cfManifestApplicationDeployer
				.getCFApplicationManifest(existingRelease);
		ApplicationManifest replacingApplicationManifest = this.cfManifestApplicationDeployer
				.getCFApplicationManifest(replacingRelease);
		if (!existingApplicationManifest.equals(replacingApplicationManifest)) {
			Map<String, String> existingMap = CloudFoundryApplicationManifestUtils
					.getCFManifestMap(existingApplicationManifest);
			Map<String, String> replacingMap = CloudFoundryApplicationManifestUtils
					.getCFManifestMap(replacingApplicationManifest);
			PropertiesDiff emptyPropertiesDiff = PropertiesDiff.builder().build();
			PropertiesDiff propertiesDiff = PropertiesDiff.builder().left(existingMap).right(replacingMap).build();
			ApplicationManifestDifference applicationManifestDifference = new ApplicationManifestDifference(
					existingApplicationManifest.getName(),
					emptyPropertiesDiff, emptyPropertiesDiff, emptyPropertiesDiff, propertiesDiff, emptyPropertiesDiff);
			applicationManifestDifferences.add(applicationManifestDifference);
		}
		return createReleaseAnalysisReport(existingRelease, replacingRelease, applicationManifestDifferences,
				Arrays.asList(existingApplicationManifest.getName()), isForceUpdate);
	}

	private ReleaseAnalysisReport createReleaseAnalysisReport(Release existingRelease,
			Release replacingRelease,
			List<ApplicationManifestDifference> applicationManifestDifferences, List<String> allApplicationNames,
			boolean isForceUpdate) {
		Set<String> appsToUpgrade = new LinkedHashSet<String>();
		ReleaseDifference releaseDifference = new ReleaseDifference();
		releaseDifference.setDifferences(applicationManifestDifferences);
		if (!releaseDifference.areEqual()) {
			logger.info("Differences detected between existing and replacing application manifests."
					+ "Upgrading applications = [" +
					StringUtils.collectionToCommaDelimitedString(releaseDifference.getChangedApplicationNames()) + "]");
			appsToUpgrade.addAll(releaseDifference.getChangedApplicationNames());
		}
		if (isForceUpdate) {
			appsToUpgrade.addAll(allApplicationNames);
		}
		return new ReleaseAnalysisReport(new ArrayList(appsToUpgrade), releaseDifference, existingRelease,
				replacingRelease);
	}
}
