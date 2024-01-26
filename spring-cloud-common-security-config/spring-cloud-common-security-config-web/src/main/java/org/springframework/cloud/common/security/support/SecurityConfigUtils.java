/*
 * Copyright 2017-2020 the original author or authors.
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
package org.springframework.cloud.common.security.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import org.springframework.cloud.common.security.AuthorizationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * State-holder for computed security meta-information.
 *
 * @author Gunnar Hillert
 */
public class SecurityConfigUtils {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SecurityConfigUtils.class);

	public static final String ROLE_PREFIX = "ROLE_";

	public static final Pattern AUTHORIZATION_RULE;

	public static final String BASIC_AUTH_REALM_NAME = "Spring";

	static {
		String methodsRegex = StringUtils.arrayToDelimitedString(HttpMethod.values(), "|");
		AUTHORIZATION_RULE = Pattern.compile("(" + methodsRegex + ")\\s+(.+)\\s+=>\\s+(.+)");
	}

	/**
	 * Read the configuration for "simple" (that is, not ACL based) security and apply it.
	 *
	 * @param security The ExpressionUrlAuthorizationConfigurer to apply the authorization rules to
	 * @param authorizationProperties Contains the rules to configure authorization
	 *
	 * @return ExpressionUrlAuthorizationConfigurer
	 */
	public static ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry configureSimpleSecurity(
			ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry security,
			AuthorizationProperties authorizationProperties) {
		for (String rule : authorizationProperties.getRules()) {
			Matcher matcher = AUTHORIZATION_RULE.matcher(rule);
			Assert.isTrue(matcher.matches(),
					String.format("Unable to parse security rule [%s], expected format is 'HTTP_METHOD ANT_PATTERN => "
							+ "SECURITY_ATTRIBUTE(S)'", rule));

			HttpMethod method = HttpMethod.valueOf(matcher.group(1).trim());
			String urlPattern = matcher.group(2).trim();
			String attribute = matcher.group(3).trim();

			logger.info("Authorization '{}' | '{}' | '{}'", method, attribute, urlPattern);
			security = security.requestMatchers(method, urlPattern).access(attribute);
		}
		return security;
	}
}
