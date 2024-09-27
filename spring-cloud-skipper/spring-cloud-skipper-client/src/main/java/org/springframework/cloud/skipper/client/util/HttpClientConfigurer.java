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
package org.springframework.cloud.skipper.client.util;

import java.net.URI;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.config.Lookup;
import org.apache.hc.core5.http.config.RegistryBuilder;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * Utility for configuring a {@link CloseableHttpClient}. This class allows for chained
 * method invocation. If both basic auth credentials and a target host are provided, the
 * HTTP client will aggressively send the credentials without having to receive an HTTP
 * 401 response first.
 *
 * <p>
 * This class can also be used to configure the client used by
 * {@link org.springframework.web.client.RestTemplate} using
 * {@link #buildClientHttpRequestFactory()}.
 *
 * @author Mike Heath
 */
public class HttpClientConfigurer {

	private final HttpClientBuilder httpClientBuilder;

	private boolean useBasicAuth;

	private HttpHost targetHost;

	protected HttpClientConfigurer() {
		httpClientBuilder = HttpClientBuilder.create();
	}

	public static HttpClientConfigurer create() {
		return new HttpClientConfigurer();
	}

	public HttpClientConfigurer basicAuthCredentials(String username, String password) {
		final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(new AuthScope(null, null, -1, null, null), new UsernamePasswordCredentials(username, password.toCharArray()));
		httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);

		useBasicAuth = true;

		return this;
	}

	/**
	 * Sets the client's {@link javax.net.ssl.SSLContext} to use
	 * {@link HttpUtils#buildCertificateIgnoringSslContext()}.
	 *
	 * @return a reference to {@code this} to enable chained method invocation
	 */
	public HttpClientConfigurer skipTlsCertificateVerification() {
		Lookup<ConnectionSocketFactory> connSocketFactoryLookup = RegistryBuilder.<ConnectionSocketFactory> create()
			.register("https", new SSLConnectionSocketFactory(HttpUtils.buildCertificateIgnoringSslContext(), NoopHostnameVerifier.INSTANCE))
			.register("http", new PlainConnectionSocketFactory())
			.build();
		httpClientBuilder.setConnectionManager(new BasicHttpClientConnectionManager(connSocketFactoryLookup));
		return this;
	}

	public HttpClientConfigurer skipTlsCertificateVerification(boolean skipTlsCertificateVerification) {
		if (skipTlsCertificateVerification) {
			skipTlsCertificateVerification();
		}

		return this;
	}

	public HttpClientConfigurer targetHost(URI targetHost) {
		this.targetHost = new HttpHost(targetHost.getScheme(), targetHost.getHost(), targetHost.getPort());
		return this;
	}

	public HttpClientConfigurer addInterceptor(HttpRequestInterceptor interceptor) {
		httpClientBuilder.addRequestInterceptorLast(interceptor);

		return this;
	}

	public CloseableHttpClient buildHttpClient() {
		return httpClientBuilder.build();
	}

	public ClientHttpRequestFactory buildClientHttpRequestFactory() {
		if (useBasicAuth && targetHost != null) {
			return new PreemptiveBasicAuthHttpComponentsClientHttpRequestFactory(buildHttpClient(), targetHost);
		}
		else {
			return new HttpComponentsClientHttpRequestFactory(buildHttpClient());
		}
	}

}
