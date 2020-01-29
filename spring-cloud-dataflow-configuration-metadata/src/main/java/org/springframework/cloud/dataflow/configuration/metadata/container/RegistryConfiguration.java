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

package org.springframework.cloud.dataflow.configuration.metadata.container;

import java.util.HashMap;
import java.util.Map;

/**
 * Configurations specific for each target Container Registry provider/instance.
 *
 * The Docker Hub configuration is set by default. Additional registries can be configured through the
 * {@link ContainerImageMetadataProperties#getRegistryConfigurations()} properties like this:
 *
 * <code>
 *  Configure Arifactory/JFrog private container registry:
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[0].registry-host=springsource-docker-private-local.jfrog.io
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[0].authorization-type=basicauth
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[0].user=[artifactory user]
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[0].secret=[artifactory encryptedkey]
 *
 *  Configure Amazon ECR private registry:
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[1].registry-host=283191309520.dkr.ecr.us-west-1.amazonaws.com
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[1].authorization-type=awsecr
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[1].user=[your AWS accessKey]
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[1].secret=[your AWS secretKey]
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[1].extra[region]=us-west-1
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[1].extra[registryIds]=283191309520
 *
 *  Configure Azure private container registry
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[2].registry-host=tzolovazureregistry.azurecr.io
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[2].authorization-type=basicauth
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[2].user=[your Azure registry username]
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[2].secret=[your Azure registry access password]
 *
 *  Harbor Registry. Same as DockerHub but with different registryAuthUri
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[3].registry-host=demo.goharbor.io
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[3].authorization-type=dockerhub
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[3].user=admin
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[3].secret=Harbor12345
 *    - spring.cloud.dataflow.container.metadata.registry-configurations[3].extra[registryAuthUri]=https://demo.goharbor.io/service/token?service=harbor-registry&scope=repository:{repository}:pull
 * </code>
 *
 * @author Christian Tzolov
 */
public class RegistryConfiguration {

	/**
	 * Registry authorization types supported by SCDF.
	 */
	public enum AuthorizationType {

		/**
		 * Uses a basic authentication. Can be used with the
		 * Azure Container Registry or the Artifactory/JFrog registries.
		 */
		basicauth,

		/**
		 * OAuth2 token based authorization.
		 * Can be used the DockerHub or Harbor registries.
		 */
		dockerhub,

		/**
		 * AWS ECR authorization model.
		 * Set the 'user' to your AWS accessKey as the 'secret' to the AWS secretKey.
		 * You have to provided the AWS region via the extra[region]=your AWS region
		 */
		awsecr
	}

	/**
	 * Container Registry Host (and optional port). Must me unique per registry.
	 *
	 * Used as a key to to map a container image to target registry where it is stored!
	 */
	private String registryHost;

	/**
	 * Credentials used by the registry specific {@link org.springframework.cloud.dataflow.configuration.metadata.container.authorization.RegistryAuthorizer}
	 * (determined by the {@link #authorizationType}) to authorize the registry access.
	 */
	private String user;
	private String secret;

	/**
	 * Authorization type supported by this Registry.
	 */
	private AuthorizationType authorizationType;

	/**
	 * Image Manifest media type. Docker and OCI are supported.
	 */
	private String manifestMediaType = ContainerImageMetadataProperties.DOCKER_IMAGE_MANIFEST_MEDIA_TYPE;

	/**
	 * Additional registry specific configuration properties.
	 * Usually used inside the Registry authorizer implementations. For example check the AwsEcrAuthorizer implementation.
	 */
	private Map<String, String> extra = new HashMap<>();

	public Map<String, String> getExtra() {
		return extra;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getRegistryHost() {
		return registryHost;
	}

	public void setRegistryHost(String registryHost) {
		this.registryHost = registryHost;
	}

	public AuthorizationType getAuthorizationType() {
		return authorizationType;
	}

	public void setAuthorizationType(AuthorizationType authorizationType) {
		this.authorizationType = authorizationType;
	}

	public String getManifestMediaType() {
		return manifestMediaType;
	}

	public void setManifestMediaType(String manifestMediaType) {
		this.manifestMediaType = manifestMediaType;
	}

	@Override
	public String toString() {
		return "RegistryConfiguration{" +
				"registryHost='" + registryHost + '\'' +
				", user='" + user + '\'' +
				", secret='" + secret + '\'' +
				", authorizationType=" + authorizationType +
				", manifestMediaType='" + manifestMediaType + '\'' +
				", extra=" + extra +
				'}';
	}
}
