/*
 * Copyright 2019 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.logcache.v1.LogCacheClient;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.logcache.v1.ReactorLogCacheClient;

/**
 * @author David Turanski
 * @author Chris Bono
 */
public class CloudFoundryPlatformClientProvider {

	private final CloudFoundryPlatformProperties platformProperties;

	private final CloudFoundryPlatformConnectionContextProvider connectionContextProvider;

	private final CloudFoundryPlatformTokenProvider platformTokenProvider;

	private final Map<String, CloudFoundryClient> cloudFoundryClients = new HashMap<>();

	private final Map<String, LogCacheClient> cloudFoundryLogClients = new HashMap<>();

	CloudFoundryPlatformClientProvider(
		CloudFoundryPlatformProperties platformProperties,
		CloudFoundryPlatformConnectionContextProvider connectionContextProvider,
		CloudFoundryPlatformTokenProvider platformTokenProvider) {
		this.platformProperties = platformProperties;
		this.connectionContextProvider = connectionContextProvider;
		this.platformTokenProvider = platformTokenProvider;
	}

	public CloudFoundryClient cloudFoundryClient(String account){
		return cloudFoundryClients.computeIfAbsent(account, (__) -> ReactorCloudFoundryClient.builder()
				.connectionContext(connectionContextProvider.connectionContext(account))
				.tokenProvider(platformTokenProvider.tokenProvider(account))
				.build());
	}

	public LogCacheClient logCacheClient(String account) {
		return cloudFoundryLogClients.computeIfAbsent(account, (__) -> ReactorLogCacheClient.builder()
				.connectionContext(connectionContextProvider.connectionContext(account))
				.tokenProvider(platformTokenProvider.tokenProvider(account))
				.build());
	}
}
