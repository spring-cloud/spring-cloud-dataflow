/*
 * Copyright 2017-2022 the original author or authors.
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
package org.springframework.cloud.skipper.server.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.deployer.spi.app.ActuatorOperations;
import org.springframework.cloud.deployer.spi.local.LocalActuatorTemplate;
import org.springframework.cloud.deployer.spi.local.LocalAppDeployer;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Platform;
import org.springframework.cloud.skipper.server.deployer.metadata.DeployerConfigurationMetadataResolver;
import org.springframework.cloud.skipper.server.repository.map.DeployerRepository;
import org.springframework.cloud.skipper.server.service.DeployerInitializationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SkipperServerPlatformConfiguration {

	@Bean
	public DeployerConfigurationMetadataResolver deployerConfigurationMetadataResolver(
			SkipperServerProperties skipperServerProperties) {
		return new DeployerConfigurationMetadataResolver(skipperServerProperties.getDeployerProperties());
	}

	@Bean
	public DeployerInitializationService deployerInitializationService(DeployerRepository deployerRepository,
			List<Platform> platforms, DeployerConfigurationMetadataResolver resolver) {
		return new DeployerInitializationService(deployerRepository, platforms, resolver);
	}

	@Profile("local")
	@Configuration
	static class LocalPlatformConfiguration {
		@Bean
		public Platform localDeployers(LocalPlatformProperties localPlatformProperties,
				RestTemplate actuatorRestTemplate) {
			List<Deployer> deployers = new ArrayList<>();
			Map<String, LocalDeployerProperties> localDeployerPropertiesMap = localPlatformProperties.getAccounts();
			if (localDeployerPropertiesMap.isEmpty()) {
				localDeployerPropertiesMap.put("default", new LocalDeployerProperties());
			}
			for (Map.Entry<String, LocalDeployerProperties> entry : localDeployerPropertiesMap
					.entrySet()) {
				LocalAppDeployer localAppDeployer = new LocalAppDeployer(entry.getValue());
				ActuatorOperations actuatorOperations =
						new LocalActuatorTemplate(actuatorRestTemplate, localAppDeployer,
								entry.getValue().getAppAdmin());
				Deployer deployer = new Deployer(entry.getKey(), "local", localAppDeployer, actuatorOperations);
				deployer.setDescription(prettyPrintLocalDeployerProperties(entry.getValue()));
				deployers.add(deployer);
			}

			return new Platform("Local", deployers);
		}

		@Bean
		@ConditionalOnMissingBean
		RestTemplate actuatorRestTemplate() {
			return new RestTemplate();
		}

		private String prettyPrintLocalDeployerProperties(LocalDeployerProperties localDeployerProperties) {
			StringBuilder builder = new StringBuilder();
			if (localDeployerProperties.getJavaOpts() != null) {
				builder.append("JavaOpts = [" + localDeployerProperties.getJavaOpts() + "], ");
			}
			builder.append("ShutdownTimeout = [" + localDeployerProperties.getShutdownTimeout() + "], ");
			builder.append("EnvVarsToInherit = ["
					+ StringUtils.arrayToCommaDelimitedString(localDeployerProperties.getEnvVarsToInherit()) + "], ");
			builder.append("JavaCmd = [" + localDeployerProperties.getJavaCmd() + "], ");
			builder.append("WorkingDirectoriesRoot = [" + localDeployerProperties.getWorkingDirectoriesRoot() + "], ");
			builder.append("DeleteFilesOnExit = [" + localDeployerProperties.isDeleteFilesOnExit() + "]");
			return builder.toString();
		}
	}
}
