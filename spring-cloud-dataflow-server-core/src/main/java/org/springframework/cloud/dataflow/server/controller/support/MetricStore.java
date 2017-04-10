/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.server.controller.support;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.dataflow.server.config.MetricsProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

/**
 * Store implementation returning metrics info from a collector application.
 * Implemented via hystrix command having a fallback to empty response.
 *
 * @author Janne Valkealahti
 *
 */
@Component
public class MetricStore {

	private static Log logger = LogFactory.getLog(MetricStore.class);
	private final RestTemplate restTemplate;
	private final MetricsProperties metricsProperties;
	private final static List<ApplicationsMetrics> EMPTY_RESPONSE = new ArrayList<ApplicationsMetrics>();

	/**
	 * Instantiates a new metric store.
	 *
	 * @param metricsProperties the metrics properties
	 */
	public MetricStore(MetricsProperties metricsProperties) {
		this.metricsProperties = metricsProperties;
		restTemplate = new RestTemplate();
	}

	@HystrixCommand(fallbackMethod = "defaultMetrics")
	public List<ApplicationsMetrics> getMetrics() {
		List<ApplicationsMetrics> metrics = null;
		if (StringUtils.hasText(metricsProperties.getCollector().getUrl())) {
			try {
				metrics = restTemplate.exchange(metricsProperties.getCollector().getUrl(), HttpMethod.GET, null,
						new ParameterizedTypeReference<List<ApplicationsMetrics>>() {
						}).getBody();
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("Error requesting metrics from url " + metricsProperties.getCollector().getUrl(), e);
				}
				throw e;
			}
		} else {
			metrics = defaultMetrics();
		}
		return metrics;
	}

	public List<ApplicationsMetrics> defaultMetrics() {
		return EMPTY_RESPONSE;
	}
}
