/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.cloud.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.common.security.support.OnOAuth2SecurityEnabled;
import org.springframework.cloud.common.security.support.SecurityStateBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * We need to mimic the configuration of Dataflow and Skipper
 *
 * @author Corneil du Plessis
 */
@Configuration(proxyBeanMethods = false)
@Conditional(OnOAuth2SecurityEnabled.class)
@Import(TestOAuthSecurityConfiguration.SecurityStateBeanConfig.class)
public class TestOAuthSecurityConfiguration extends OAuthSecurityConfiguration {

	@Configuration(proxyBeanMethods = false)
	public static class SecurityStateBeanConfig {
		@Bean
		public SecurityStateBean securityStateBean() {
			return new SecurityStateBean();
		}

		@Bean
		@ConfigurationProperties(prefix = "spring.cloud.common.security.test.authorization")
		public AuthorizationProperties authorizationProperties() {
			return new AuthorizationProperties();
		}
	}
}
