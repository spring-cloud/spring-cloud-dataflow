/*
 * Copyright 2022-2022 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.utils.Constants;
import org.springdoc.webmvc.ui.SwaggerConfig;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.dataflow.server.support.SpringDocJsonDecodeFilter;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.util.AntPathMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Lightweight integration tests for {@link SpringDocAutoConfiguration}.
 *
 * @author Chris Bono
 * @author Corneil du Plessis
 */
class SpringDocAutoConfigurationTests {

	// The base web context runner does the following:
	//   - loads default props via config data additional location
	//	 - loads the config props auto-config to ensure config props beans get bound to the env
	//	 - loads all auto-configs that are loaded by Springdoc
	//   - loads custom Springdoc auto-config
	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withPropertyValues("spring.config.additional-location=classpath:/META-INF/dataflow-server-defaults.yml")
			.withInitializer(new ConfigDataApplicationContextInitializer())
			.withBean("mvcConversionService", FormattingConversionService.class, () -> mock(FormattingConversionService.class))
			.withConfiguration(AutoConfigurations.of(
					ConfigurationPropertiesAutoConfiguration.class,
					SpringDocConfiguration.class,
					SpringDocConfigProperties.class,
					SwaggerConfig.class,
					SwaggerUiConfigProperties.class,
					SwaggerUiOAuthProperties.class,
					SpringDocAutoConfiguration.class));

	private WebApplicationContextRunner contextRunnerWithSpringDocEnabled() {
		return contextRunner.withPropertyValues("springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true");
	}

	@Test
	void enabledWithDefaultSpringDocSettings() {
		contextRunnerWithSpringDocEnabled()
				.run((context) -> {
					assertThat(context)
							.hasSingleBean(SpringDocAutoConfiguration.class)
							.hasBean("springDocWebSecurityCustomizer")
							.hasBean("springDocJsonDecodeFilterRegistration");
					verifyFilterHasUrlPatterns(context,"/v3", "/v3/*", "/v3/api-docs", "/v3/api-docs/*");
					verifyCustomizerHasIgnorePatterns(context, "/swagger-ui/**",
							"/v3/**",
							"/swagger-ui.html",
							"/v3/api-docs/swagger-config",
							"https://validator.swagger.io/validator",
							"/swagger-ui/oauth2-redirect.html",
							"/webjars",
							"/webjars/**"
					);
				});
	}

	@Test
	void enabledWithCustomSpringDocSettings() {
		contextRunnerWithSpringDocEnabled()
				.withPropertyValues(
						"springdoc.api-docs.path=/v4/foo/api-docs",
						"springdoc.swagger-ui.config-url=/v4/bar/swagger-config")
				.run((context) -> {
					assertThat(context)
							.hasSingleBean(SpringDocAutoConfiguration.class)
							.hasBean("springDocWebSecurityCustomizer")
							.hasBean("springDocJsonDecodeFilterRegistration");
					verifyFilterHasUrlPatterns(context,"/v4/foo", "/v4/foo/*", "/v4/bar", "/v4/bar/*");
					verifyCustomizerHasIgnorePatterns(context, "/swagger-ui/**",
							"/v4/foo/**",
							"/swagger-ui.html",
							"/v4/bar/swagger-config",
							"https://validator.swagger.io/validator",
							"/swagger-ui/oauth2-redirect.html",
							"/webjars",
							"/webjars/**"
					);
				});
	}

	private void verifyFilterHasUrlPatterns(AssertableWebApplicationContext context, String... expected) {
		FilterRegistrationBean<SpringDocJsonDecodeFilter> filterRegistration =
				context.getBean("springDocJsonDecodeFilterRegistration", FilterRegistrationBean.class);
		assertThat(filterRegistration.getUrlPatterns()).containsExactlyInAnyOrder(expected);
	}

	private void verifyCustomizerHasIgnorePatterns(AssertableWebApplicationContext context, String... expected) {
		WebSecurityCustomizer customizer = context.getBean("springDocWebSecurityCustomizer", WebSecurityCustomizer.class);
		WebSecurity webSecurity = mock(WebSecurity.class, Answers.RETURNS_DEEP_STUBS);
		customizer.customize(webSecurity);
		ArgumentCaptor<String[]> antMatchersCaptor = ArgumentCaptor.forClass(String[].class);
		verify(webSecurity.ignoring()).requestMatchers(antMatchersCaptor.capture());
		assertThat(antMatchersCaptor.getAllValues()).containsExactly(expected);
	}

	@Test
	void defaultsAreInSyncWithSpringdoc() {
		contextRunnerWithSpringDocEnabled()
				.run((context) -> {
					SpringDocConfigProperties springDocConfigProps = context.getBean(SpringDocConfigProperties.class);
					SwaggerUiConfigProperties swaggerUiConfigProps = context.getBean(SwaggerUiConfigProperties.class);
					assertThat(springDocConfigProps.getApiDocs().getPath()).isEqualTo(Constants.DEFAULT_API_DOCS_URL);
					assertThat(swaggerUiConfigProps.getPath()).isEqualTo(Constants.DEFAULT_SWAGGER_UI_PATH);
					assertThat(swaggerUiConfigProps.getConfigUrl()).isEqualTo(
							Constants.DEFAULT_API_DOCS_URL + AntPathMatcher.DEFAULT_PATH_SEPARATOR + Constants.SWAGGGER_CONFIG_FILE);
					assertThat(swaggerUiConfigProps.getValidatorUrl()).isEqualTo("https://validator.swagger.io/validator");
					assertThat(swaggerUiConfigProps.getOauth2RedirectUrl()).isEqualTo(Constants.SWAGGER_UI_OAUTH_REDIRECT_URL);
				});
	}

	@Test
	void customWebSecurityCustomizerIsRespected() {
		WebSecurityCustomizer customizer = mock(WebSecurityCustomizer.class);
		contextRunnerWithSpringDocEnabled()
				.withBean("springDocWebSecurityCustomizer", WebSecurityCustomizer.class, () -> customizer)
				.run((context) -> assertThat(context).getBean("springDocWebSecurityCustomizer").isSameAs(customizer));
	}

	@Test
	void customFilterRegistrationIsRespected() {
		FilterRegistrationBean<SpringDocJsonDecodeFilter> filterRegistrationBean = mock(FilterRegistrationBean.class);
		contextRunnerWithSpringDocEnabled()
				.withBean("springDocJsonDecodeFilterRegistration", FilterRegistrationBean.class, () -> filterRegistrationBean)
				.run((context) -> assertThat(context).getBean("springDocJsonDecodeFilterRegistration").isSameAs(filterRegistrationBean));
	}

	@Test
	void disabledByDefault() {
		contextRunner.run((context) ->
				assertThat(context).hasNotFailed().doesNotHaveBean(SpringDocAutoConfiguration.class));
	}

	@Test
	void disabledWhenOnlySpringDocsEnabled() {
		contextRunner.withPropertyValues("springdoc.api-docs.enabled=true").run((context) ->
				assertThat(context).hasNotFailed().doesNotHaveBean(SpringDocAutoConfiguration.class));
	}

	@Test
	void disabledWhenOnlySwaggerUiEnabled() {
		contextRunner.withPropertyValues("springdoc.swagger-ui.enabled=true").run((context) ->
				assertThat(context).hasNotFailed().doesNotHaveBean(SpringDocAutoConfiguration.class));
	}

	@Test
	void disabledWhenSpringDocConfigPropsClassNotAvailable() {
		contextRunnerWithSpringDocEnabled()
				.withClassLoader(new FilteredClassLoader(SpringDocConfigProperties.class))
				.run((context) -> assertThat(context).hasNotFailed().doesNotHaveBean(SpringDocAutoConfiguration.class));
	}

	@Test
	void disabledWhenSwaggerUiConfigPropsClassNotAvailable() {
		contextRunnerWithSpringDocEnabled()
				.withClassLoader(new FilteredClassLoader(SwaggerUiConfigProperties.class))
				.run((context) -> assertThat(context).hasNotFailed().doesNotHaveBean(SpringDocAutoConfiguration.class));
	}
}
