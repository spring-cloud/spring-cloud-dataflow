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
package org.springframework.cloud.skipper.server.deployer.strategies;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.deployer.AppDeploymentRequestFactory;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.repository.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.scheduling.annotation.Async;

import static org.springframework.cloud.skipper.server.config.SkipperServerConfiguration.SKIPPER_THREAD_POOL_EXECUTOR;

/**
 * A simple approach to deploying a new application. All the new apps are deployed and a
 * health check is done to on the new apps. If they are healthy, the old apps are deleted.
 * Executes on it's own thread since it is a long lived operation. Delegates to
 * {@link DeployAppStep}, {@link HealthCheckStep} and {@link HandleHealthCheckStep}
 * @author Mark Pollack
 */
public class SimpleRedBlackUpgradeStrategy implements UpgradeStrategy {

	private final Logger logger = LoggerFactory.getLogger(SimpleRedBlackUpgradeStrategy.class);

	private final DeployerRepository deployerRepository;

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final AppDeploymentRequestFactory appDeploymentRequestFactory;

	private final HealthCheckStep healthCheckStep;

	private final HandleHealthCheckStep handleHealthCheckStep;

	private final ReleaseRepository releaseRepository;

	private final DeployAppStep deployAppStep;

	public SimpleRedBlackUpgradeStrategy(DeployerRepository deployerRepository,
			AppDeployerDataRepository appDeployerDataRepository,
			AppDeploymentRequestFactory appDeploymentRequestFactory,
			HealthCheckStep healthCheckStep,
			HandleHealthCheckStep handleHealthCheckStep,
			DeployAppStep deployAppStep,
			ReleaseRepository releaseRepository) {
		this.deployerRepository = deployerRepository;
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.appDeploymentRequestFactory = appDeploymentRequestFactory;
		this.healthCheckStep = healthCheckStep;
		this.handleHealthCheckStep = handleHealthCheckStep;
		this.deployAppStep = deployAppStep;
		this.releaseRepository = releaseRepository;
	}

	@Override
	@Async(SKIPPER_THREAD_POOL_EXECUTOR)
	public Release upgrade(Release existingRelease, Release replacingRelease,
			ReleaseAnalysisReport releaseAnalysisReport) {

		List<String> applicationNamesToUpgrade = this.deployAppStep.deployApps(existingRelease, replacingRelease,
				releaseAnalysisReport);
		if (!replacingRelease.getInfo().getStatus().getStatusCode().equals(StatusCode.FAILED)) {
			boolean isHealthy = this.healthCheckStep.waitForNewAppsToDeploy(replacingRelease);
			this.handleHealthCheckStep.handleHealthCheck(isHealthy, existingRelease,
					applicationNamesToUpgrade, replacingRelease);
		}
		return replacingRelease;
	}

}
