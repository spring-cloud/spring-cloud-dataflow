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

package org.springframework.cloud.dataflow.configuration.metadata.container.authorization;

import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImage;
import org.springframework.cloud.dataflow.configuration.metadata.container.RegistryConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * @author James Wynn
 */
public class AnonymousRegistryAuthorizer implements RegistryAuthorizer {

	@Override
	public RegistryConfiguration.AuthorizationType getType() {
		return RegistryConfiguration.AuthorizationType.anonymous;
	}

	@Override
	public HttpHeaders getAuthorizationHeaders(ContainerImage containerImage,
			RegistryConfiguration registryConfiguration) {

		Assert.isTrue(registryConfiguration.getAuthorizationType() == this.getType(),
				"Incorrect type: " + registryConfiguration.getAuthorizationType());

		final HttpHeaders headers = new HttpHeaders();
		return headers;
	}
}
