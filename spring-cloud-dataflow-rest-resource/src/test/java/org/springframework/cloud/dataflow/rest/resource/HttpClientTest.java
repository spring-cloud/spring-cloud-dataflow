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


import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.cloud.dataflow.rest.util.ResourceBasedAuthorizationInterceptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 *
 * @author Mike Heath
 */
public class HttpClientTest {

	@Test(expected = Passed.class)
	public void resourceBasedAuthorizationHeader() throws Exception {
		final String credentials = "Super Secret Credentials";

		final Resource resource = new ByteArrayResource(credentials.getBytes());

		try (final CloseableHttpClient client = HttpClientConfigurer.create()
				.addInterceptor(new ResourceBasedAuthorizationInterceptor(resource))
				.addInterceptor((request, context) -> {
					final String authorization = request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue();
					Assertions.assertThat(authorization).isEqualTo(credentials);

					// Throw an exception to short-circuit making an HTTP request
					throw new Passed();
				})
				.buildHttpClient()) {
			client.execute(new HttpGet("http://test.com"));
		}
	}

	static final class Passed extends RuntimeException {}

}
