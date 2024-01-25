/*
 * Copyright 2017-2018 the original author or authors.
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

import org.apache.hc.client5.http.impl.DefaultAuthenticationStrategy;
import org.apache.hc.client5.http.impl.auth.SystemDefaultCredentialsProvider;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;

import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
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
 * @author Gunnar Hillert
 */
public class HttpClientConfigurer {

	private final HttpClientBuilder httpClientBuilder;

	private boolean useBasicAuth;
	private HttpHost targetHost;

	private CredentialsProvider credentialsProvider;

	public static HttpClientConfigurer create(URI targetHost) {
		return new HttpClientConfigurer(targetHost);
	}

	protected HttpClientConfigurer(URI targetHost) {
		httpClientBuilder = HttpClientBuilder.create();
		this.targetHost = new HttpHost(targetHost.getScheme(), targetHost.getHost(), targetHost.getPort());
	}

	public HttpClientConfigurer basicAuthCredentials(String username, String password) {
		final CredentialsProvider credentialsProvider = this.getOrInitializeCredentialsProvider();
		if(credentialsProvider instanceof BasicCredentialsProvider basicCredentialsProvider) {
			basicCredentialsProvider.setCredentials(new AuthScope(this.targetHost),
				new UsernamePasswordCredentials(username, password.toCharArray()));
		} else if (credentialsProvider instanceof SystemDefaultCredentialsProvider systemDefaultCredProvider) {
			systemDefaultCredProvider.setCredentials(new AuthScope(this.targetHost),
				new UsernamePasswordCredentials(username, password.toCharArray()));
		}
		httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
		useBasicAuth = true;
		return this;
	}

	private CredentialsProvider getOrInitializeCredentialsProvider() {
		if (this.credentialsProvider == null) {
			this.credentialsProvider = new BasicCredentialsProvider();
		}
		return this.credentialsProvider;
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
			.setProxy(new HttpHost(proxyUri.getScheme(), proxyUri.getHost(), proxyUri.getPort()));
		if (proxyUsername !=null && proxyPassword != null) {
			final CredentialsProvider credentialsProvider = this.getOrInitializeCredentialsProvider();
			if(credentialsProvider instanceof BasicCredentialsProvider basicCredentialsProvider) {
				basicCredentialsProvider.setCredentials(new AuthScope(proxyUri.getHost(), proxyUri.getPort()),
					new UsernamePasswordCredentials(proxyUsername, proxyPassword.toCharArray()));
			} else if (credentialsProvider instanceof SystemDefaultCredentialsProvider systemDefaultCredProvider) {
				systemDefaultCredProvider.setCredentials(new AuthScope(proxyUri.getHost(), proxyUri.getPort()),
					new UsernamePasswordCredentials(proxyUsername, proxyPassword.toCharArray()));
			}
			httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
				.setProxyAuthenticationStrategy(new DefaultAuthenticationStrategy());
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
		ConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(HttpUtils.buildCertificateIgnoringSslContext(), NoopHostnameVerifier.INSTANCE);
			Registry<ConnectionSocketFactory> socketFactoryRegistry =
				RegistryBuilder.<ConnectionSocketFactory> create()
					.register("https", sslsf)
					.register("http", new PlainConnectionSocketFactory())
					.build();
			final BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(socketFactoryRegistry);
			httpClientBuilder.setConnectionManager(connectionManager);

		return this;
	}

	public HttpClientConfigurer skipTlsCertificateVerification(boolean skipTlsCertificateVerification) {
		if (skipTlsCertificateVerification) {
			skipTlsCertificateVerification();
		}

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
		} else {
			return new HttpComponentsClientHttpRequestFactory(buildHttpClient());
		}
	}

}
