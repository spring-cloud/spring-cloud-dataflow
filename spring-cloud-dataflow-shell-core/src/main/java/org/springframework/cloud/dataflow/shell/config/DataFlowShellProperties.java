/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.cloud.dataflow.shell.Target;

/**
 * Configuration properties for Dataflow Shell.
 *
 * @author Chris Bono
 * @since 2.10
 */
@ConfigurationProperties("dataflow")
public class DataFlowShellProperties {

	/** The uri of the Dataflow REST endpoint */
	private final String uri;

	/** The username for authenticated access to the Admin REST endpoint */
	private final String username;

	/** The password for authenticated access to the Admin REST endpoint */
	private final String password;

	/** The registration id for oauth2 config */
	private final String clientRegistrationId;

	/** Accept any SSL certificate (even self-signed) */
	private final boolean skipSslValidation;

	/** A command to run that outputs the HTTP credentials used for authentication */
	private final String credentialsProviderCommand;

	private final Proxy proxy;

	public DataFlowShellProperties(
			@DefaultValue(Target.DEFAULT_TARGET) String uri,
			@DefaultValue(Target.DEFAULT_USERNAME) String username,
			@DefaultValue(Target.DEFAULT_PASSWORD) String password,
			@DefaultValue(Target.DEFAULT_CLIENT_REGISTRATION_ID) String clientRegistrationId,
			@DefaultValue(Target.DEFAULT_SKIP_SSL_VALIDATION) boolean skipSslValidation,
			@DefaultValue(Target.DEFAULT_CREDENTIALS_PROVIDER_COMMAND) String credentialsProviderCommand,
			@DefaultValue Proxy proxy) {
		this.uri = uri;
		this.username = username;
		this.password = password;
		this.clientRegistrationId = clientRegistrationId;
		this.skipSslValidation = skipSslValidation;
		this.credentialsProviderCommand = credentialsProviderCommand;
		this.proxy = proxy;
	}

	public String getUri() {
		return uri;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getClientRegistrationId() {
		return clientRegistrationId;
	}

	public boolean isSkipSslValidation() {
		return skipSslValidation;
	}

	public String getCredentialsProviderCommand() {
		return credentialsProviderCommand;
	}

	public Proxy getProxy() {
		return proxy;
	}

	public static class Proxy {

		/** The uri of the proxy server */
		private final String uri;

		/** The username for authenticated access to the secured proxy server */
		private final String username;

		/** The password for authenticated access to the secured proxy server */
		private final String password;

		public Proxy(
				@DefaultValue(Target.DEFAULT_PROXY_URI) String uri,
				@DefaultValue(Target.DEFAULT_PROXY_USERNAME) String username,
				@DefaultValue(Target.DEFAULT_PROXY_PASSWORD) String password) {
			this.uri = uri;
			this.username = username;
			this.password = password;
		}

		public String getUri() {
			return uri;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return password;
		}
	}

}
