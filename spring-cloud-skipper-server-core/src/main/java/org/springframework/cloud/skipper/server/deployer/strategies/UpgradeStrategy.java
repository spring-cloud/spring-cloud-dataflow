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
package org.springframework.cloud.skipper.server.deployer.strategies;

import java.util.Collection;

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;

/**
 * A strategy interface for how to deploy a new release on top of an existing release
 * driven by the information comparing the two releases.
 *
 * @author Mark Pollack
 */
public interface UpgradeStrategy {

	Collection<String> getSupportedKinds();

	void deployApps(Release existingRelease, Release replacingRelease, ReleaseAnalysisReport releaseAnalysisReport);

	boolean checkStatus(Release replacingRelease);

	void accept(Release existingRelease, Release replacingRelease, ReleaseAnalysisReport releaseAnalysisReport,
			boolean rollback);

	void cancel(Release existingRelease, Release replacingRelease, ReleaseAnalysisReport releaseAnalysisReport,
			Long timeout, boolean cancel, boolean rollback);

}
