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

import java.util.Properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
* A security configuration that conditionally sets up in-memory users from a file.
*
* @author Eric Bottard
* @author Gunnar Hillert
*/
@Configuration
@ConditionalOnProperty("dataflow.security.authentication.file.enabled")
@ConfigurationProperties(prefix = "dataflow.security.authentication.file")
public class FileAuthenticationConfiguration extends GlobalAuthenticationConfigurerAdapter {

	private Properties users;

	public void setUsers(Properties users) {
		this.users = users;
	}

	public Properties getUsers() {
		return users;
	}

	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {
		InMemoryUserDetailsManager inMemory = new InMemoryUserDetailsManager(getUsers());
		auth.userDetailsService(inMemory);
	}

}