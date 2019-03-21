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
package org.springframework.cloud.skipper.deployer.cloudfoundry;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.SkipperManifestKind;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategy;

/**
 * A simple approach to deploying a new application. All the new apps are
 * deployed and a health check is done to on the new apps. If they are healthy,
 * the old apps are deleted. Executes on it's own thread since it is a long
 * lived operation. Delegates to {@link CloudFoundryDeployAppStep},
 * {@link CloudFoundryHealthCheckStep} and
 * {@link CloudFoundryHandleHealthCheckStep}
 *
 * @author Mark Pollack
 * @author Janne Valkealahti
 */
public class CloudFoundrySimpleRedBlackUpgradeStrategy implements UpgradeStrategy {

	private final CloudFoundryHealthCheckStep healthCheckStep;

	private final CloudFoundryHandleHealthCheckStep handleHealthCheckStep;

	private final CloudFoundryDeployAppStep deployAppStep;

	public CloudFoundrySimpleRedBlackUpgradeStrategy(CloudFoundryHealthCheckStep healthCheckStep,
			CloudFoundryHandleHealthCheckStep handleHealthCheckStep,
			CloudFoundryDeployAppStep deployAppStep) {
		this.healthCheckStep = healthCheckStep;
		this.handleHealthCheckStep = handleHealthCheckStep;
		this.deployAppStep = deployAppStep;
	}

	@Override
	public Collection<String> getSupportedKinds() {
		return Arrays.asList(SkipperManifestKind.CloudFoundryApplication.name());
	}

	@Override
	public void deployApps(Release existingRelease, Release replacingRelease, ReleaseAnalysisReport releaseAnalysisReport) {
		this.deployAppStep.deployApps(existingRelease, replacingRelease, releaseAnalysisReport);
	}

	@Override
	public boolean checkStatus(Release replacingRelease) {
		return this.healthCheckStep.isHealthy(replacingRelease);
	}

	@Override
	public void accept(Release existingRelease, Release replacingRelease,
			ReleaseAnalysisReport releaseAnalysisReport, boolean rollback) {
		this.handleHealthCheckStep.handleHealthCheck(true, existingRelease,
				releaseAnalysisReport.getApplicationNamesToUpgrade(), replacingRelease, null, false, rollback);
	}

	@Override
	public void cancel(Release existingRelease, Release replacingRelease, ReleaseAnalysisReport releaseAnalysisReport,
			Long timeout, boolean cancel, boolean rollback) {
		this.handleHealthCheckStep.handleHealthCheck(false, existingRelease,
				releaseAnalysisReport.getApplicationNamesToUpgrade(), replacingRelease, timeout, cancel, rollback);
	}
}
