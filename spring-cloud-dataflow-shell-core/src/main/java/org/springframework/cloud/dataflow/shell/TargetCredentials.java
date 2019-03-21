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

package org.springframework.cloud.dataflow.shell;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.dataflow.shell.command.support.RoleType;

/**
 * Encapsulates the credentials to the Data Flow Server Target, such as {@link #username}
 * and {@link #password}. Maybe also, depending on security settings, include a list of
 * {@link #roles}s that are associated with the user account.
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
public class TargetCredentials {

	final List<RoleType> roles = new ArrayList<>(0);

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

	public String getDisplayableContents() {
		if (this.usesAccessToken) {
			return "[Uses OAuth2 Access Token]";
		}
		else {
			return "[username='" + username + "', password='********']";
		}
	}

	public List<RoleType> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles.clear();
		for (String roleAsString : roles) {
			this.roles.add(RoleType.fromKey(roleAsString));
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Credentials [username=");
		builder.append(username);
		builder.append(", password=");
		builder.append("*********");
		builder.append(", roles=");
		builder.append(roles);
		builder.append("]");
		return builder.toString();
	}
}
