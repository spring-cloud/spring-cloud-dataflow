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

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;

public class PlatformCloudFoundryOperations {

	private static final Logger logger = LoggerFactory.getLogger(PlatformCloudFoundryOperations.class);

	private final CloudFoundryPlatformProperties cloudFoundryPlatformProperties;
	private final Map<String, CloudFoundryOperations> cache = new HashMap<>();

	public PlatformCloudFoundryOperations(CloudFoundryPlatformProperties cloudFoundryPlatformProperties) {
		this.cloudFoundryPlatformProperties = cloudFoundryPlatformProperties;
	}

	public synchronized CloudFoundryOperations getCloudFoundryOperations(String platformName) {
		CloudFoundryOperations operations = cache.get(platformName);
		if (operations == null) {
			logger.debug("No existing CloudFoundryOperations for platformName {}, creating new");
			operations = buildCloudFoundryOperations(platformName);
			cache.put(platformName, operations);
		}
		else {
			logger.trace("Using existing CloudFoundryOperations for platformName {}");
		}
		return operations;
	}

	private CloudFoundryOperations buildCloudFoundryOperations(String platformName) {
		CloudFoundryPlatformProperties.CloudFoundryProperties cloudFoundryProperties = this.cloudFoundryPlatformProperties
				.getAccounts()
				.get(platformName);
		CloudFoundryConnectionProperties connectionProperties = cloudFoundryProperties.getConnection();
		ConnectionContext connectionContext = DefaultConnectionContext.builder()
				.apiHost(connectionProperties.getUrl().getHost())
				.skipSslValidation(connectionProperties.isSkipSslValidation())
				.build();
		TokenProvider tokenProvider = PasswordGrantTokenProvider.builder()
				.username(connectionProperties.getUsername())
				.password(connectionProperties.getPassword()).build();
		CloudFoundryClient cloudFoundryClient = ReactorCloudFoundryClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
		return DefaultCloudFoundryOperations
				.builder().cloudFoundryClient(cloudFoundryClient)
				.organization(connectionProperties.getOrg())
				.space(connectionProperties.getSpace()).build();
	}
}
