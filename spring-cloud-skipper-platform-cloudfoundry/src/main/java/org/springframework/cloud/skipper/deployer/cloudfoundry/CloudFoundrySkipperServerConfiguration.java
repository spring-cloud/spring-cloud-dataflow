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
package org.springframework.cloud.skipper.deployer.cloudfoundry;

import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.skipper.domain.CloudFoundryApplicationManifestReader;
import org.springframework.cloud.skipper.server.deployer.ReleaseManagerFactory;
import org.springframework.cloud.skipper.server.repository.jpa.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for CF related server features for CF manifest support.
 *
 * @author Janne Valkealahti
 *
 */
@Configuration
public class CloudFoundrySkipperServerConfiguration {

	@Bean
	public CloudFoundryReleaseManager cloudFoundryReleaseManager(ReleaseRepository releaseRepository,
			AppDeployerDataRepository appDeployerDataRepository,
			CloudFoundryReleaseAnalyzer cloudFoundryReleaseAnalyzer,
			PlatformCloudFoundryOperations platformCloudFoundryOperations,
			CloudFoundryManifestApplicationDeployer cfManifestApplicationDeployer) {
		return new CloudFoundryReleaseManager(releaseRepository, appDeployerDataRepository, cloudFoundryReleaseAnalyzer,
				platformCloudFoundryOperations, cfManifestApplicationDeployer);
	}

	@Bean
	public CloudFoundryApplicationManifestReader cfApplicationManifestReader() {
		return new CloudFoundryApplicationManifestReader();
	}

	@Bean
	public CloudFoundryHandleHealthCheckStep cloudFoundryHandleHealthCheckStep(ReleaseRepository releaseRepository,
			AppDeployerDataRepository appDeployerDataRepository, CloudFoundryDeleteStep deleteStep,
			ReleaseManagerFactory releaseManagerFactory) {
		return new CloudFoundryHandleHealthCheckStep(releaseRepository, appDeployerDataRepository, deleteStep,
				releaseManagerFactory);
	}

	@Bean
	public CloudFoundrySimpleRedBlackUpgradeStrategy cloudFoundrySimpleRedBlackUpgradeStrategy(
			CloudFoundryHealthCheckStep healthCheckStep, CloudFoundryHandleHealthCheckStep handleHealthCheckStep,
			CloudFoundryDeployAppStep deployAppStep) {
		return new CloudFoundrySimpleRedBlackUpgradeStrategy(healthCheckStep, handleHealthCheckStep, deployAppStep);
	}

	@Bean
	public CloudFoundryHealthCheckStep cloudFoundryHealthCheckStep(
			CloudFoundryManifestApplicationDeployer cfManifestApplicationDeployer) {
		return new CloudFoundryHealthCheckStep(cfManifestApplicationDeployer);
	}

	@Bean
	public CloudFoundryDeleteStep cloudFoundryDeleteStep(ReleaseRepository releaseRepository,
			PlatformCloudFoundryOperations platformCloudFoundryOperations) {
		return new CloudFoundryDeleteStep(releaseRepository, platformCloudFoundryOperations);
	}

	@Bean
	public CloudFoundryDeployAppStep cloudFoundryDeployAppStep(AppDeployerDataRepository appDeployerDataRepository,
			ReleaseRepository releaseRepository, PlatformCloudFoundryOperations platformCloudFoundryOperations,
			CloudFoundryManifestApplicationDeployer cfManifestApplicationDeployer) {
		return new CloudFoundryDeployAppStep(appDeployerDataRepository, releaseRepository,
				platformCloudFoundryOperations, cfManifestApplicationDeployer);
	}

	@Bean
	public PlatformCloudFoundryOperations platformCloudFoundryOperations(
			CloudFoundryPlatformProperties cloudFoundryPlatformProperties) {
		return new PlatformCloudFoundryOperations(cloudFoundryPlatformProperties);
	}

	@Bean
	public CloudFoundryManifestApplicationDeployer cfApplicationDeployer(CloudFoundryApplicationManifestReader cfApplicationManifestReader,
			PlatformCloudFoundryOperations platformCloudFoundryOperations,
			DelegatingResourceLoader delegatingResourceLoader) {
		return new CloudFoundryManifestApplicationDeployer(cfApplicationManifestReader, platformCloudFoundryOperations,
				delegatingResourceLoader);
	}

	@Bean
	public CloudFoundryReleaseAnalyzer cloudFoundryReleaseAnalyzer(
			CloudFoundryManifestApplicationDeployer cfManifestApplicationDeployer) {
		return new CloudFoundryReleaseAnalyzer(cfManifestApplicationDeployer);
	}
}
