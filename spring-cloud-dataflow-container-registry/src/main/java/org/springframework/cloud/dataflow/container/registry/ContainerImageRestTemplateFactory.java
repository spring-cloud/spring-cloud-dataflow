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

package org.springframework.cloud.dataflow.container.registry;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.dataflow.container.registry.authorization.DropAuthorizationHeaderOnSignedS3RequestRedirectStrategy;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * @author Christian Tzolov
 */
public class ContainerImageRestTemplateFactory {

	private final RestTemplateBuilder restTemplateBuilder;

	private final ContainerRegistryProperties properties;

	private final Map<CacheKey, RestTemplate> restTemplateCache;

	private static class CacheKey {
		private final boolean disablesSslVerification;
		private final boolean useHttpProxy;

		public CacheKey(boolean disablesSslVerification, boolean useHttpProxy) {
			this.disablesSslVerification = disablesSslVerification;
			this.useHttpProxy = useHttpProxy;
		}

		static CacheKey of(boolean disablesSslVerification, boolean useHttpProxy) {
			return new CacheKey(disablesSslVerification, useHttpProxy);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CacheKey cacheKey = (CacheKey) o;
			return disablesSslVerification == cacheKey.disablesSslVerification && useHttpProxy == cacheKey.useHttpProxy;
		}

		@Override
		public int hashCode() {
			return Objects.hash(disablesSslVerification, useHttpProxy);
		}
	}

	public ContainerImageRestTemplateFactory(RestTemplateBuilder restTemplateBuilder, ContainerRegistryProperties properties) {
		this.restTemplateBuilder = restTemplateBuilder;
		this.properties = properties;
		this.restTemplateCache = new ConcurrentHashMap();
	}

	public RestTemplate getContainerRestTemplate(boolean skipSslVerification, boolean withHttpProxy) {
		try {
			CacheKey cacheKey = CacheKey.of(skipSslVerification, withHttpProxy);
			if (!this.restTemplateCache.containsKey(cacheKey)) {
				RestTemplate restTemplate = createContainerRestTemplate(skipSslVerification, withHttpProxy);
				this.restTemplateCache.putIfAbsent(cacheKey, restTemplate);
			}
			return this.restTemplateCache.get(cacheKey);
		}
		catch (Exception e) {
			throw new ContainerRegistryException(
					"Failed to create Container Image RestTemplate for disableSsl:"
							+ skipSslVerification + ", httpProxy:" + withHttpProxy, e);
		}
	}

	public RestTemplate createContainerRestTemplate(boolean skipSslVerification, boolean withHttpProxy)
			throws NoSuchAlgorithmException, KeyManagementException {

		if (!skipSslVerification) {
			// Create a RestTemplate that uses custom request factory
			return this.initRestTemplate(HttpClients.custom(), withHttpProxy);
		}

		// Trust manager that blindly trusts all SSL certificates.
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return new X509Certificate[0];
					}

					public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					}

					public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					}
				}
		};
		SSLContext sslContext = SSLContext.getInstance("SSL");
		// Install trust manager to SSL Context.
		sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

		// Create a RestTemplate that uses custom request factory
		return initRestTemplate(
				HttpClients.custom()
						.setSSLContext(sslContext)
						.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE),
				withHttpProxy);
	}

	private RestTemplate initRestTemplate(HttpClientBuilder clientBuilder, boolean withHttpProxy) {
		StringHttpMessageConverter octetToStringMessageConverter = new StringHttpMessageConverter() {
			@Override
			public boolean supports(Class<?> clazz) {
				return true;
			}
		};
		List<MediaType> mediaTypeList = new ArrayList(octetToStringMessageConverter.getSupportedMediaTypes());
		mediaTypeList.add(MediaType.APPLICATION_OCTET_STREAM);
		octetToStringMessageConverter.setSupportedMediaTypes(mediaTypeList);

		clientBuilder.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build());

		// Set the HTTP proxy if configured.
		if (withHttpProxy) {
			HttpHost proxy = new HttpHost(properties.getHttpProxy().getHost(), properties.getHttpProxy().getPort());
			clientBuilder.setProxy(proxy);
		}

		HttpComponentsClientHttpRequestFactory customRequestFactory =
				new HttpComponentsClientHttpRequestFactory(
						clientBuilder
								.setRedirectStrategy(new DropAuthorizationHeaderOnSignedS3RequestRedirectStrategy())
								.build());

		return restTemplateBuilder
				.additionalMessageConverters(octetToStringMessageConverter)
				.requestFactory(() -> customRequestFactory)
				.build();
	}
}
