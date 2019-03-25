/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config.cloudfoundry;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;

/**
 * @author David Turanski
 **/
public class CloudFoundryPlatformClientProvider {

	private final CloudFoundryPlatformProperties platformProperties;

	private final CloudFoundryPlatformConnectionContextProvider connectionContextProvider;

	private final CloudFoundryPlatformTokenProvider platformTokenProvider;

	private final Map<String, CloudFoundryClient> cloudFoundryClients = new HashMap<>();

	CloudFoundryPlatformClientProvider(
		CloudFoundryPlatformProperties platformProperties,
		CloudFoundryPlatformConnectionContextProvider connectionContextProvider,
		CloudFoundryPlatformTokenProvider platformTokenProvider) {
		this.platformProperties = platformProperties;
		this.connectionContextProvider = connectionContextProvider;
		this.platformTokenProvider = platformTokenProvider;
	}

	public CloudFoundryClient cloudFoundryClient(String account){
			cloudFoundryClients.putIfAbsent(account, ReactorCloudFoundryClient.builder()
				.connectionContext(connectionContextProvider.connectionContext(account))
				.tokenProvider(platformTokenProvider.tokenProvider(account))
				.build());
			return cloudFoundryClients.get(account);
	}
}
