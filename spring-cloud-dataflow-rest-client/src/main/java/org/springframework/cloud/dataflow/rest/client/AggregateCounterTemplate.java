/*
 * Copyright 2016 the original author or authors.
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

import java.util.Date;

import org.joda.time.DateTime;

import org.springframework.analytics.rest.domain.AggregateCounterResource;
import org.springframework.analytics.rest.domain.MetricResource;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Implementation for {@link AggregateCounterOperations} that interacts with the Spring
 * Cloud Data Flow REST API.
 *
 * @author Ilayaperumal Gopinathan
 */
public class AggregateCounterTemplate implements AggregateCounterOperations {

	public static final String AGGREGATE_COUNTER_COLLECTION_RELATION = "aggregate-counters";

	public static final String AGGREGATE_COUNTER_RELATION = "aggregate-counters/counter";

	private final RestTemplate restTemplate;

	private final ResourceSupport links;

	public AggregateCounterTemplate(RestTemplate restTemplate, ResourceSupport resources) {
		this.restTemplate = restTemplate;
		links = resources;
	}

	@Override
	public AggregateCounterResource retrieve(String name, Date from, Date to, Resolution resolution) {
		Assert.notNull(resolution, "Resolution must not be null");
		DateTime fromParam = from == null ? null : new DateTime(from.getTime());
		DateTime toParam = to == null ? null : new DateTime(to.getTime());
		String url = links.getLink(AGGREGATE_COUNTER_RELATION).expand(name).getHref();
		String uriString = UriComponentsBuilder.fromUriString(url)
				.queryParam("resolution", new Object[] { resolution.toString() })
				.queryParam("from", new Object[] { fromParam }).queryParam("to", new Object[] { toParam }).build()
				.toUriString();
		return restTemplate.getForObject(uriString, AggregateCounterResource.class);
	}

	@Override
	public PagedResources<MetricResource> list() {
		return restTemplate.getForObject(links.getLink(AGGREGATE_COUNTER_COLLECTION_RELATION).getHref(),
				MetricResource.Page.class);
	}

	@Override
	public void reset(String name) {
		restTemplate.delete(links.getLink(AGGREGATE_COUNTER_RELATION).expand(name).getHref());
	}
}
