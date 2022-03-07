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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.dataflow.server.support.SpringDocJsonDecodeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * Makes SpringDoc public available without any authentication required by initializing a {@link WebSecurityCustomizer} and
 * applying all path of SpringDoc to be ignored. Also applies a filter registration bean to unescape JSON content for the
 * SpringDoc frontend.
 *
 * @author Tobias Soloschenko
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean({ SpringDocConfigProperties.class, SwaggerUiConfigProperties.class })
public class SpringDocAutoConfiguration {

    public static final String SWAGGER_UI_CONTEXT = "/swagger-ui/**";

    private final SpringDocConfigProperties springDocConfigProperties;

    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    /**
     * Creates the SpringDocConfiguration with the given properties.
     *
     * @param springDocConfigProperties the spring doc config properties
     * @param swaggerUiConfigProperties the swagger ui config properties
     */
    public SpringDocAutoConfiguration(SpringDocConfigProperties springDocConfigProperties, SwaggerUiConfigProperties swaggerUiConfigProperties) {
        this.springDocConfigProperties = springDocConfigProperties;
        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
    }

    /**
     * Creates a web security customizer for the spring security which makes the SpringDoc frontend public available.
     *
     * @return a web security customizer with security settings for SpringDoc
     */
    @Bean
    @Qualifier("springDocWebSecurityCustomizer")
    public WebSecurityCustomizer springDocCustomizer() {
        return (webSecurity -> webSecurity.ignoring().antMatchers(
                SWAGGER_UI_CONTEXT,
                getApiDocsPathContext() + "/**",
                swaggerUiConfigProperties.getPath(),
                swaggerUiConfigProperties.getConfigUrl(),
                swaggerUiConfigProperties.getValidatorUrl(),
                swaggerUiConfigProperties.getOauth2RedirectUrl(),
                springDocConfigProperties.getWebjars().getPrefix(),
                springDocConfigProperties.getWebjars().getPrefix() + "/**"));
    }

    /**
     * Applies {@link SpringDocJsonDecodeFilter} to the filter chain which decodes the JSON of ApiDocs and SwaggerUi so that the SpringDoc frontend is able
     * to read it. Spring Cloud Data Flow however requires the JSON to be escaped and wrapped into quotes, because the
     * Angular Ui frontend is using it that way.
     *
     * @return a filter registration bean which unescapes the content of the JSON endpoints of SpringDoc before it is returned.
     */
    @Bean
    @Qualifier("springDocJsonDecodeFilterRegistration")
    public FilterRegistrationBean<SpringDocJsonDecodeFilter> springDocJsonDecodeFilterRegistration() {
        String apiDocsPathContext = getApiDocsPathContext();
        String swaggerUiConfigContext = getSwaggerUiConfigContext();

        FilterRegistrationBean<SpringDocJsonDecodeFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SpringDocJsonDecodeFilter());
        registrationBean.addUrlPatterns(apiDocsPathContext, apiDocsPathContext + "/*", swaggerUiConfigContext,
                swaggerUiConfigContext + "/*");

        return registrationBean;
    }

    /**
     * Gets the SwaggerUi config context. For example the default configuration for the SwaggerUi config is /v3/api-docs/swagger-config
     * which results in a context of /v3/api-docs.
     *
     * @return the SwaggerUi config path context
     */
    private String getSwaggerUiConfigContext() {
        String swaggerUiConfigUrl = swaggerUiConfigProperties.getConfigUrl();
        return swaggerUiConfigUrl.substring(0, swaggerUiConfigUrl.lastIndexOf("/"));
    }

    /**
     * Gets the ApiDocs context path. For example the default configuration for the ApiDocs path is /v3/api-docs
     * which results in a context of /v3.
     *
     * @return the api docs path context
     */
    private String getApiDocsPathContext() {
        String apiDocsPath = springDocConfigProperties.getApiDocs().getPath();
        return apiDocsPath.substring(0, apiDocsPath.lastIndexOf("/"));
    }
}
