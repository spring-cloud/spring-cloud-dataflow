/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.resource.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.RepresentationModel;

/**
 * Provides security related meta-information. E.g. is security enabled, username, roles
 * etc.
 *
 * @author Gunnar Hillert
 */
public class SecurityInfoResource extends RepresentationModel {

	private boolean authenticationEnabled;

	private boolean authenticated;

	private String username;

	private List<String> roles = new ArrayList<>(0);

	private List<String> clientRegistrations = new ArrayList<>(0);

	/**
	 * Default constructor for serialization frameworks.
	 */
	public SecurityInfoResource() {
	}

	/**
	 * @return true if the authentication feature is enabled, false otherwise
	 */
	public boolean isAuthenticationEnabled() {
		return authenticationEnabled;
	}

	public void setAuthenticationEnabled(boolean authenticationEnabled) {
		this.authenticationEnabled = authenticationEnabled;
	}

	/**
	 * @return True if the user is authenticated
	 */
	public boolean isAuthenticated() {
		return authenticated;
	}

	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}

	/**
	 * @return The username of the authenticated user, null otherwise.
	 */
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return List of Roles, if no roles are associated, an empty collection is returned.
	 */
	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	/**
	 *
	 * @return List of all available client registrations
	 */
	public List<String> getClientRegistrations() {
		return clientRegistrations;
	}

	public void setClientRegistrations(List<String> clientRegistrations) {
		this.clientRegistrations = clientRegistrations;
	}

	/**
	 * @param role Adds the role to {@link #roles}
	 * @return the resource with an additional role
	 */
	public SecurityInfoResource addRole(String role) {
		this.roles.add(role);
		return this;
	}
}
