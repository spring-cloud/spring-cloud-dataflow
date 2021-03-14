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
package org.springframework.cloud.dataflow.integration.test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.integration.test.util.RuntimeApplicationHelper;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientAutoConfiguration;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientProperties;
import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Provisions RestTemplate (instrumented with skip-ssl-validation and debug interceptors)
 * and RuntimeApplicationHelper.
 *
 * It imports the DataFlowClientAutoConfiguration which extends the provide RestTemplate with authentication support
 * and in turn builds the DataFlowTemplate instance.
 *
 * @author Christian Tzolov
 */
@Configuration
@Import(DataFlowClientAutoConfiguration.class)
@EnableConfigurationProperties({ IntegrationTestProperties.class })
public class DataFlowOperationsITConfiguration {

	@Bean
	public RestTemplate restTemplate(DataFlowClientProperties dataFlowClientProperties) throws URISyntaxException {
		RestTemplate restTemplate = new RestTemplate(HttpClientConfigurer
				.create(new URI(dataFlowClientProperties.getServerUri()))
				.skipTlsCertificateVerification(dataFlowClientProperties.isSkipSslValidation())
				.buildClientHttpRequestFactory());

		restTemplate.setInterceptors(Arrays.asList(new AcceptCharsetInterceptor(), new LoggingInterceptor()));

		return restTemplate;
	}

	@Bean
	public RuntimeApplicationHelper runtimeApplicationHelper(DataFlowTemplate dataFlowOperations,
			IntegrationTestProperties testProperties) {

		return new RuntimeApplicationHelper(dataFlowOperations,
				testProperties.getPlatform().getConnection().getPlatformName());
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
