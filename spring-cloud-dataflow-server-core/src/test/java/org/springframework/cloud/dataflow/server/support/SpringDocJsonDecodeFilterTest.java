/*
 * Copyright 2023 the original author or authors.
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

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is a test for {@link SpringDocJsonDecodeFilter} to check if the json content is decoded correctly.
 *
 * @author Tobias Soloschenko
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
class SpringDocJsonDecodeFilterTest {

    private static final String OPENAPI_JSON_ESCAPED_CONTENT = "\"{\\\"openapi:\\\"3.0.1\\\",\\\"info\\\":{\\\"title\\\":\\\"OpenAPI definition\\\",\\\"version\\\":\\\"v0\\\"}}\"";

    private static final String OPENAPI_JSON_UNESCAPED_CONTENT = "{\"openapi:\"3.0.1\",\"info\":{\"title\":\"OpenAPI definition\",\"version\":\"v0\"}}";

	private MockHttpServletResponse mockHttpServletResponse;

	private MockHttpServletRequest mockHttpServletRequest;

	@BeforeEach
	void setup() {
		this.mockHttpServletResponse = new MockHttpServletResponse();
		this.mockHttpServletRequest = new MockHttpServletRequest();
	}

	@Test
	void doFilterTestEscaped() throws ServletException, IOException {

        MockFilterChain mockFilterChain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                response.getOutputStream().write(OPENAPI_JSON_ESCAPED_CONTENT.getBytes(StandardCharsets.UTF_8));
                super.doFilter(request, response);
            }
        };
        new SpringDocJsonDecodeFilter().doFilter(this.mockHttpServletRequest, this.mockHttpServletResponse, mockFilterChain);
        assertThat(this.mockHttpServletResponse.getContentAsString()).isEqualTo(OPENAPI_JSON_UNESCAPED_CONTENT);
    }

	@Test
	void doFilterTestUnEscaped() throws ServletException, IOException {
		MockFilterChain mockFilterChain = new MockFilterChain() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
				response.getOutputStream().write(OPENAPI_JSON_UNESCAPED_CONTENT.getBytes(StandardCharsets.UTF_8));
				super.doFilter(request, response);
			}
		};
		new SpringDocJsonDecodeFilter().doFilter(this.mockHttpServletRequest, this.mockHttpServletResponse, mockFilterChain);
		assertThat(this.mockHttpServletResponse.getContentAsString()).isEqualTo(OPENAPI_JSON_UNESCAPED_CONTENT);
	}

}
