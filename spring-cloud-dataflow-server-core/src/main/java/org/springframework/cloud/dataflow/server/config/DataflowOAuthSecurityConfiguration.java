/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.common.security.OAuthSecurityConfiguration;
import org.springframework.cloud.common.security.support.OnOAuth2SecurityEnabled;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;

/**
 * Setup Spring Security OAuth for the Rest Endpoints of Spring Cloud Data Flow.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 */
@Configuration
@Conditional(OnOAuth2SecurityEnabled.class)
public class DataflowOAuthSecurityConfiguration extends OAuthSecurityConfiguration {

	private static final String SWAGGER_UI_CONTEXT = "/swagger-ui/**";

	@Value("${springdoc.api-docs.path:/v3/api-docs}")
	private String apiDocsPath;

	@Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
	private String swaggerUiPath;

	@Value("${springdoc.webjars.prefix:/webjars}")
	private String webjarsPrefix;

	@Value("${springdoc.swagger-ui.configUrl:/v3/api-docs/swagger-config}")
	private String swaggerUiConfig;

	@Value("${springdoc.swagger-ui.validatorUrl:validator.swagger.io/validator}")
	private String swaggerUiValidatorUrl;

	@Value("${springdoc.swagger-ui.oauth2RedirectUrl:/swagger-ui/oauth2-redirect.html}")
	private String swaggerUiOAuth2RedirectUrl;

	@Override
	public void configure(WebSecurity web) {
		String apiDocsPathContext = StringUtils.substringBeforeLast(apiDocsPath, "/");
		web.ignoring().antMatchers(
				SWAGGER_UI_CONTEXT,
				swaggerUiPath,
				webjarsPrefix,
				webjarsPrefix + "/**",
				swaggerUiConfig,
				swaggerUiValidatorUrl,
				swaggerUiOAuth2RedirectUrl,
				apiDocsPathContext + "/**");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		super.configure(http);
	}
}
