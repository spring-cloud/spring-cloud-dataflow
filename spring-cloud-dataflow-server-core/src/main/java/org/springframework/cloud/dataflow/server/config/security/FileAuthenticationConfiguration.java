/*
 * Copyright 2016 the original author or authors.
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

import java.util.Properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.util.Assert;

/**
* A security configuration that conditionally sets up in-memory users from a file.
*
* @author Eric Bottard
* @author Gunnar Hillert
*
* @since 1.1.0
*/
@Configuration
@ConditionalOnProperty("dataflow.security.authentication.file.enabled")
@ConfigurationProperties(prefix = FileAuthenticationConfiguration.CONFIGURATION_PROPERTIES_PREFIX)
public class FileAuthenticationConfiguration extends GlobalAuthenticationConfigurerAdapter {

	public static final String CONFIGURATION_PROPERTIES_PREFIX = "dataflow.security.authentication.file";

	private Properties users;

	/**
	 * Set users as {@link Properties}. Value (String) of the property must be in the format e.g.:
	 * {@code bobspassword, ROLE_NAME}.
	 *
	 */
	public void setUsers(Properties users) {
		this.users = users;
	}

	public Properties getUsers() {
		return users;
	}

	/**
	 * Initializes the {@link AuthenticationManagerBuilder}. Creates an
	 * {@link InMemoryUserDetailsManager} with the provided {@link FileAuthenticationConfiguration#getUsers()}.
	 * {@link FileAuthenticationConfiguration#getUsers()} must contain at least 1 user.
	 *
	 * @throws IllegalArgumentException if {@link FileAuthenticationConfiguration#getUsers()} is empty.
	 */
	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {

		Assert.notEmpty(this.users,
			String.format("No user specified. Please specify at least 1 user (e.g. via '%s')",
				CONFIGURATION_PROPERTIES_PREFIX + ".users"));

		final InMemoryUserDetailsManager inMemory = new InMemoryUserDetailsManager(getUsers());
		auth.userDetailsService(inMemory);
	}

}