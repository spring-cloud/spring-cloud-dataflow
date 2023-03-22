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

import java.util.Arrays;
import java.util.Collection;

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.SkipperManifestKind;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;

/**
 * A simple approach to deploying a new application. All the new apps are deployed and a
 * health check is done to on the new apps. If they are healthy, the old apps are deleted.
 * Executes on it's own thread since it is a long lived operation. Delegates to
 * {@link DeployAppStep}, {@link HealthCheckStep} and {@link HandleHealthCheckStep}
 * @author Mark Pollack
 */
public class SimpleRedBlackUpgradeStrategy implements UpgradeStrategy {

	private final HealthCheckStep healthCheckStep;

	private final HandleHealthCheckStep handleHealthCheckStep;

	private final DeployAppStep deployAppStep;

	public SimpleRedBlackUpgradeStrategy(HealthCheckStep healthCheckStep,
			HandleHealthCheckStep handleHealthCheckStep,
			DeployAppStep deployAppStep) {
		this.healthCheckStep = healthCheckStep;
		this.handleHealthCheckStep = handleHealthCheckStep;
		this.deployAppStep = deployAppStep;
	}

	@Override
	public Collection<String> getSupportedKinds() {
		return Arrays.asList(SkipperManifestKind.SpringBootApp.name(),
				SkipperManifestKind.SpringCloudDeployerApplication.name());
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
