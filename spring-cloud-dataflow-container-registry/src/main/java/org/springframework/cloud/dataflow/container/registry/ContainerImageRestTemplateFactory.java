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
import java.util.Collections;
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
import org.springframework.cloud.dataflow.container.registry.authorization.DropAuthorizationHeaderRequestRedirectStrategy;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * On demand creates a cacheable {@link RestTemplate} instances for the purpose of the Container Registry access.
 * Created RestTemplates can be configured to use Http Proxy and/or bypassing the SSL verification.
 *
 * For configuring a Http Proxy in need to:
 *  1. Add http proxy configuration using the spring.cloud.dataflow.container.httpProxy.* properties.
 *  2. For every {@link ContainerRegistryConfiguration} that has to interact via the http proxy set the use-http-proxy
 *     flag to true. For example:
 *     <code>spring.cloud.dataflow.container.registry-configurations[reg-name].use-http-proxy=ture</code>
 *
 * Following example configures the default (e.g. DockerHub) registry to use the HTTP Proxy (my-proxy.test:8080)
 * while the dockerhub-mirror and the private-snapshots registry configurations allow direct communication:
 * <code>
 *     spring:
 *   cloud:
 *     dataflow:
 *       container:
 *         httpProxy:
 *           host: my-proxy.test
 *           port: 8080
 *         registry-configurations:
 *           default:
 *             use-http-proxy: true
 *           dockerhub-mirror:
 *             authorization-type: anonymous
 *             registry-host: containerhub.aaa.bbb.ccc:8888
 *           private-snapshots:
 *             authorization-type: anonymous
 *             registry-host: containerhub.aaa.bbb.ccc:7777
 * </code>
 *
 * @author Christian Tzolov
 * @author Cheng Guan Poh
 */
public class ContainerImageRestTemplateFactory {

	private final RestTemplateBuilder restTemplateBuilder;

	private final ContainerRegistryProperties properties;

	/**
	 * Depends on the disablesSslVerification and useHttpProxy a 4 different RestTemplate configurations might be
	 * used at the same time for interacting with different container registries.
	 * The cache map allows reusing the RestTemplates for given useHttpProxy and disablesSslVerification combination.
	 */
	private final ConcurrentHashMap<CacheKey, RestTemplate> restTemplateCache;

	/**
	 * Unique key for any useHttpProxy and disablesSslVerification combination.
	 */
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
		return this.getContainerRestTemplate(skipSslVerification, withHttpProxy, Collections.emptyMap());
	}

	public RestTemplate getContainerRestTemplate(boolean skipSslVerification, boolean withHttpProxy, Map<String, String> extra) {
		try {
			CacheKey cacheKey = CacheKey.of(skipSslVerification, withHttpProxy);
			if (!this.restTemplateCache.containsKey(cacheKey)) {
				RestTemplate restTemplate = createContainerRestTemplate(skipSslVerification, withHttpProxy, extra);
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

	private RestTemplate createContainerRestTemplate(boolean skipSslVerification, boolean withHttpProxy, Map<String, String> extra)
			throws NoSuchAlgorithmException, KeyManagementException {

		if (!skipSslVerification) {
			// Create a RestTemplate that uses custom request factory
			return this.initRestTemplate(HttpClients.custom(), withHttpProxy, extra);
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
				withHttpProxy,
				extra);
	}

	private RestTemplate initRestTemplate(HttpClientBuilder clientBuilder, boolean withHttpProxy, Map<String, String> extra) {

		clientBuilder.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build());

		// Set the HTTP proxy if configured.
		if (withHttpProxy) {
			if (!properties.getHttpProxy().isEnabled()) {
				throw new ContainerRegistryException("Registry Configuration uses a HttpProxy but non is configured!");
			}
			HttpHost proxy = new HttpHost(properties.getHttpProxy().getHost(), properties.getHttpProxy().getPort());
			clientBuilder.setProxy(proxy);
		}

		HttpComponentsClientHttpRequestFactory customRequestFactory =
				new HttpComponentsClientHttpRequestFactory(
						clientBuilder
								.setRedirectStrategy(new DropAuthorizationHeaderRequestRedirectStrategy(extra))
								// Azure redirects may contain double slashes and on default those are normilised
								.setDefaultRequestConfig(RequestConfig.custom().setNormalizeUri(false).build())
								.build());

		// Extend the MappingJackson2HttpMessageConverter to support the following:
		// - DockerHub response media-type is application/octet-stream although the content is in JSON
		// - Github CR response media-type is text/plain although the content is in JSON
		// - quay.io response media-type is binary/octet-stream although the content is in JSON
		MappingJackson2HttpMessageConverter octetSupportJsonConverter = new MappingJackson2HttpMessageConverter();
		ArrayList<MediaType> mediaTypeList = new ArrayList(octetSupportJsonConverter.getSupportedMediaTypes());
		mediaTypeList.add(MediaType.APPLICATION_OCTET_STREAM);
		mediaTypeList.add(MediaType.TEXT_PLAIN);
		mediaTypeList.add(MediaType.parseMediaType("binary/octet-stream")); // quay.io responds with binary/octet-stream instead of application/octet-stream
		octetSupportJsonConverter.setSupportedMediaTypes(mediaTypeList);

		return restTemplateBuilder
				.additionalMessageConverters(octetSupportJsonConverter)
				.requestFactory(() -> customRequestFactory)
				.build();
	}
}
