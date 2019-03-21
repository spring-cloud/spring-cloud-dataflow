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
package org.springframework.cloud.skipper.server.deployer;

import java.util.List;

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.deployer.ReleaseDifference;
import org.springframework.util.Assert;

/**
 * Report returned from the {@link ReleaseAnalyzer} that gives the
 * {@link ReleaseDifference} that describes if there is a difference between the existing
 * release and the requested on, and if so, a description of the differences. The list of
 * application names is also provided. Deployment strategies are the consumers of this
 * report. The reports dictates what needs to change, and the strategies determine how to
 * make the change.
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class ReleaseAnalysisReport {

	private final List<String> applicationNamesToUpgrade;

	private final ReleaseDifference releaseDifference;

	private final Release existingRelease;

	private final Release replacingRelease;

	/**
	 * Create an analysis report.
	 * @param applicationNamesToUpgrade the list of application names that needs to be updates
	 * @param releaseDifference a description of the changes between the current release and
	 * the proposed release
	 * @param existingRelease the currently deployed release
	 * @param replacingRelease the release to be deployed
	 */
	public ReleaseAnalysisReport(List<String> applicationNamesToUpgrade, ReleaseDifference releaseDifference,
			Release existingRelease, Release replacingRelease) {
		Assert.notNull(applicationNamesToUpgrade, "ApplicationNamesToUpgrade can not be null.");
		Assert.notNull(releaseDifference, "ReleaseDifference can not be null.");
		Assert.notNull(existingRelease, "ExistingRelease can not be null.");
		Assert.notNull(replacingRelease, "ReplacingRelease can not be null.");
		this.applicationNamesToUpgrade = applicationNamesToUpgrade;
		this.releaseDifference = releaseDifference;
		this.existingRelease = existingRelease;
		this.replacingRelease = replacingRelease;
	}

	/**
	 * @return the names of applications to upgrade
	 */
	public List<String> getApplicationNamesToUpgrade() {
		return this.applicationNamesToUpgrade;
	}

	public ReleaseDifference getReleaseDifference() {
		return this.releaseDifference;
	}

	/**
	 * @return a textual summary of the ReleaseDifference
	 */
	public String getReleaseDifferenceSummary() {
		ReleaseDifferenceSummaryGenerator releaseDifferenceSummaryGenerator = new ReleaseDifferenceSummaryGenerator();
		return releaseDifferenceSummaryGenerator.generateSummary(existingRelease.getName(),
				existingRelease.getVersion(),
				replacingRelease.getName(),
				replacingRelease.getVersion(),
				this.releaseDifference);
	}

	public Release getExistingRelease() {
		return this.existingRelease;
	}

	public Release getReplacingRelease() {
		return this.replacingRelease;
	}
}
