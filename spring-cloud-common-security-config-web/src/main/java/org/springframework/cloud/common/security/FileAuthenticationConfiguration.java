/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.cloud.common.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.common.security.support.FileSecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.util.Assert;

/**
 * A security configuration that conditionally sets up in-memory users from a file.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@Configuration
public class FileAuthenticationConfiguration extends GlobalAuthenticationConfigurerAdapter {

	@Autowired
	private FileSecurityProperties fileSecurityProperties;

	/**
	 * Initializes the {@link AuthenticationManagerBuilder}. Creates an
	 * {@link InMemoryUserDetailsManager} with the provided users. Users must contain at
	 * least 1 user.
	 *
	 * @throws IllegalArgumentException if users is empty.
	 */
	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {
		Assert.notEmpty(this.fileSecurityProperties.getUsers(),
				String.format("No user specified. Please specify at least 1 user for the file based authentication."));

		final InMemoryUserDetailsManager inMemory = new InMemoryUserDetailsManager(
				this.fileSecurityProperties.getUsers());
		auth.userDetailsService(inMemory);
	}

}
