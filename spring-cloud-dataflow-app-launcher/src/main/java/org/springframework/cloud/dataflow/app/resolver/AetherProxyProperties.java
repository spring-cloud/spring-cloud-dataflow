/*
 * Copyright 2016 the original author or authors.
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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Proxy properties for the Aether Module Resolver.
 *
 * @author Ilayaperumal Gopinathan
 */
@ConfigurationProperties(prefix = "aether.proxy")
public class AetherProxyProperties {
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
