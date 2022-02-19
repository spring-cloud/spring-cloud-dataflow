/*
 * Copyright 2016-2018 the original author or authors.
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

import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

public class DataFlowSwaggerApiEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(DataFlowSwaggerApiEnvironmentPostProcessor.class);

    private static final String SPRINGDOC_API_DOCS_ENABLED_KEY = "springdoc.api-docs.enabled";
    private static final String SWAGGER_UI_ENABLED_KEY = "springdoc.swagger-ui.enabled";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        Optional<Object> apiDocsEnabledOptional = environment.getPropertySources().stream()
                .filter(p -> p.containsProperty(SPRINGDOC_API_DOCS_ENABLED_KEY))
                .map(p -> p.getProperty(SPRINGDOC_API_DOCS_ENABLED_KEY))
                .findAny();
        // Apply default properties
        if (!apiDocsEnabledOptional.isPresent()) {
            LOG.debug("Disable springdoc {} feature, because it is not set.", SPRINGDOC_API_DOCS_ENABLED_KEY);
            applyDisableFeaturePropertySources(environment, "apiDocsDisabled", SPRINGDOC_API_DOCS_ENABLED_KEY);
        }

        Optional<Object> swaggerUiEnabledOptional = environment.getPropertySources().stream()
                .filter(p -> p.containsProperty(SWAGGER_UI_ENABLED_KEY))
                .map(p -> p.getProperty(SWAGGER_UI_ENABLED_KEY))
                .findAny();
        if (!(swaggerUiEnabledOptional.isPresent())) {
            LOG.debug("Disable springdoc {} feature, because it is not set.", SWAGGER_UI_ENABLED_KEY);
            applyDisableFeaturePropertySources(environment, "swaggerUiDisabled", SWAGGER_UI_ENABLED_KEY);
        }
    }

    private void applyDisableFeaturePropertySources(ConfigurableEnvironment environment, String name, String key) {
        Properties properties = new Properties();
        properties.setProperty(key, "false");
        PropertiesPropertySource propertiesPropertySource = new PropertiesPropertySource(name, properties);
        environment.getPropertySources().addLast(propertiesPropertySource);
    }
}
