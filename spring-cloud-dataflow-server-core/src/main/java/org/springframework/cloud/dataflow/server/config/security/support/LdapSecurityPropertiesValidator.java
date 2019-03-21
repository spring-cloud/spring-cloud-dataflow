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
package org.springframework.cloud.dataflow.server.config.security.support;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.StringUtils;

/**
 *
 * @author Gunnar Hillert
 * @since 1.1.0
 *
 */
public class LdapSecurityPropertiesValidator implements ConstraintValidator<LdapSecurityPropertiesValid, Object> {

	@Override
	public void initialize(LdapSecurityPropertiesValid constraintAnnotation) {
	}

	@Override
	public boolean isValid(Object value, ConstraintValidatorContext context) {
		if (!(value instanceof LdapSecurityProperties)) {
			throw new IllegalArgumentException("@LdapSecurityPropertiesValid only applies to LdapSecurityProperties");
		}

		final LdapSecurityProperties ldapSecurityProperties = (LdapSecurityProperties) value;

		boolean isValid = true;

		if (!(StringUtils.isEmpty(ldapSecurityProperties.getUserDnPattern())
			^ StringUtils.isEmpty(ldapSecurityProperties.getUserSearchFilter()))) {
			context.buildConstraintViolationWithTemplate(
				"Exactly one of 'userDnPattern' or 'userSearch' must be provided").addConstraintViolation();
			isValid = false;
		}

		return isValid;
	}

}
