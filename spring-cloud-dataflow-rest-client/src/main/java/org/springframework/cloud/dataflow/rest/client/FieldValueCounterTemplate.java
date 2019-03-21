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

import org.springframework.cloud.dataflow.rest.resource.FieldValueCounterResource;
import org.springframework.cloud.dataflow.rest.resource.MetricResource;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation for {@link FieldValueCounterOperations} that interacts with the Spring Cloud Data Flow REST API.
 *
 * @author Eric Bottard
 */
public class FieldValueCounterTemplate implements FieldValueCounterOperations {

	public static final String FVC_COLLECTION_RELATION = "field-value-counters";

	public static final String FVC_RELATION = "field-value-counters/counter";

	private final RestTemplate restTemplate;

	private final ResourceSupport links;

	public FieldValueCounterTemplate(RestTemplate restTemplate, ResourceSupport resources) {
		this.restTemplate = restTemplate;
		links = resources;
	}

	@Override
	public FieldValueCounterResource retrieve(String name) {
		return restTemplate.getForObject(links.getLink(FVC_RELATION).expand(name).getHref(), FieldValueCounterResource.class);
	}

	@Override
	public PagedResources<MetricResource> list() {
		return restTemplate.getForObject(links.getLink(FVC_COLLECTION_RELATION).getHref(), MetricResource.Page.class);
	}

	@Override
	public void reset(String name) {
		restTemplate.delete(links.getLink(FVC_RELATION).expand(name).getHref());
	}
}
