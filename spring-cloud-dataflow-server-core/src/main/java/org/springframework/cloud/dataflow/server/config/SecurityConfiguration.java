/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.common.security.BasicAuthSecurityConfiguration;
import org.springframework.cloud.common.security.DefaultBootUserAuthenticationConfiguration;
import org.springframework.cloud.common.security.FileAuthenticationConfiguration;
import org.springframework.cloud.common.security.IgnoreAllSecurityConfiguration;
import org.springframework.cloud.common.security.LdapAuthenticationConfiguration;
import org.springframework.cloud.common.security.OAuthSecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@Import({ BasicAuthSecurityConfiguration.class, DefaultBootUserAuthenticationConfiguration.class,
		OAuthSecurityConfiguration.class, IgnoreAllSecurityConfiguration.class })
public class SecurityConfiguration {

	@Configuration
	@ConditionalOnProperty(name = "spring.cloud.dataflow.security.authentication.file.enabled", havingValue = "true")
	public class FileBasedAuthenticationConfiguration extends FileAuthenticationConfiguration {
	}

	@Configuration
	@ConditionalOnProperty(name = "spring.cloud.dataflow.security.authentication.ldap.enabled", havingValue = "true")
	public class LdapBasedAuthenticationConfiguration extends LdapAuthenticationConfiguration {
	}
}
