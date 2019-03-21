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
package org.springframework.cloud.dataflow.server.config.security.support;

/**
 * State-holder for computed security meta-information.
 *
 * @author Gunnar Hillert
 */
public class SecurityStateBean {

	private boolean authenticationEnabled;

	private boolean authorizationEnabled;

	public SecurityStateBean() {
		super();
	}

	public boolean isAuthenticationEnabled() {
		return authenticationEnabled;
	}

	public void setAuthenticationEnabled(boolean authenticationEnabled) {
		this.authenticationEnabled = authenticationEnabled;
	}

	public boolean isAuthorizationEnabled() {
		return authorizationEnabled;
	}

	public void setAuthorizationEnabled(boolean authorizationEnabled) {
		this.authorizationEnabled = authorizationEnabled;
	}

}
