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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Sets up a filter that unescapes the JSON content of the API endpoints of OpenApi and Swagger.
 * This is similar to the issue mentioned here: https://github.com/springdoc/springdoc-openapi/issues/624
 * Spring Cloud Data Flow however needs the escaped JSON to show task logs in the UI.
 *
 * @author Tobias Soloschenko
 */
public class SpringDocJsonDecodeFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(SpringDocJsonDecodeFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequestWrapper httpServletRequestWrapper = new HttpServletRequestWrapper((HttpServletRequest) request);
        final ContentCachingResponseWrapper httpServletResponseWrapper = new ContentCachingResponseWrapper((HttpServletResponse) response);

        chain.doFilter(httpServletRequestWrapper, httpServletResponseWrapper);

        ServletOutputStream outputStream = httpServletResponseWrapper.getResponse().getOutputStream();

        LOG.debug("Request for Swagger api-docs detected - unescaping json content.");
        String content = new String(httpServletResponseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        // Replaces all escaped quotes
        content = StringEscapeUtils.unescapeJson(content);
        // Replaces first and last quote
		if (StringUtils.hasText(content) && content.startsWith("\"") && content.endsWith("\"")) {
			content = content.substring(1, content.length() - 1);
		}
        if (LOG.isTraceEnabled()) {
            LOG.trace("Using decoded JSON for serving api-docs: {}", content);
        }
        outputStream.write(content.getBytes(StandardCharsets.UTF_8));

    }
}
