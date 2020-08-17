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

/**
 * Different Container Registry providers may enforce different mechanisms and policies to access the registries.
 * Specific implementations of {@link RegistryAuthorizer} would allow to obtain the right credentials for each supported
 * registry provider. One {@link RegistryAuthorizer} is provided per
 * {@link ContainerRegistryConfiguration.AuthorizationType}
 *
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 */
public interface RegistryAuthorizer {

	/**
	 * @return Returns the type of Registry servers that this authorizer supports.
	 */
	ContainerRegistryConfiguration.AuthorizationType getType();

	/**
	 * @param containerImage Container Image for which authorization is required.
	 * @param registryConfiguration configuration such as credentials and additional information required to obtain the
	 *                              authorized headers.
	 * @return Returns HTTP headers, configured with authorization credentials or tokens that would allow access
	 * the target Registry.
	 */
	HttpHeaders getAuthorizationHeaders(ContainerImage containerImage, ContainerRegistryConfiguration registryConfiguration);

	/**
	 * @param registryConfiguration configuration such as credentials and additional information required to obtain the
	 *                              authorized headers.
	 * @return Returns HTTP headers, configured with authorization credentials or tokens that would allow access
	 * the target Registry.
	 */
	HttpHeaders getAuthorizationHeaders(ContainerRegistryConfiguration registryConfiguration, Map<String, String> configProperties);
}
