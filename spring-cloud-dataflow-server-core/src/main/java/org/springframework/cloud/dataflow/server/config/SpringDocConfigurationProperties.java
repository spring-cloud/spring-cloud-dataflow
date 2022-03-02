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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Properties to read the spring doc configuration and provides it to some Spring Cloud Data Flow Server
 * classes.
 */
@Configuration
@ConfigurationProperties(prefix = "springdoc")
public class SpringDocConfigurationProperties {

    public static final String SWAGGER_UI_CONTEXT = "/swagger-ui/**";

    private Webjars webjars;

    private ApiDocs apiDocs;

    private SwaggerUi swaggerUi;

    public static class Webjars {
        private String prefix = "/webjars";

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }

    public static class ApiDocs {
        private String path = "/v3/api-docs";

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class SwaggerUi {
        private String path = "/swagger-ui.html";

        private String configUrl = "/v3/api-docs/swagger-config";

        private String validatorUrl = "validator.swagger.io/validator";

        private String oauth2RedirectUrl = "/swagger-ui/oauth2-redirect.html";

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getConfigUrl() {
            return configUrl;
        }

        public void setConfigUrl(String configUrl) {
            this.configUrl = configUrl;
        }

        public String getValidatorUrl() {
            return validatorUrl;
        }

        public void setValidatorUrl(String validatorUrl) {
            this.validatorUrl = validatorUrl;
        }

        public String getOauth2RedirectUrl() {
            return oauth2RedirectUrl;
        }

        public void setOauth2RedirectUrl(String oauth2RedirectUrl) {
            this.oauth2RedirectUrl = oauth2RedirectUrl;
        }
    }

    public Webjars getWebjars() {
        return webjars;
    }

    public void setWebjars(Webjars webjars) {
        this.webjars = webjars;
    }

    public ApiDocs getApiDocs() {
        return apiDocs;
    }

    public void setApiDocs(ApiDocs apiDocs) {
        this.apiDocs = apiDocs;
    }

    public SwaggerUi getSwaggerUi() {
        return swaggerUi;
    }

    public void setSwaggerUi(SwaggerUi swaggerUi) {
        this.swaggerUi = swaggerUi;
    }
}
