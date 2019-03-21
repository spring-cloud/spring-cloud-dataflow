/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.dataflow.rest.util;

import java.net.URI;

import org.apache.http.client.HttpClient;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Gunnar Hillert
 * @since 1.4
 */
public class HttpClientConfigurerTests {

	/**
	 * Basic test ensuring that the {@link HttpClient} is built successfully.
	 */
	@Test
	public void testThatHttpClientWithProxyIsCreated() {
		final HttpClientConfigurer builder = HttpClientConfigurer.create();
		builder.withProxyCredentials(URI.create("https://spring.io"), "spring", "cloud");
		builder.buildHttpClient();
	}

	/**
	 * Basic test ensuring that the {@link HttpClient} is built successfully with
	 * null username and password.
	 */
	@Test
	public void testThatHttpClientWithProxyIsCreatedWithNullUsernameAndPassword() {
		final HttpClientConfigurer builder = HttpClientConfigurer.create();
		builder.withProxyCredentials(URI.create("https://spring.io"), null, null);
		builder.buildHttpClient();
	}

	/**
	 * Basic test ensuring that an exception is thrown if the scheme of the proxy
	 * Uri is not set.
	 */
	@Test
	public void testHttpClientWithProxyCreationWithMissingScheme() {
		final HttpClientConfigurer builder = HttpClientConfigurer.create();
		try {
			builder.withProxyCredentials(URI.create("spring"), "spring", "cloud");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("The scheme component of the proxyUri must not be empty.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	/**
	 * Basic test ensuring that an exception is thrown if the proxy
	 * Uri is null.
	 */
	@Test
	public void testHttpClientWithNullProxyUri() {
		final HttpClientConfigurer builder = HttpClientConfigurer.create();
		try {
			builder.withProxyCredentials(null, null, null);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("The proxyUri must not be null.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}
}
