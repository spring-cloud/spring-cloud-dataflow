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

import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;

import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;

/**
 * @author David Turanski
 **/
public class CloudFoundryPlatformConnectionContextProvider {

	private Map<String, ConnectionContext> connectionContexts = new HashMap<>();

	private final CloudFoundryPlatformProperties platformProperties;

	public CloudFoundryPlatformConnectionContextProvider(
		CloudFoundryPlatformProperties platformProperties) {
		this.platformProperties = platformProperties;
	}

	public ConnectionContext connectionContext(String account) {
		CloudFoundryConnectionProperties connectionProperties =
			this.platformProperties.accountProperties(account).getConnection();
		this.connectionContexts.putIfAbsent(account, DefaultConnectionContext.builder()
			.apiHost(connectionProperties.getUrl().getHost())
			.skipSslValidation(connectionProperties.isSkipSslValidation())
			.build());
		return connectionContexts.get(account);
	}
}
