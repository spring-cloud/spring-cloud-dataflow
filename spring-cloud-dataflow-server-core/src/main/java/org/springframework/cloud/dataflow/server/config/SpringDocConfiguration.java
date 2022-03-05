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

package org.springframework.cloud.dataflow.server.config;

import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SwaggerUiConfigProperties;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.dataflow.server.support.SpringDocJsonDecodeFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * Makes SpringDoc public available without any authentication required by initializing a {@link WebSecurityCustomizer} and
 * applying all path of SpringDoc to be ignored.
 *
 * @author Tobias Soloschenko
 */
@Configuration(proxyBeanMethods = false)
public class SpringDocConfiguration {

    public static final String SWAGGER_UI_CONTEXT = "/swagger-ui/**";

    private final SpringDocConfigProperties springDocConfigProperties;

    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    public SpringDocConfiguration(SpringDocConfigProperties springDocConfigProperties, SwaggerUiConfigProperties swaggerUiConfigProperties) {
        this.springDocConfigProperties = springDocConfigProperties;
        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
    }

    @Bean
    public WebSecurityCustomizer springDocCustomizer() {
        return (webSecurity -> {
            String apiDocsPath = springDocConfigProperties.getApiDocs().getPath();
            String apiDocsPathContext = apiDocsPath.substring(0, apiDocsPath.lastIndexOf("/"));

            webSecurity.ignoring().antMatchers(
                    SWAGGER_UI_CONTEXT,
                    apiDocsPathContext + "/**",
                    swaggerUiConfigProperties.getPath(),
                    swaggerUiConfigProperties.getConfigUrl(),
                    swaggerUiConfigProperties.getValidatorUrl(),
                    swaggerUiConfigProperties.getOauth2RedirectUrl(),
                    springDocConfigProperties.getWebjars().getPrefix(),
                    springDocConfigProperties.getWebjars().getPrefix() + "/**");
        });
    }

    @Bean
    public FilterRegistrationBean<SpringDocJsonDecodeFilter> jsonDecodeFilterRegistration() {
        String apiDocsPath = springDocConfigProperties.getApiDocs().getPath();
        String swaggerUiConfigUrl = swaggerUiConfigProperties.getConfigUrl();
        String apiDocsPathContext = apiDocsPath.substring(0, apiDocsPath.lastIndexOf("/"));
        String swaggerUiConfigContext = swaggerUiConfigUrl.substring(0, swaggerUiConfigUrl.lastIndexOf("/"));

        FilterRegistrationBean<SpringDocJsonDecodeFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SpringDocJsonDecodeFilter());
        registrationBean.addUrlPatterns(apiDocsPathContext, apiDocsPathContext + "/*", swaggerUiConfigContext, swaggerUiConfigContext + "/*");

        return registrationBean;
    }
}
