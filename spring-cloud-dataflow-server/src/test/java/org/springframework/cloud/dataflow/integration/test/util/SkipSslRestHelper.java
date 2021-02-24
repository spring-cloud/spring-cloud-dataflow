/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.cloud.dataflow.integration.test.util;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

/**
 * @author Christian Tzolov
 */
public class SkipSslRestHelper {

	private SkipSslRestHelper() {

	}

	public static DataFlowTemplate dataFlowTemplate(String dataflowUrl) {
		return new DataFlowTemplate(URI.create(dataflowUrl), restTemplate());
	}

	public static RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
		try {
			httpClientBuilder.setSSLContext(SSLContexts.custom().loadTrustMaterial((chain, authType) -> true).build());
		}
		catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(
				new HttpComponentsClientHttpRequestFactory(httpClientBuilder.build())));
		restTemplate.setInterceptors(Arrays.asList(new AcceptCharsetInterceptor(), new LoggingInterceptor()));
		return restTemplate;
	}

	static class AcceptCharsetInterceptor implements ClientHttpRequestInterceptor {

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {
			request.getHeaders().setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
			return execution.execute(request, body);
		}
	}

	static class LoggingInterceptor implements ClientHttpRequestInterceptor {
		private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
				throws IOException {
			logRequest(request, body);
			ClientHttpResponse response = execution.execute(request, body);
			logResponse(response);
			return response;
		}

		private void logRequest(HttpRequest request, byte[] body) {
			if (log.isDebugEnabled()) {
				log.debug("=========================request begin================================================");
				log.debug("URI         : {}", request.getURI());
				log.debug("Method      : {}", request.getMethod());
				log.debug("Headers     : {}", request.getHeaders());
				log.debug("Request body: {}", new String(body, StandardCharsets.UTF_8));
				log.debug("==========================request end================================================");
			}
		}

		private void logResponse(ClientHttpResponse response) throws IOException {
			if (log.isDebugEnabled()) {
				log.debug("============================response begin==========================================");
				log.debug("Status code  : {}", response.getStatusCode());
				log.debug("Status text  : {}", response.getStatusText());
				log.debug("Headers      : {}", response.getHeaders());
				log.debug("Response body: {}", StreamUtils.copyToString(response.getBody(), Charset.defaultCharset()));
				log.debug("=======================response end=================================================");
			}
		}
	}
}
