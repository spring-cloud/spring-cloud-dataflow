/*
 * Copyright 2017-2024 the original author or authors.
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
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.dataflow.rest.util.CheckableResource;
import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.cloud.dataflow.rest.util.ResourceBasedAuthorizationInterceptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * @author Mike Heath
 * @author Corneil du Plessis
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = HttpClientTest.HttpClientTestApp.class)
class HttpClientTest {

	@LocalServerPort
	private int port;

	@Test
	void resourceBasedAuthorizationHeader() throws Exception {
		var credentials = "Super Secret Credentials";
		var resource = new ByteArrayCheckableResource(credentials.getBytes(), null);
		var targetHost = new URI("http://localhost:" + port);
		try (var client = HttpClientConfigurer.create(targetHost)
				.addInterceptor(new ResourceBasedAuthorizationInterceptor(resource))
				.addInterceptor((request, entityDetails, context) -> {
					var authorization = request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue();
					assertThat(authorization).isEqualTo(credentials);
					// Throw an exception to short-circuit making an HTTP request
					throw new Passed();
				})
				.buildHttpClient()) {
			assertThatExceptionOfType(Passed.class).isThrownBy(() -> client.execute(new HttpGet(targetHost)));
		}
	}

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

	static final class Passed extends RuntimeException {
	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
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
