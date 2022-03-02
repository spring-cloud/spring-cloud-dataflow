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

package org.springframework.cloud.dataflow.server.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.dataflow.server.config.SpringDocConfigurationProperties;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;

public class SpringDocJsonDecodeFilterTest {

    private SpringDocJsonDecodeFilter springDocJsonDecodeFilter;

    private SpringDocConfigurationProperties springDocConfigurationProperties;

    private static final String OPENAPI_JSON_ESCAPED_CONTENT = "\"{\\\"openapi:\\\"3.0.1\\\",\\\"info\\\":{\\\"title\\\":\\\"OpenAPI definition\\\",\\\"version\\\":\\\"v0\\\"}}\"";

    private static final String OPENAPI_JSON_UNESCAPED_CONTENT = "{\"openapi:\"3.0.1\",\"info\":{\"title\":\"OpenAPI definition\",\"version\":\"v0\"}}";

    @Before
    public void setup() {
        springDocConfigurationProperties = new SpringDocConfigurationProperties();
        springDocConfigurationProperties.setApiDocs(new SpringDocConfigurationProperties.ApiDocs());
        springDocConfigurationProperties.setSwaggerUi(new SpringDocConfigurationProperties.SwaggerUi());
        springDocJsonDecodeFilter = new SpringDocJsonDecodeFilter(springDocConfigurationProperties);
    }

    @Test
    public void doFilterTest() throws ServletException, IOException {
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setServletPath(springDocConfigurationProperties.getApiDocs().getPath());
        MockFilterChain mockFilterChain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                response.getOutputStream().write(OPENAPI_JSON_ESCAPED_CONTENT.getBytes(StandardCharsets.UTF_8));
                super.doFilter(request, response);
            }
        };
        springDocJsonDecodeFilter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);
        assertEquals(OPENAPI_JSON_UNESCAPED_CONTENT, mockHttpServletResponse.getContentAsString());
    }

}
