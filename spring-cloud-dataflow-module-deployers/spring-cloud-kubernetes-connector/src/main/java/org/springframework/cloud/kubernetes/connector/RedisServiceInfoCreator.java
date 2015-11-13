/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.kubernetes.connector;

import io.fabric8.kubernetes.api.model.Service;

import org.springframework.cloud.service.common.RedisServiceInfo;

/**
 * A very simplistic redis service info creator that expects that the Kubernetes service
 * has been labeled with {@literal spring-cloud-service=redis}.
 *
 * @author Eric Bottard
 */
public class RedisServiceInfoCreator extends KubernetesServiceInfoCreator<RedisServiceInfo> {

	public RedisServiceInfoCreator() {
		super("redis");
	}

	@Override
	public RedisServiceInfo createServiceInfo(Service serviceData) {
		int port = serviceData.getSpec().getPorts().iterator().next().getPort();
		return new RedisServiceInfo(serviceData.getMetadata().getName(), serviceData.getSpec().getClusterIP(), port, null);
	}
}
