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
package org.springframework.cloud.skipper.shell.command.support;

/**
 * Encapsulates the credentials to the Skipper Server Target, such as {@link #username}
 * and {@link #password}.
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
public class TargetCredentials {

	private final String username;

	private final String password;

	private final boolean usesAccessToken;

	public TargetCredentials(String username, String password) {
		this.username = username;
		this.password = password;
		this.usesAccessToken = false;
	}

	public TargetCredentials(boolean usesAccessToken) {
		super();
		this.username = null;
		this.password = null;
		this.usesAccessToken = usesAccessToken;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public String toString() {
		if (this.usesAccessToken) {
			return "[Uses OAuth2 Access Token]";
		}
		else {
			return "Credentials [username='" + username + "', password='********']";
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		TargetCredentials that = (TargetCredentials) o;

		if (usesAccessToken != that.usesAccessToken) {
			return false;
		}
		if (username != null ? !username.equals(that.username) : that.username != null) {
			return false;
		}
		return password != null ? password.equals(that.password) : that.password == null;
	}

	@Override
	public int hashCode() {
		int result = username != null ? username.hashCode() : 0;
		result = 31 * result + (password != null ? password.hashCode() : 0);
		result = 31 * result + (usesAccessToken ? 1 : 0);
		return result;
	}
}
