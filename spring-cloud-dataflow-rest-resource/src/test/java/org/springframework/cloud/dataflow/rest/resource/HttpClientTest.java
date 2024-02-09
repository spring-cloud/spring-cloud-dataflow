/*
 * Copyright 2017-2023 the original author or authors.
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
package org.springframework.cloud.dataflow.rest.resource;


import java.io.IOException;
import java.net.URI;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.cloud.dataflow.rest.util.CheckableResource;
import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.cloud.dataflow.rest.util.ResourceBasedAuthorizationInterceptor;
import org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
/**
 * @author Mike Heath
 * @author Corneil du Plessis
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = HttpClientTest.HttpClientTestApp.class)
public class HttpClientTest {

	private static final Logger logger = LoggerFactory.getLogger(HttpClientTest.class);

	@Autowired
	private ServletWebServerApplicationContext webServerAppCtxt;
	static final class TestException extends IOException {
		TestException() {
			super("It broke");
		}
	}

	static final class ByteArrayCheckableResource extends ByteArrayResource implements CheckableResource {
		private final IOException exception;

		ByteArrayCheckableResource(byte[] byteArray, IOException exc) {
			super(byteArray);
			exception = exc;
		}

		@Override
		public void check() throws IOException {
			if (exception != null) {
				throw exception;
			}
		}
	}

	@Test
	public void resourceBasedAuthorizationHeader() throws Exception {
		final String credentials = "Super Secret Credentials";

		final CheckableResource resource = new ByteArrayCheckableResource(credentials.getBytes(), null);

		final URI targetHost = new URI("http://localhost:" + webServerAppCtxt.getWebServer().getPort());
		try (final CloseableHttpClient client = HttpClientConfigurer.create(targetHost)
				.addInterceptor(new ResourceBasedAuthorizationInterceptor(resource))
				.addInterceptor((request, entityDetails, context) -> {
					final String authorization = request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue();
				assertThat(authorization).isEqualTo(credentials);

					// Throw an exception to short-circuit making an HTTP request
					throw new Passed();
				})
				.buildHttpClient()) {
			logger.info("executing:GET:{}", targetHost);
			assertThatThrownBy(() -> client.execute(new HttpGet(targetHost))).isInstanceOf(Passed.class);

		}
	}

	static final class Passed extends RuntimeException {
	}

	@Test
	public void resourceBasedAuthorizationHeaderResourceCheck() throws Exception {
		final String credentials = "Super Secret Credentials";

		final CheckableResource resource = new ByteArrayCheckableResource(credentials.getBytes(), new TestException());

		final URI targetHost = new URI("http://localhost:" + webServerAppCtxt.getWebServer().getPort());
		try (final CloseableHttpClient client = HttpClientConfigurer.create(targetHost)
				.addInterceptor(new ResourceBasedAuthorizationInterceptor(resource))
				.addInterceptor((request, entityDetails, context) -> {
					final String authorization = request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue();
				assertThat(authorization).isEqualTo(credentials);

					// Throw an exception to short-circuit making an HTTP request
					throw new Passed();
				})
				.buildHttpClient()) {
			logger.info("executing:GET:{}", targetHost);
			assertThatThrownBy(() -> client.execute(new HttpGet(targetHost))).isInstanceOf(TestException.class);

		}
	}

	@SpringBootApplication(exclude = { SimpleTaskAutoConfiguration.class })
	static class HttpClientTestApp {

		@RestController
		static class TestController {

			@GetMapping("/")
			public String home() {
				return "Hello World";
			}

		}

	}
}
