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

import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.domain.Release;

/**
 * Checks if the apps in the Replacing release are healthy. Health polling values are set
 * using {@link HealthCheckProperties}
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class CloudFoundryHealthCheckStep {

	private final CloudFoundryManifestApplicationDeployer cfManifestApplicationDeployer;

	public CloudFoundryHealthCheckStep(CloudFoundryManifestApplicationDeployer cfManifestApplicationDeployer) {
		this.cfManifestApplicationDeployer = cfManifestApplicationDeployer;
	}

	public boolean isHealthy(Release replacingRelease) {
		AppStatus appStatus = cfManifestApplicationDeployer.status(replacingRelease);
		if (appStatus.getState() == DeploymentState.deployed) {
			return true;
		}
		return false;
	}
}
