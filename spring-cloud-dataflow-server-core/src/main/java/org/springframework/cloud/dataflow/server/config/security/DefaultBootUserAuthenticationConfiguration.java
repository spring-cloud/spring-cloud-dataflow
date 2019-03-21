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
package org.springframework.cloud.dataflow.server.config.security;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.cloud.dataflow.server.config.security.support.CoreSecurityRoles;
import org.springframework.cloud.dataflow.server.config.security.support.OnDefaultBootUserAuthenticationEnabled;
import org.springframework.cloud.dataflow.server.config.security.support.OnSecurityEnabledAndOAuth2Disabled;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.util.StringUtils;

/**
 * Activated if basic authentication is enabled and neither
 * {@link FileAuthenticationConfiguration} nor {@link LdapAuthenticationConfiguration} are
 * loaded. In that case the Spring Boot default user is used and if that user was not
 * explicitly customized by the user, the user will get full access to the application,
 * assigning her all applicable roles.
 *
 * @author Gunnar Hillert
 * @since 1.2.0
 */
@Configuration
@Conditional({ OnDefaultBootUserAuthenticationEnabled.class, OnSecurityEnabledAndOAuth2Disabled.class })
public class DefaultBootUserAuthenticationConfiguration extends GlobalAuthenticationConfigurerAdapter {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BasicAuthSecurityConfiguration.class);

	@Autowired
	private SecurityProperties securityProperties;

	@Autowired(required = false)
	private ManagementServerProperties managementServerProperties;

	/**
	 * Initializes the {@link AuthenticationManagerBuilder}. Creates an
	 * {@link InMemoryUserDetailsManager} with the provided
	 * {@link AuthenticationManagerBuilder}. {@link SecurityProperties#getUser()} must
	 * contain 1 user.
	 *
	 * @param auth the authentication manager builder
	 */
	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {

		final User user = this.securityProperties.getUser();

		final User defaultSpringBootUser = new SecurityProperties().getUser();

		final String[] rolesToPopulate;

		final boolean hasDefaultRoles;

		if (this.managementServerProperties != null
				&& this.managementServerProperties.getSecurity().getRoles().size() == 1
				&& "MANAGE".equals(this.managementServerProperties.getSecurity().getRoles().get(0))) {
			defaultSpringBootUser.getRole().add("MANAGE");
		}

		if (defaultSpringBootUser.getName().equals(user.getName())
				&& user.getRole().size() == defaultSpringBootUser.getRole().size()
				&& defaultSpringBootUser.getRole().equals(user.getRole())) {
			hasDefaultRoles = true;
		}
		else {
			hasDefaultRoles = false;
		}

		if (hasDefaultRoles) {
			rolesToPopulate = CoreSecurityRoles.getAllRolesAsStringArray();
		}
		else {
			rolesToPopulate = user.getRole().toArray(new String[user.getRole().size()]);
		}

		if (user.isDefaultPassword()) {
			logger.info(String.format("%n%nUsing default security password: %s with roles '%s'%n", user.getPassword(),
					StringUtils.arrayToCommaDelimitedString(rolesToPopulate)));
		}

		auth.inMemoryAuthentication().withUser(user.getName()).password(user.getPassword()).roles(rolesToPopulate);
	}

}
