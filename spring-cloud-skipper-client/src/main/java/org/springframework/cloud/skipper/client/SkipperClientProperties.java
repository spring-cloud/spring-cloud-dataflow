/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for making a connection to the Skipper server.
 * @author Mark Pollack
 */
@ConfigurationProperties("spring.cloud.skipper.client")
public class SkipperClientProperties {

	public static final String DEFAULT_HOME = System.getProperty("user.home") + java.io.File.separator + ".skipper";

	public static final String DEFAULT_SCHEME = "http";

	public static final String DEFAULT_HOST = "localhost";

	public static final int DEFAULT_PORT = 7577; // = skpr :)

	public static final String DEFAULT_TARGET = DEFAULT_SCHEME + "://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/";

	public static final String DEFAULT_USERNAME = "";

	public static final String DEFAULT_PASSWORD = "";

	public static final String DEFAULT_SKIP_SSL_VALIDATION = "true";

	public static final String DEFAULT_CREDENTIALS_PROVIDER_COMMAND = "";

	private String home = DEFAULT_HOME;

	private String uri = DEFAULT_TARGET;

	private String username = DEFAULT_USERNAME;

	private String password = DEFAULT_PASSWORD;

	private boolean skipSllValidation = Boolean.getBoolean(DEFAULT_SKIP_SSL_VALIDATION);

	private String credentialsProviderCommand = DEFAULT_CREDENTIALS_PROVIDER_COMMAND;

	public String getHome() {
		return home;
	}

	public void setHome(String home) {
		this.home = home;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String url) {
		this.uri = url;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isSkipSllValidation() {
		return skipSllValidation;
	}

	public void setSkipSllValidation(boolean skipSllValidation) {
		this.skipSllValidation = skipSllValidation;
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
