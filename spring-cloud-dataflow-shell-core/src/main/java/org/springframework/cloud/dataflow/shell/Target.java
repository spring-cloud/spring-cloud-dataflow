/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.shell;

import java.net.URI;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Encapsulates various data points related to the Data Flow Server Target, such as target
 * URI, success/error state, exception messages that may have occurred.
 *
 * @author Gunnar Hillert
 * @author Chris Bono
 * @since 1.0
 */
public class Target {

	public static final String DEFAULT_SCHEME = "http";

	public static final String DEFAULT_HOST = "localhost";

	public static final int DEFAULT_PORT = 9393;

	public static final String DEFAULT_TARGET = DEFAULT_SCHEME + "://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/";

	public static final String DEFAULT_USERNAME = "";

	public static final String DEFAULT_PASSWORD = "";

	public static final String DEFAULT_CLIENT_REGISTRATION_ID = "dataflow-shell";

	public static final String DEFAULT_SKIP_SSL_VALIDATION = "true";

	public static final String DEFAULT_CREDENTIALS_PROVIDER_COMMAND = "";

	public static final String DEFAULT_PROXY_USERNAME = "";

	public static final String DEFAULT_PROXY_PASSWORD = "";

	public static final String DEFAULT_PROXY_URI = "";

	private final URI targetUri;

	private final boolean skipSslValidation;

	private TargetCredentials targetCredentials;

	private Exception targetException;

	private String targetResultMessage;

	private TargetStatus status;

	private boolean authenticationEnabled;

	private boolean authenticated;

	/**
	 * Construct a new Target. The passed in <code>targetUriAsString</code> String
	 * parameter will be converted to a {@link URI}. This method allows for providing a
	 * username and password for authentication.
	 *
	 * @param targetUriAsString the data flow server URI, must not be empty
	 * @param targetUsername the username, may be empty, if access is unauthenticated
	 * @param targetPassword the password, may be empty
	 * @param skipSslValidation whether or not we skip SSL validation.
	 * @throws IllegalArgumentException if the given URI string violates RFC 2396.
	 */
	public Target(String targetUriAsString, String targetUsername, String targetPassword, boolean skipSslValidation) {
		Assert.hasText(targetUriAsString, "The provided targetUriAsString must neither be null nor empty.");
		this.targetUri = URI.create(targetUriAsString);
		this.skipSslValidation = skipSslValidation;

		if (ObjectUtils.isEmpty(targetUsername)) {
			this.targetCredentials = null;
		}
		else {
			this.targetCredentials = new TargetCredentials(targetUsername, targetPassword);
		}
	}

	/**
	 * Construct a new Target. The passed in <code>targetUriAsString</code> String
	 * parameter will be converted to a {@link URI}.
	 *
	 * @param targetUriAsString Must not be empty
	 * @throws IllegalArgumentException if the given string violates RFC 2396
	 */
	public Target(String targetUriAsString) {
		this(targetUriAsString, null, null, false);
	}

	/**
	 * Return the target status, which is either Success or Error.
	 *
	 * @return The {@link TargetStatus}. May be null.
	 */
	public TargetStatus getStatus() {
		return status;
	}

	/**
	 * If during targeting an error occurred, the resulting {@link Exception} is made
	 * available for further introspection.
	 *
	 * @return If present, returns the Exception, otherwise null is returned.
	 */
	public Exception getTargetException() {
		return targetException;
	}

	/**
	 * Sets the exception in case an error occurred during targeting. Will also set the
	 * respective {@link TargetStatus} to {@link TargetStatus#ERROR}.
	 *
	 * @param targetException Must not be null.
	 */
	public void setTargetException(Exception targetException) {
		Assert.notNull(targetException, "The provided targetException must not be null.");
		this.targetException = targetException;
		this.status = TargetStatus.ERROR;
	}

	/**
	 * Provides a result message indicating whether the provide {@link #getTargetUri()}
	 * was successfully targeted or not.
	 *
	 * @return The formatted result message.
	 */
	public String getTargetResultMessage() {
		return targetResultMessage;
	}

	/**
	 * Set the result messages indicating the success or failure while targeting the
	 * Spring Cloud Data Flow Server.
	 *
	 * @param targetResultMessage Must not be empty.
	 */
	public void setTargetResultMessage(String targetResultMessage) {
		Assert.hasText(targetResultMessage, "The provided targetResultMessage must neither be null nor empty.");
		this.targetResultMessage = targetResultMessage;
	}

	/**
	 * @return The Target Uri. Will never be null.
	 */
	public URI getTargetUri() {
		return targetUri;
	}

	/**
	 * Returns the target URI as a String.
	 *
	 * @return Never null and will always return a valid URI value
	 */
	public String getTargetUriAsString() {
		return targetUri.toString();
	}

	/**
	 * Returns if sslValidation should be skipped
	 *
	 * @return Return whether or not we skip SSL validation.
	 */
	public boolean isSkipSslValidation() {
		return skipSslValidation;
	}

	/**
	 * Returns the target credentials
	 *
	 * @return The target credentials. May be null if there is no authentication
	 */
	public TargetCredentials getTargetCredentials() {
		return targetCredentials;
	}

	public void setTargetCredentials(TargetCredentials targetCredentials) {
		this.targetCredentials = targetCredentials;
	}

	/**
	 * Indicates whether authentication is enabled for this target.
	 *
	 * @return True if authentication is enabled, false otherwise
	 */
	public boolean isAuthenticationEnabled() {
		return authenticationEnabled;
	}

	/**
	 * @param authenticationEnabled False by default
	 */
	public void setAuthenticationEnabled(boolean authenticationEnabled) {
		this.authenticationEnabled = authenticationEnabled;
	}

	/**
	 * @return True if the user is successfully authenticated with this Target
	 */
	public boolean isAuthenticated() {
		return authenticated;
	}

	/**
	 * @param authenticated whether a user is successfully authenticated with the Target
	 */
	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}

	@Override
	public String toString() {
		return "Target [targetUri=" + targetUri + ", targetException=" + targetException + ", targetResultMessage="
				+ targetResultMessage + ", status=" + status + "]";
	}

	public enum TargetStatus {
		SUCCESS,
		ERROR
	}

}
