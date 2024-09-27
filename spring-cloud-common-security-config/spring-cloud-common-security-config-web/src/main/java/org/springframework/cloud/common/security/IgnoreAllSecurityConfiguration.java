/*
 * Copyright 2018-2019 the original author or authors.
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

import org.springframework.cloud.common.security.support.OnOAuth2SecurityDisabled;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.WebSecurity;

/**
 * Spring Security {@link WebSecurityConfigurer} simply ignoring all paths conditionally if security is not enabled.
 *
 * The org.springframework.cloud.common.security.enabled=true property disables this configuration and
 * fall back to the Spring Boot default security configuration.
 *
 * @author Janne Valkealahti
 * @author Gunnar Hillert
 * @author Christian Tzolov
 *
 */
@Configuration
@Conditional(OnOAuth2SecurityDisabled.class)
public class IgnoreAllSecurityConfiguration implements WebSecurityConfigurer<WebSecurity> {

	@Override
	public void init(WebSecurity builder) {
	}

	@Override
	public void configure(WebSecurity builder) {
		builder.ignoring().requestMatchers("/**");
	}

}
