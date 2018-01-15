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
package org.springframework.cloud.skipper.server.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.deployer.spi.local.LocalAppDeployer;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Platform;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;
import org.springframework.cloud.skipper.server.service.DeployerInitializationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class SkipperServerPlatformConfiguration {

	@Bean
	public DeployerInitializationService deployerInitializationService(
			DeployerRepository deployerRepository,
			List<Platform> platforms) {
		return new DeployerInitializationService(deployerRepository, platforms);
	}

	@Bean
	@ConditionalOnProperty(value = "spring.cloud.skipper.server.enableLocalPlatform", matchIfMissing = true)
	public Platform localDeployers(LocalPlatformProperties localPlatformProperties) {
		List<Deployer> deployers = new ArrayList<>();
		Map<String, LocalDeployerProperties> localDeployerPropertiesMap = localPlatformProperties.getAccounts();
		if (localDeployerPropertiesMap.isEmpty()) {
			localDeployerPropertiesMap.put("default", new LocalDeployerProperties());
		}
		for (Map.Entry<String, LocalDeployerProperties> entry : localDeployerPropertiesMap
				.entrySet()) {
			LocalAppDeployer localAppDeployer = new LocalAppDeployer(entry.getValue());
			Deployer deployer = new Deployer(entry.getKey(), "local", localAppDeployer);
			deployer.setDescription(prettyPrintLocalDeployerProperties(entry.getValue()));
			deployers.add(deployer);
		}

		return new Platform("Local", deployers);
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
