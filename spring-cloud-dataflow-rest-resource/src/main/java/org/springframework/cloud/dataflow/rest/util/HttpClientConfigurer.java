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
package org.springframework.cloud.dataflow.rest.util;

import java.net.URI;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.Assert;

/**
 * Utility for configuring a {@link CloseableHttpClient}. This class allows for
 * chained method invocation. If both basic auth credentials and a target host
 * are provided, the HTTP client will aggressively send the credentials
 * without having to receive an HTTP 401 response first.
 *
 * <p>This class can also be used to configure the client used by
 * {@link org.springframework.web.client.RestTemplate} using
 * {@link #buildClientHttpRequestFactory()}.
 *
 * @author Mike Heath
 */
public class HttpClientConfigurer {

	private final HttpClientBuilder httpClientBuilder;

	private boolean useBasicAuth;
	private HttpHost targetHost;

	public static HttpClientConfigurer create() {
		return new HttpClientConfigurer();
	}

	protected HttpClientConfigurer() {
		httpClientBuilder = HttpClientBuilder.create();
	}

	public HttpClientConfigurer basicAuthCredentials(String username, String password) {
		final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
		httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);

		useBasicAuth = true;

		return this;
	}

	/**
	 * Configures the {@link HttpClientBuilder} with a proxy host. If the
	 * {@code proxyUsername} and {@code proxyPassword} are not {@code null}
	 * then a {@link CredentialsProvider} is also configured for the proxy host.
	 *
	 * @param proxyUri Must not be null and must be configured with a scheme (http or https).
	 * @param proxyUsername May be null
	 * @param proxyPassword May be null
	 * @return a reference to {@code this} to enable chained method invocation
	 */
	public HttpClientConfigurer withProxyCredentials(URI proxyUri, String proxyUsername, String proxyPassword) {

		Assert.notNull(proxyUri, "The proxyUri must not be null.");
		Assert.hasText(proxyUri.getScheme(), "The scheme component of the proxyUri must not be empty.");

		httpClientBuilder
			.setProxy(new HttpHost(proxyUri.getHost(), proxyUri.getPort(), proxyUri.getScheme()));
		if (proxyUsername !=null && proxyPassword != null) {
			final CredentialsProvider proxyCredsProvider = new BasicCredentialsProvider();
			proxyCredsProvider.setCredentials(
				new AuthScope(proxyUri.getHost(), proxyUri.getPort()),
				new UsernamePasswordCredentials(proxyUsername, proxyPassword));
			httpClientBuilder.setDefaultCredentialsProvider(proxyCredsProvider)
				.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
		}
		return this;
	}

	/**
	 * Sets the client's {@link javax.net.ssl.SSLContext} to use
	 * {@link HttpUtils#buildCertificateIgnoringSslContext()}.
	 *
	 * @return a reference to {@code this} to enable chained method invocation
	 */
	public HttpClientConfigurer skipTlsCertificateVerification() {
		httpClientBuilder.setSSLContext(HttpUtils.buildCertificateIgnoringSslContext());
		httpClientBuilder.setSSLHostnameVerifier(new NoopHostnameVerifier());

		return this;
	}

	public HttpClientConfigurer skipTlsCertificateVerification(boolean skipTlsCertificateVerification) {
		if (skipTlsCertificateVerification) {
			skipTlsCertificateVerification();
		}

		return this;
	}

	public HttpClientConfigurer targetHost(URI targetHost) {
		this.targetHost = new HttpHost(targetHost.getHost(), targetHost.getPort(), targetHost.getScheme());

		return this;
	}

	public HttpClientConfigurer addInterceptor(HttpRequestInterceptor interceptor) {
		httpClientBuilder.addInterceptorLast(interceptor);

		return this;
	}

	public CloseableHttpClient buildHttpClient() {
		return httpClientBuilder.build();
	}

	public ClientHttpRequestFactory buildClientHttpRequestFactory() {
		if (useBasicAuth && targetHost != null) {
			return new PreemptiveBasicAuthHttpComponentsClientHttpRequestFactory(buildHttpClient(), targetHost);
		} else {
			return new HttpComponentsClientHttpRequestFactory(buildHttpClient());
		}
	}

}
