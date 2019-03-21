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

package org.springframework.cloud.dataflow.server.controller.support;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.cloud.dataflow.server.config.MetricsProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


/**
 * Store implementation returning metrics info from a collector application. Implemented
 * via hystrix command having a fallback to empty response.
 *
 * @author Janne Valkealahti
 */
public class MetricStore {

	private final static List<ApplicationsMetrics> EMPTY_RESPONSE = new ArrayList<ApplicationsMetrics>();

	private static Log logger = LogFactory.getLog(MetricStore.class);

	private final RestTemplate restTemplate;

	private final MetricsProperties metricsProperties;

	private String collectorEndpoint;

	/**
	 * Instantiates a new metric store.
	 *
	 * @param metricsProperties the metrics properties
	 */
	public MetricStore(MetricsProperties metricsProperties) {
		this.metricsProperties = metricsProperties;
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.registerModule(new Jackson2HalModule());
		MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
		messageConverter.setSupportedMediaTypes(MediaType.parseMediaTypes("application/hal+json"));
		messageConverter.setObjectMapper(mapper);
		restTemplate = new RestTemplate(Arrays.asList(messageConverter));
		final MetricsProperties.Collector collector = metricsProperties.getCollector();
		String baseURI = collector.getUri();
		if (StringUtils.hasText(baseURI)) {
			try {
				URI uri = new URI(baseURI);
				this.collectorEndpoint = UriComponentsBuilder.fromUri(uri).path("/collector/metrics/streams").build()
						.toString();
				logger.info("Metrics Collector URI = [" + collectorEndpoint + "]");
				validateUsernamePassword(collector.getUsername(),
						collector.getPassword());
				if (StringUtils.hasText(collector.getUsername())
						&& StringUtils.hasText(collector.getPassword())) {
					this.restTemplate.setRequestFactory(HttpClientConfigurer.create(new URI(collectorEndpoint))
							.basicAuthCredentials(collector.getUsername(), collector.getPassword())
							.skipTlsCertificateVerification(collector.isSkipSslValidation())
							.buildClientHttpRequestFactory());
					logger.debug("Configured basic security for Metrics Collector endpoint");
				}
				else {
					logger.debug("Not configuring basic security for Metrics Collector endpoint");
				}
			}
			catch (URISyntaxException e) {
				logger.warn("Could not parse collector URI, stream metrics monitoring will not be available");
			}
		}
		else {
			logger.info("Metrics Collector URI = []");
		}
	}

	@HystrixCommand(fallbackMethod = "defaultMetrics")
	public List<ApplicationsMetrics> getMetrics() {
		List<ApplicationsMetrics> metrics = null;
		if (StringUtils.hasText(this.collectorEndpoint)) {
			try {
				PagedResources<ApplicationsMetrics> response = restTemplate.exchange(this.collectorEndpoint,
						HttpMethod.GET, null, new ParameterizedTypeReference<PagedResources<ApplicationsMetrics>>() {
						}).getBody();
				metrics = new ArrayList<>(response.getContent());
				if (logger.isDebugEnabled()) {
					logger.debug("Metrics = " + metrics);
				}
			}
			catch (Exception e) {
				if (e instanceof HttpClientErrorException && e.getMessage().startsWith("401")) {
					logger.warn(String.format(
							"Failure while requesting metrics from url '%s': '%s'. "
									+ "Unauthorized, please provide valid credentials.",
							this.collectorEndpoint, e.getMessage()));
				}
				else {
					logger.warn(String.format("Failure while requesting metrics from url '%s': %s",
							this.collectorEndpoint, e.getMessage()));
				}
				if (logger.isDebugEnabled()) {
					logger.debug("The metrics request failed with:", e);
				}
				throw e;
			}
		}
		else {
			metrics = defaultMetrics();
		}
		return metrics;
	}

	public List<ApplicationsMetrics> defaultMetrics() {
		return EMPTY_RESPONSE;
	}

	private void validateUsernamePassword(String userName, String password) {
		if (!StringUtils.isEmpty(password) && StringUtils.isEmpty(userName)) {
			logger.warn("A password may be specified only together with a username");
		}

		if (StringUtils.isEmpty(password) && !StringUtils.isEmpty(userName)) {
			logger.warn("A username may be specified only together with a password");
		}
	}
}
