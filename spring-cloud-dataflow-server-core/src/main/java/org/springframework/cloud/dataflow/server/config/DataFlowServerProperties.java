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
package org.springframework.cloud.dataflow.server.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.app.resolver.AetherProperties;
import org.springframework.util.StringUtils;

/**
 * Dataflow Server configuration properties.
 *
 * @author Ilayaperumal Gopinathan
 * @author Eric Bottard
 */
@ConfigurationProperties(prefix = DataFlowServerProperties.PREFIX)
public class DataFlowServerProperties {

	static final String PREFIX = "deployer";

	static final String AETHER_PROXY_PREFIX = "aether.proxy.";

	static final String AETHER_PROXY_AUTH_PREFIX = "aether.proxy.auth.";

	/**
	 * File path to a locally available maven repository, where modules will be downloaded.
	 */
	private String localRepository;

	/**
	 * Locations of remote maven repositories from which modules will be downloaded, if not available locally.
	 */
	private String[] remoteRepositories;

	/**
	 * Whether the resolver should operate in offline mode.
	 */
	private Boolean offline;

	/**
	 *
	 */
	private AetherProperties aether;

	/**
	 * Return a String representation of the properties actually set in this object, suitable for
	 * passing to a ModuleLauncher process.
	 */
	public Map<String, String> asStringProperties() {
		Map<String, String> properties = new HashMap<>();
		if (StringUtils.hasText(this.getLocalRepository())) {
			properties.put("localRepository", this.getLocalRepository());
		}
		if (this.getRemoteRepositories() != null) {
			properties.put("remoteRepositories", StringUtils.arrayToCommaDelimitedString(
					this.getRemoteRepositories()));
		}
		if (this.getOffline() != null) {
			properties.put("offline", String.valueOf(this.getOffline()));
		}
		if (getAether() != null && getAether().getProxy() != null) {
			AetherProperties.Proxy proxy = getAether().getProxy();
			if (StringUtils.hasText(proxy.getProtocol())) {
				properties.put(AETHER_PROXY_PREFIX + "protocol", proxy.getProtocol());
			}
			if (StringUtils.hasText(proxy.getHost())) {
				properties.put(AETHER_PROXY_PREFIX + "host", proxy.getHost());
			}
			if (proxy.getPort() > 0) {
				properties.put(AETHER_PROXY_PREFIX + "port", String.valueOf(proxy.getPort()));
			}
			if (StringUtils.hasText(proxy.getNonProxyHosts())) {
				properties.put(AETHER_PROXY_PREFIX + "nonProxyHosts", proxy.getNonProxyHosts());
			}
			if (proxy.getAuth() != null) {
				AetherProperties.Proxy.Authentication auth = proxy.getAuth();
				if (StringUtils.hasText(auth.getUsername())) {
					properties.put(AETHER_PROXY_AUTH_PREFIX + "username", auth.getUsername());
				}
				if (StringUtils.hasText(auth.getPassword())) {
					properties.put(AETHER_PROXY_AUTH_PREFIX + "password", auth.getPassword());
				}
			}
		}
		return properties;
	}


	public void setRemoteRepositories(String[] remoteRepositories) {
		this.remoteRepositories = remoteRepositories;
	}

	public String[] getRemoteRepositories() {
		return remoteRepositories;
	}

	public void setLocalRepository(String localRepository) {
		this.localRepository = localRepository;
	}

	public String getLocalRepository() {
		return localRepository;
	}

	public Boolean getOffline() {
		return offline;
	}

	public void setOffline(Boolean offline) {
		this.offline = offline;
	}

	public AetherProperties getAether() {
		return this.aether;
	}

	public void setAether(AetherProperties aether) {
		this.aether = aether;
	}
}
