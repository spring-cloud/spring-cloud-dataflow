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

import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.cloud.dataflow.server.config.security.support.OnSecurityEnabledAndOAuth2Enabled;
import org.springframework.cloud.dataflow.server.service.impl.ManualOAuthAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;

/**
 * Setup Spring Security OAuth for the Rest Endpoints of Spring Cloud Data Flow.
 *
 * @author Gunnar Hillert
 * @since 1.1.0
 *
 */
@EnableOAuth2Sso
@Configuration
@Conditional(OnSecurityEnabledAndOAuth2Enabled.class)
public class OAuthSecurityConfiguration {

	@Bean
	public AuthenticationProvider authenticationProvider() {
		return new ManualOAuthAuthenticationProvider();
	}

}