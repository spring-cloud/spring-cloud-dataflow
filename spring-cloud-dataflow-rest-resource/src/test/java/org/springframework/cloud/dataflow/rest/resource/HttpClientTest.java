/*
 * Copyright 2017 the original author or authors.
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

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.rest.util.CheckableResource;
import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.cloud.dataflow.rest.util.ResourceBasedAuthorizationInterceptor;
import org.springframework.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * @author Mike Heath
 */
public class HttpClientTest {

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

		final URI targetHost = new URI("http://test.com");
		assertThatExceptionOfType(Passed.class).isThrownBy(() -> {
			try (final CloseableHttpClient client = HttpClientConfigurer.create(targetHost)
					.addInterceptor(new ResourceBasedAuthorizationInterceptor(resource))
					.addInterceptor((request, context) -> {
						final String authorization = request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue();
						assertThat(authorization).isEqualTo(credentials);

						// Throw an exception to short-circuit making an HTTP request
						throw new Passed();
					})
					.buildHttpClient()) {
				client.execute(new HttpGet(targetHost));
			}
		});
	}

	static final class Passed extends RuntimeException {
	}

	@Test
	public void resourceBasedAuthorizationHeaderResourceCheck() throws Exception {
		final String credentials = "Super Secret Credentials";

		final CheckableResource resource = new ByteArrayCheckableResource(credentials.getBytes(), new TestException());

		final URI targetHost = new URI("http://test.com");
		assertThatExceptionOfType(TestException.class).isThrownBy(() -> {
			try (final CloseableHttpClient client = HttpClientConfigurer.create(targetHost)
					.addInterceptor(new ResourceBasedAuthorizationInterceptor(resource))
					.addInterceptor((request, context) -> {
						final String authorization = request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue();
						assertThat(authorization).isEqualTo(credentials);

						// Throw an exception to short-circuit making an HTTP request
						throw new Passed();
					})
					.buildHttpClient()) {
				client.execute(new HttpGet(targetHost));
			}
		});
	}
}
