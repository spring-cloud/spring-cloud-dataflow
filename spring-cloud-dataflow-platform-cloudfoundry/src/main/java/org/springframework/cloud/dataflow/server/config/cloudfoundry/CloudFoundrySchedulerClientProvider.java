/*
 * Copyright 2019-2021 the original author or authors.
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

import io.pivotal.reactor.scheduler.ReactorSchedulerClient;
import io.pivotal.scheduler.SchedulerClient;
import reactor.core.publisher.Mono;


/**
 * @author David Turanski
 **/
public class CloudFoundrySchedulerClientProvider {

	private final CloudFoundryPlatformConnectionContextProvider connectionContextProvider;

	private final CloudFoundryPlatformTokenProvider platformTokenProvider;

	private final CloudFoundryPlatformProperties platformProperties;

	public CloudFoundrySchedulerClientProvider(
		CloudFoundryPlatformConnectionContextProvider connectionContextProvider,
		CloudFoundryPlatformTokenProvider platformTokenProvider,
		CloudFoundryPlatformProperties platformProperties) {


		this.connectionContextProvider = connectionContextProvider;
		this.platformTokenProvider = platformTokenProvider;
		this.platformProperties = platformProperties;
	}

	public SchedulerClient cloudFoundrySchedulerClient(String account) {
		return ReactorSchedulerClient.builder()
				.connectionContext(connectionContextProvider.connectionContext(account))
				.tokenProvider(platformTokenProvider.tokenProvider(account))
				.root(Mono.just(platformProperties.getAccounts().get(account).getDeployment().getSchedulerUrl()))
				.build();
	}

}
