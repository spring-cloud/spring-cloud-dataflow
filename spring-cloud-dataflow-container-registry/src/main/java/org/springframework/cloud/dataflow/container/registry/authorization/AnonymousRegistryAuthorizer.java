/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.container.registry.authorization;

import java.util.Map;

import org.springframework.cloud.dataflow.container.registry.ContainerImage;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * @author James Wynn
 */
public class AnonymousRegistryAuthorizer implements RegistryAuthorizer {

	@Override
	public ContainerRegistryConfiguration.AuthorizationType getType() {
		return ContainerRegistryConfiguration.AuthorizationType.anonymous;
	}

	@Override
	public HttpHeaders getAuthorizationHeaders(ContainerRegistryConfiguration registryConfiguration,
			Map<String, String> configProperties) {

		Assert.isTrue(registryConfiguration.getAuthorizationType() == this.getType(),
				"Incorrect type: " + registryConfiguration.getAuthorizationType());

		return new HttpHeaders();
	}

	@Override
	public HttpHeaders getAuthorizationHeaders(ContainerImage containerImage,
			ContainerRegistryConfiguration registryConfiguration) {

		return getAuthorizationHeaders(registryConfiguration, null);
	}


}
