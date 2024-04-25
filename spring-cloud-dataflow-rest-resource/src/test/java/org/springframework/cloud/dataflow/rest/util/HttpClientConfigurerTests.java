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

import java.lang.reflect.Field;
import java.net.URI;

import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Gunnar Hillert
 * @since 1.4
 */
public class HttpClientConfigurerTests {

	/**
	 * Basic test ensuring that the {@link HttpClient} is built successfully.
	 */
	@Test
	public void testThatHttpClientWithProxyIsCreated() throws Exception {

		final URI targetHost = new URI("http://test.com");
		final HttpClientConfigurer builder = HttpClientConfigurer.create(targetHost);
		builder.withProxyCredentials(URI.create("https://spring.io"), "spring", "cloud");
		builder.buildHttpClient();
	}

	/**
	 * Basic test ensuring that the {@link HttpClient} is built successfully with
	 * null username and password.
	 */
	@Test
	public void testThatHttpClientWithProxyIsCreatedWithNullUsernameAndPassword() throws Exception {
		final URI targetHost = new URI("http://test.com");
		final HttpClientConfigurer builder = HttpClientConfigurer.create(targetHost);
		builder.withProxyCredentials(URI.create("https://spring.io"), null, null);
		builder.buildHttpClient();
	}

	/**
	 * Basic test ensuring that an exception is thrown if the scheme of the proxy
	 * Uri is not set.
	 */
	@Test
	public void testHttpClientWithProxyCreationWithMissingScheme() throws Exception {
		final URI targetHost = new URI("http://test.com");
		final HttpClientConfigurer builder = HttpClientConfigurer.create(targetHost);
		try {
			builder.withProxyCredentials(URI.create("spring"), "spring", "cloud");
		}
		catch (IllegalArgumentException e) {
			assertEquals("The scheme component of the proxyUri must not be empty.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	/**
	 * Basic test ensuring that an exception is thrown if the proxy
	 * Uri is null.
	 */
	@Test
	public void testHttpClientWithNullProxyUri() throws Exception {
		final URI targetHost = new URI("http://test.com");
		final HttpClientConfigurer builder = HttpClientConfigurer.create(targetHost);
		try {
			builder.withProxyCredentials(null, null, null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("The proxyUri must not be null.", e.getMessage());
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown.");
	}

	/**
	 * Test ensuring that the {@link AuthScope} is set for the target host.
	 */
	@Test
	public void testThatHttpClientWithProxyIsCreatedAndHasCorrectCredentialsProviders() throws Exception {
		final URI targetHost = new URI("http://test.com");
		final HttpClientConfigurer builder = HttpClientConfigurer.create(targetHost);
		builder.basicAuthCredentials("foo", "password");
		builder.withProxyCredentials(URI.create("https://spring.io"), null, null);

		final Field credentialsProviderField = ReflectionUtils.findField(HttpClientConfigurer.class, "credentialsProvider");
		ReflectionUtils.makeAccessible(credentialsProviderField);
		CredentialsProvider credentialsProvider = (CredentialsProvider) credentialsProviderField.get(builder);
		assertNotNull(credentialsProvider.getCredentials(new AuthScope("test.com", 80)));
		assertNull(credentialsProvider.getCredentials(new AuthScope("spring.io", 80)));
	}

	/**
	 * Test ensuring that the {@link AuthScope} is set for the target host and the proxy server.
	 */
	@Test
	public void testThatHttpClientWithProxyIsCreatedAndHasCorrectCredentialsProviders2() throws Exception {
		final URI targetHost = new URI("http://test.com");
		final HttpClientConfigurer builder = HttpClientConfigurer.create(targetHost);
		builder.basicAuthCredentials("foo", "password");
		builder.withProxyCredentials(URI.create("https://spring.io"), "proxyuser", "proxypassword");

		final Field credentialsProviderField = ReflectionUtils.findField(HttpClientConfigurer.class, "credentialsProvider");
		ReflectionUtils.makeAccessible(credentialsProviderField);
		CredentialsProvider credentialsProvider = (CredentialsProvider) credentialsProviderField.get(builder);
		assertNotNull(credentialsProvider.getCredentials(new AuthScope("test.com", 80)));
		assertNotNull(credentialsProvider.getCredentials(new AuthScope("spring.io", 80)));
	}
}
