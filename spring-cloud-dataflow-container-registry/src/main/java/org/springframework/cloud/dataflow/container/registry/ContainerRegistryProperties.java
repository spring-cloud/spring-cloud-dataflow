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

package org.springframework.cloud.dataflow.container.registry;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 */
@ConfigurationProperties(prefix = ContainerRegistryProperties.CONTAINER_IMAGE_METADATA_PREFIX)
public class ContainerRegistryProperties {

	public static final String CONTAINER_IMAGE_METADATA_PREFIX = "spring.cloud.dataflow.container";
	public static final String OCI_IMAGE_MANIFEST_MEDIA_TYPE = "application/vnd.oci.image.manifest.v1+json";
	public static final String DOCKER_IMAGE_MANIFEST_MEDIA_TYPE = "application/vnd.docker.distribution.manifest.v2+json";
	public static final String DOCKER_HUB_HOST = "registry-1.docker.io";
	public static final String DEFAULT_TAG = "latest";
	public static final String DEFAULT_OFFICIAL_REPO_NAMESPACE = "library";

	/**
	 * Default registry host to use when not provided in the image name.
	 * Usually the privet registries provide the host as the part of the image name, while empty host defaults to
	 * the DockerHub public registry.
	 */
	private String defaultRegistryHost = DOCKER_HUB_HOST;

	/**
	 * Default repository tag to be used if not provided through the image name.
	 */
	private String defaultRepositoryTag = DEFAULT_TAG;

	/**
	 * (applicable only for the default registry host) default namespace used if the image belongs to the default
	 * registry but no namespace was specified.
	 */
	private String officialRepositoryNamespace = DEFAULT_OFFICIAL_REPO_NAMESPACE;

	/**
	 * Registry authentication configuration
	 */
	private Map<String, ContainerRegistryConfiguration> registryConfigurations = new HashMap<>();

	/**
	 * HTTP Cache Proxy
	 */
	private HttpProxy httpProxy = new HttpProxy();

	/**
	 * When the `kubectl create secret docker-registry` command is used without explicit docker-server property set
	 * the later defaults to `https://index.docker.io/v1/` (or to `domain.io`). Those secrets can be used as
	 * K8s `imagePullSecret` to pull images from Docker Hub but can not be used for SCDF Metadata Container Registry access.
	 * Later expects a docker-server=registry-1.docker.io instead.
	 * To be able to reuse docker registry secretes for the purpose of imagePullSecrets and SCDF Container Metadata retrieval.
	 * by default the `https://index.docker.io/v1/` and `domain.io` docker-server values found in any mounted dockerconfigjson secret
	 * are replaced by `registry-1.docker.io`.
	 *
	 * You can override this behaviour by setting replaceDefaultDockerRegistryServer to false.
	 */
	private boolean replaceDefaultDockerRegistryServer = true;

	public static class HttpProxy {
		private String host = "";
		private int port = 0;

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public boolean isEnabled() {
			return StringUtils.hasText(host);
		}
	}

	public HttpProxy getHttpProxy() {
		return httpProxy;
	}

	public Map<String, ContainerRegistryConfiguration> getRegistryConfigurations() {
		return registryConfigurations;
	}

	public void setRegistryConfigurations(Map<String, ContainerRegistryConfiguration> registryConfigurations) {
		this.registryConfigurations = registryConfigurations;
	}

	public String getDefaultRegistryHost() {
		return defaultRegistryHost;
	}

	public void setDefaultRegistryHost(String defaultRegistryHost) {
		this.defaultRegistryHost = defaultRegistryHost;
	}

	public String getDefaultRepositoryTag() {
		return defaultRepositoryTag;
	}

	public void setDefaultRepositoryTag(String defaultRepositoryTag) {
		this.defaultRepositoryTag = defaultRepositoryTag;
	}

	public String getOfficialRepositoryNamespace() {
		return officialRepositoryNamespace;
	}

	public void setOfficialRepositoryNamespace(String officialRepositoryNamespace) {
		this.officialRepositoryNamespace = officialRepositoryNamespace;
	}

	public boolean isReplaceDefaultDockerRegistryServer() {
		return replaceDefaultDockerRegistryServer;
	}

	public void setReplaceDefaultDockerRegistryServer(boolean replaceDefaultDockerRegistryServer) {
		this.replaceDefaultDockerRegistryServer = replaceDefaultDockerRegistryServer;
	}

	@Override
	public String toString() {
		return "ContainerRegistryProperties{" +
				"defaultRegistryHost='" + defaultRegistryHost + '\'' +
				", defaultRepositoryTag='" + defaultRepositoryTag + '\'' +
				", officialRepositoryNamespace='" + officialRepositoryNamespace + '\'' +
				", registryConfigurations=" + registryConfigurations +
				'}';
	}
}
