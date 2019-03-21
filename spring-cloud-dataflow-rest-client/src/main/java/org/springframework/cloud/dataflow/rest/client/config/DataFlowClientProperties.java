/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.cloud.dataflow.rest.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;

/**
 * Configuration properties used in {@link DataFlowClientAutoConfiguration}
 *
 * @author Vinicius Carvalho
 * @author David Turanski
 */
@ConfigurationProperties(prefix = DataFlowPropertyKeys.PREFIX + "client")
public class DataFlowClientProperties {

	/**
	 * The Data Flow server URI.
	 */
	private String serverUri = "http://localhost:9393";

	private Authentication authentication = new Authentication();

	/**
	 * Skip Ssl validation.
	 */
	private boolean skipSslValidation = true;


	/**
	 * Enable Data Flow DSL access.
	 */
	private boolean enableDsl = false;

	public boolean isEnableDsl() {
		return enableDsl;
	}

	public void setEnableDsl(boolean enableDsl) {
		this.enableDsl = enableDsl;
	}

	public boolean isSkipSslValidation() {
		return skipSslValidation;
	}

	public void setSkipSslValidation(boolean skipSslValidation) {
		this.skipSslValidation = skipSslValidation;
	}

	public String getServerUri() {
		return serverUri;
	}

	public void setServerUri(String serverUri) {
		this.serverUri = serverUri;
	}

	public Authentication getAuthentication() {
		return authentication;
	}

	public void setAuthentication(Authentication authentication) {
		this.authentication = authentication;
	}

	public static class Authentication {

		private Basic basic = new Basic();

		public Basic getBasic() {
			return basic;
		}

		public void setBasic(Basic basic) {
			this.basic = basic;
		}

		public static class Basic {

			/**
			 * The login username.
			 */
			private String username;

			/**
			 * The login password.
			 */
			private String password;

			public String getUsername() {
				return username;
			}

			public void setUsername(String username) {
				this.username = username;
			}

			public String getPassword() {
				return password;
			}

			public void setPassword(String password) {
				this.password = password;
			}
		}


	}
}
