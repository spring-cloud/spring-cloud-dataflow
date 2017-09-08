/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.security.common;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Holds configuration for the authorization aspects of security.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
public class AuthorizationProperties {

	private boolean enabled = true;

	private List<String> rules = new ArrayList<>();

	private String dashboardUrl = "/dashboard";

	private String loginUrl = "/#/login";

	private String loginProcessingUrl = "/login";

	private String logoutUrl = "/logout";

	private String logoutSuccessUrl = "/logout-success.html";

	private List<String> permitAllPaths = new ArrayList<>();

	private List<String> authenticatedPaths = new ArrayList<>();

	public List<String> getRules() {
		return rules;
	}

	public void setRules(List<String> rules) {
		this.rules = rules;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getDashboardUrl() {
		return dashboardUrl;
	}

	public void setDashboardUrl(String dashboardUrl) {
		this.dashboardUrl = dashboardUrl;
	}

	public String getLoginUrl() {
		return loginUrl;
	}

	public void setLoginUrl(String loginUrl) {
		this.loginUrl = loginUrl;
	}

	public String getLoginProcessingUrl() {
		return loginProcessingUrl;
	}

	public void setLoginProcessingUrl(String loginProcessingUrl) {
		this.loginProcessingUrl = loginProcessingUrl;
	}

	public String getLogoutUrl() {
		return logoutUrl;
	}

	public void setLogoutUrl(String logoutUrl) {
		this.logoutUrl = logoutUrl;
	}

	public String getLogoutSuccessUrl() {
		return logoutSuccessUrl;
	}

	public void setLogoutSuccessUrl(String logoutSuccessUrl) {
		this.logoutSuccessUrl = logoutSuccessUrl;
	}

	public List<String> getPermitAllPaths() {
		return permitAllPaths;
	}

	public void setPermitAllPaths(List<String> permitAllPaths) {
		this.permitAllPaths = permitAllPaths;
	}

	public List<String> getAuthenticatedPaths() {
		return authenticatedPaths;
	}

	public void setAuthenticatedPaths(List<String> authenticatedPaths) {
		this.authenticatedPaths = authenticatedPaths;
	}

}
