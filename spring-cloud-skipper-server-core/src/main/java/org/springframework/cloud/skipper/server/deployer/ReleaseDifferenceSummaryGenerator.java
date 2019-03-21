/*
 * Copyright 2018 the original author or authors.
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

import org.springframework.cloud.skipper.domain.deployer.ApplicationManifestDifference;
import org.springframework.cloud.skipper.domain.deployer.ReleaseDifference;

/**
 * Generate a summary description of the {@link ReleaseDifference} as String.
 * @author Mark Pollack
 */
public class ReleaseDifferenceSummaryGenerator {

	/**
	 * Generate a textual summary of the ReleaseDifference
	 * @param releaseDifference the difference between two releases
	 * @return the textual summary
	 */
	public String generateSummary(String existingReleaseName, int existingReleaseVersion,
			String replacingReleseName, int replacingReleaseVersion,
			ReleaseDifference releaseDifference) {
		StringBuilder stringBuilder = new StringBuilder();
		if (releaseDifference.getDifferences() != null) {
			stringBuilder.append("Release Difference Summary between existing release ");
			stringBuilder.append("[name: " + existingReleaseName + "version: " + existingReleaseVersion + "]");
			stringBuilder.append(" and replacing release ");
			stringBuilder
					.append("[name: " + replacingReleseName + "version: " + replacingReleaseVersion + "]\n");
			ApplicationManifestDifferenceSummaryGenerator applicationManifestDifferenceSummaryGenerator = new ApplicationManifestDifferenceSummaryGenerator();
			for (ApplicationManifestDifference applicationManifestDifference : releaseDifference.getDifferences()) {
				if (!applicationManifestDifference.areEqual()) {
					stringBuilder.append(applicationManifestDifferenceSummaryGenerator
							.generateSummary(applicationManifestDifference));
				}
			}
		}
		return stringBuilder.toString();
	}
}
