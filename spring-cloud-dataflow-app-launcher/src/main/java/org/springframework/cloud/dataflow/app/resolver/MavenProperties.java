/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.app.resolver;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration Properties for Maven.
 *
 * @author Ilayaperumal Gopinathan
 * @author Eric Bottard
 * @author Mark Fisher
 */
@ConfigurationProperties(prefix = MavenProperties.PREFIX)
public class MavenProperties {

	static final String PREFIX = "maven";

	static final String PROXY_PREFIX = "proxy.";

	static final String PROXY_AUTH_PREFIX = PROXY_PREFIX + "auth.";

	/**
	 * File path to a locally available maven repository, where modules will be downloaded.
	 */
	private String localRepository;

	/**
	 * Locations of remote maven repositories from which modules will be downloaded, if not available locally.
	 */
	private String[] remoteRepositories = new String[]{"https://repo.spring.io/libs-snapshot"};

	/**
	 * Whether the resolver should operate in offline mode.
	 */
	private boolean offline;

	private Proxy proxy;

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

	public boolean isOffline() {
		return offline;
	}

	public void setOffline(Boolean offline) {
		this.offline = offline;
	}

	public Proxy getProxy() {
		return this.proxy;
	}

	public void setProxy(Proxy proxy) {
		this.proxy = proxy;
	}

	public static class Proxy {

		/**
		 * Protocol to use for proxy settings.
		 */
		private String protocol = "http";

		/**
		 * Host for the proxy.
		 */
		private String host;

		/**
		 * Port for the proxy.
		 */
		private int port;

		/**
		 * List of non proxy hosts.
		 */
		private String nonProxyHosts;

		private Authentication auth;

		public String getProtocol() {
			return this.protocol;
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}

		public String getHost() {
			return this.host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return this.port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public String getNonProxyHosts() {
			return this.nonProxyHosts;
		}

		public void setNonProxyHosts(String nonProxyHosts) {
			this.nonProxyHosts = nonProxyHosts;
		}

		public Authentication getAuth() {
			return this.auth;
		}

		public void setAuth(Authentication auth) {
			this.auth = auth;
		}

		public static class Authentication {

			/**
			 * Username for the proxy.
			 */
			private String username;

			/**
			 * Password for the proxy.
			 */
			private String password;

			public String getUsername() {
				return this.username;
			}

			public void setUsername(String username) {
				this.username = username;
			}

			public String getPassword() {
				return this.password;
			}

			public void setPassword(String password) {
				this.password = password;
			}

		}
	}

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
		properties.put("offline", String.valueOf(this.isOffline()));
		Proxy proxy = getProxy();
		if (proxy != null) {
			if (StringUtils.hasText(proxy.getProtocol())) {
				properties.put(PROXY_PREFIX + "protocol", proxy.getProtocol());
			}
			if (StringUtils.hasText(proxy.getHost())) {
				properties.put(PROXY_PREFIX + "host", proxy.getHost());
			}
			if (proxy.getPort() > 0) {
				properties.put(PROXY_PREFIX + "port", String.valueOf(proxy.getPort()));
			}
			if (StringUtils.hasText(proxy.getNonProxyHosts())) {
				properties.put(PROXY_PREFIX + "nonProxyHosts", proxy.getNonProxyHosts());
			}
			if (proxy.getAuth() != null) {
				Proxy.Authentication auth = proxy.getAuth();
				if (StringUtils.hasText(auth.getUsername())) {
					properties.put(PROXY_AUTH_PREFIX + "username", auth.getUsername());
				}
				if (StringUtils.hasText(auth.getPassword())) {
					properties.put(PROXY_AUTH_PREFIX + "password", auth.getPassword());
				}
			}
		}
		return properties;
	}
}
