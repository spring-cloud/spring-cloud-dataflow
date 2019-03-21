/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for making a connection to the Skipper server.
 *
 * @author Mark Pollack
 * @author Gunnar Hillert
 */
@ConfigurationProperties("spring.cloud.skipper.client")
public class SkipperClientProperties {

	public static final String DEFAULT_SCHEME = "http";

	public static final String DEFAULT_HOST = "localhost";

	public static final int DEFAULT_PORT = 7577; // = skpr :)

	public static final String DEFAULT_TARGET = DEFAULT_SCHEME + "://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/api";

	public static final String DEFAULT_USERNAME = "";

	public static final String DEFAULT_PASSWORD = "";

	public static final String DEFAULT_SKIP_SSL_VALIDATION = "true";

	public static final String DEFAULT_CREDENTIALS_PROVIDER_COMMAND = "";

	private String serverUri = DEFAULT_TARGET;

	private String username = DEFAULT_USERNAME;

	private String password = DEFAULT_PASSWORD;

	private boolean skipSslValidation = Boolean.getBoolean(DEFAULT_SKIP_SSL_VALIDATION);

	private String credentialsProviderCommand = DEFAULT_CREDENTIALS_PROVIDER_COMMAND;

	public String getServerUri() {
		return serverUri;
	}

	public void setServerUri(String url) {
		this.serverUri = url;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isSkipSslValidation() {
		return skipSslValidation;
	}

	public void setSkipSslValidation(boolean skipSslValidation) {
		this.skipSslValidation = skipSslValidation;
	}

	public String getCredentialsProviderCommand() {
		return credentialsProviderCommand;
	}

	public void setCredentialsProviderCommand(String credentialsProviderCommand) {
		this.credentialsProviderCommand = credentialsProviderCommand;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
