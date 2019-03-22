/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.dataflow.server.service;

import java.util.Optional;

import org.springframework.cloud.common.security.support.SecurityStateBean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 *
 * @author Gunnar Hillert
 *
 */
public class SpringSecurityAuditorAware implements AuditorAware<String> {

	private final SecurityStateBean securityStateBean;

	public SpringSecurityAuditorAware(SecurityStateBean securityStateBean) {
		this.securityStateBean = securityStateBean;
	}

	@Override
	public Optional<String> getCurrentAuditor() {
		final boolean authenticationEnabled = securityStateBean.isAuthenticationEnabled();
		if (authenticationEnabled && SecurityContextHolder.getContext() != null) {
			final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (!(authentication instanceof AnonymousAuthenticationToken)) {
				return Optional.of(authentication.getName());
			}
		}
		return Optional.ofNullable(null);
	}
}
