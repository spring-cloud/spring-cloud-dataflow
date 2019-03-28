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

package org.springframework.cloud.dataflow.server.config.kubernetes;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.cloud.deployer.spi.kubernetes.KubernetesClientFactory;

/**
 * @author David Turanski
 **/
public class KubernetesPlatformClientProvider {

	private final KubernetesPlatformProperties platformProperties;
	private final Map<String, KubernetesClient> clients = new HashMap<>();

	public KubernetesPlatformClientProvider(KubernetesPlatformProperties platformProperties) {
		this.platformProperties = platformProperties;
	}

	public KubernetesClient kubenertesClient(String account) {
		clients.putIfAbsent(account,
			KubernetesClientFactory.getKubernetesClient(this.platformProperties.accountProperties(account)));
		return clients.get(account);
	}

}
