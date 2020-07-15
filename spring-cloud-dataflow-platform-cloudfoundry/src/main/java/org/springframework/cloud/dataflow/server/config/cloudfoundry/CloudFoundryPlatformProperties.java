/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config.cloudfoundry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.AbstractPlatformProperties;
import org.springframework.cloud.dataflow.server.config.cloudfoundry.CloudFoundryPlatformProperties.CloudFoundryProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.deployer.spi.scheduler.cloudfoundry.CloudFoundrySchedulerProperties;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Donovan Muller
 * @author David Turanski
 *
 * @since 2.0
 */
@ConfigurationProperties("spring.cloud.dataflow.task.platform.cloudfoundry")
public class CloudFoundryPlatformProperties extends AbstractPlatformProperties<CloudFoundryProperties> {

	public static class CloudFoundryProperties {

		private CloudFoundryConnectionProperties connection;

		private CloudFoundryDeploymentProperties deployment;

		private CloudFoundrySchedulerProperties scheduler;

		public CloudFoundryConnectionProperties getConnection() {
			return connection;
		}

		public void setConnection(CloudFoundryConnectionProperties connection) {
			this.connection = connection;
		}

		public CloudFoundryDeploymentProperties getDeployment() {
			return deployment;
		}

		public void setDeployment(CloudFoundryDeploymentProperties deployment) {
			this.deployment = deployment;
		}

		public CloudFoundrySchedulerProperties getScheduler() {
			return scheduler;
		}

		public void setScheduler(CloudFoundrySchedulerProperties scheduler) {
			this.scheduler = scheduler;
		}
	}
}
