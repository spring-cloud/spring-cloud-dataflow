/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
* A security configuration that conditionally sets up in-memory users from a file.
*
* @author Marius Bogoevici
* @author Gunnar Hillert
*
* @since 1.1.0
*/
@Configuration
@ConfigurationProperties(prefix = "dataflow.security.authentication.ldap")
@ConditionalOnProperty("dataflow.security.authentication.ldap.enabled")
public class LdapAuthenticationConfiguration extends GlobalAuthenticationConfigurerAdapter {

	private String url;

	private String userDnPattern;

	private String managerDn;

	private String managerPassword;

	private String userSearchBase = "";

	private String userSearchFilter;

	private String groupSearchFilter = "";

	private String groupSearchBase = "";

	private String groupRoleAttribute = "cn";

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUserDnPattern() {
		return userDnPattern;
	}

	public void setUserDnPattern(String userDnPattern) {
		this.userDnPattern = userDnPattern;
	}

	public String getManagerDn() {
		return managerDn;
	}

	public void setManagerDn(String managerDn) {
		this.managerDn = managerDn;
	}

	public String getManagerPassword() {
		return managerPassword;
	}

	public void setManagerPassword(String managerPassword) {
		this.managerPassword = managerPassword;
	}

	public String getUserSearchBase() {
		return userSearchBase;
	}

	public void setUserSearchBase(String userSearchBase) {
		this.userSearchBase = userSearchBase;
	}

	public String getUserSearchFilter() {
		return userSearchFilter;
	}

	public void setUserSearchFilter(String userSearchFilter) {
		this.userSearchFilter = userSearchFilter;
	}

	public String getGroupSearchFilter() {
		return groupSearchFilter;
	}

	public void setGroupSearchFilter(String groupSearchFilter) {
		this.groupSearchFilter = groupSearchFilter;
	}

	public String getGroupSearchBase() {
		return groupSearchBase;
	}

	public void setGroupSearchBase(String groupSearchBase) {
		this.groupSearchBase = groupSearchBase;
	}

	public String getGroupRoleAttribute() {
		return groupRoleAttribute;
	}

	public void setGroupRoleAttribute(String groupRoleAttribute) {
		this.groupRoleAttribute = groupRoleAttribute;
	}

	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {

		LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> ldapConfigurer = auth.ldapAuthentication();

		Assert.hasText(url, "'url' must not be empty");

		Assert.isTrue(StringUtils.isEmpty(userDnPattern) ^ StringUtils.isEmpty(userSearchFilter),
				"exactly one of 'userDnPattern' or 'userSearch' must be provided");

		ldapConfigurer.contextSource()
				.url(url)
				.managerDn(managerDn)
				.managerPassword(managerPassword);

		if (!StringUtils.isEmpty(userDnPattern)) {
			ldapConfigurer.userDnPatterns(userDnPattern);
		}

		if (!StringUtils.isEmpty(userSearchFilter)) {
			ldapConfigurer
					.userSearchBase(userSearchBase)
					.userSearchFilter(userSearchFilter);
		}

		if (!StringUtils.isEmpty(groupSearchFilter)) {
			ldapConfigurer.groupSearchBase(groupSearchBase)
					.groupSearchFilter(groupSearchFilter)
					.groupRoleAttribute(groupRoleAttribute);
		}
		else {
			ldapConfigurer.ldapAuthoritiesPopulator(new LdapAuthoritiesPopulator() {
				@Override
				public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
					return Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN"));
				}
			});
		}

	}
}