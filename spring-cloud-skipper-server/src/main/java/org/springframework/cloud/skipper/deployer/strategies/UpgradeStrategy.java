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
package org.springframework.cloud.skipper.deployer.strategies;

import org.springframework.cloud.skipper.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.domain.Release;

/**
 * A strategy interface for how to deploy a new release on top of an existing release
 * driven by the information comparing the two releases.
 * @author Mark Pollack
 */
public interface UpgradeStrategy {

	/**
	 * Given the two releases, the one currently deployed, the 'existingRelease' and to one to
	 * be deployed, the 'replacingRelace', use the information in the analysis report to
	 * deploy the apps in the new release and delete the apps in the old.
	 * @param existingRelease the apps currently deployed
	 * @param replacingRelease the apps to be deployed
	 * @param releaseAnalysisReport report to guide the strategy on what apps to replace.
	 * @return the replacingRelease, now deployed.
	 */
	Release upgrade(Release existingRelease, Release replacingRelease, ReleaseAnalysisReport releaseAnalysisReport);

}
