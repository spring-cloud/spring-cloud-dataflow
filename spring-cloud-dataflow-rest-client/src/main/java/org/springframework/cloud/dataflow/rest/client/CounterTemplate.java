/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import org.springframework.analytics.rest.domain.CounterResource;
import org.springframework.analytics.rest.domain.MetricResource;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation for {@link CounterOperations} that interacts with the Spring Cloud Data
 * Flow REST API.
 *
 * @author Eric Bottard
 */
public class CounterTemplate implements CounterOperations {

	public static final String COUNTERS_COLLECTION_RELATION = "counters";

	public static final String COUNTER_RELATION = "counters/counter";

	private final RestTemplate restTemplate;

	private final ResourceSupport links;

	public CounterTemplate(RestTemplate restTemplate, ResourceSupport resources) {
		this.restTemplate = restTemplate;
		links = resources;
	}

	@Override
	public CounterResource retrieve(String name) {
		return restTemplate.getForObject(links.getLink(COUNTER_RELATION).expand(name).getHref(), CounterResource.class);
	}

	@Override
	public PagedResources<MetricResource> list() {
		return restTemplate.getForObject(links.getLink(COUNTERS_COLLECTION_RELATION).getHref(),
				MetricResource.Page.class);
	}

	@Override
	public void reset(String name) {
		restTemplate.delete(links.getLink(COUNTER_RELATION).expand(name).getHref());
	}
}
