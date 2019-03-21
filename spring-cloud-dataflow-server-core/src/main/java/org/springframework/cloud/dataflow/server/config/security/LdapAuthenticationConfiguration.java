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

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.server.config.security.support.LdapSecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
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
@ConditionalOnProperty("dataflow.security.authentication.ldap.enabled")
@EnableConfigurationProperties(LdapSecurityProperties.class)
public class LdapAuthenticationConfiguration extends GlobalAuthenticationConfigurerAdapter {

	@Autowired
	private LdapSecurityProperties ldapSecurityProperties;

	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {

		LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder> ldapConfigurer = auth.ldapAuthentication();

		ldapConfigurer.contextSource()
				.url(ldapSecurityProperties.getUrl().toString())
				.managerDn(ldapSecurityProperties.getManagerDn())
				.managerPassword(ldapSecurityProperties.getManagerPassword());

		if (!StringUtils.isEmpty(ldapSecurityProperties.getUserDnPattern())) {
			ldapConfigurer.userDnPatterns(ldapSecurityProperties.getUserDnPattern());
		}

		if (!StringUtils.isEmpty(ldapSecurityProperties.getUserSearchFilter())) {
			ldapConfigurer
					.userSearchBase(ldapSecurityProperties.getUserSearchBase())
					.userSearchFilter(ldapSecurityProperties.getUserSearchFilter());
		}

		if (!StringUtils.isEmpty(ldapSecurityProperties.getGroupSearchFilter())) {
			ldapConfigurer.groupSearchBase(ldapSecurityProperties.getGroupSearchBase())
					.groupSearchFilter(ldapSecurityProperties.getGroupSearchFilter())
					.groupRoleAttribute(ldapSecurityProperties.getGroupRoleAttribute());
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