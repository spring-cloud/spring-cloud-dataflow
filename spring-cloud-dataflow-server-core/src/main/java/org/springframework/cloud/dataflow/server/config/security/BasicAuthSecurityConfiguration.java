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

import org.springframework.cloud.dataflow.server.config.security.support.OnSecurityEnabledAndOAuth2Disabled;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Setup Spring Security with Basic Authentication for the Rest Endpoints and the
 * Dashboard of Spring Cloud Data Flow.
 *
 * For the OAuth2-specific configuration see {@link OAuthSecurityConfiguration}.
 *
 * An (optionally) injected {@link AuthenticationProvider} will be used if available,
 * e.g. via OAuth2-specific configuration.
 *
 * @author Gunnar Hillert
 * @since 1.0
 *
 * @see OAuthSecurityConfiguration
 *
 */
@Configuration
@Conditional(OnSecurityEnabledAndOAuth2Disabled.class)
@EnableWebSecurity
public class BasicAuthSecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		SharedSecurityConfigurator.configureSharedHttpSecurity(http);
	}

}
