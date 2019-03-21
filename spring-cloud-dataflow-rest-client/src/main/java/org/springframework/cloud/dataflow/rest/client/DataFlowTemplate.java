/*
 * Copyright 2015-2016 the original author or authors.
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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.UriTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of DataFlowOperations delegating to sub-templates, discovered
 * via REST relations.
 *
 *  @author Ilayaperumal Gopinathan
 *  @author Mark Fisher
 *  @author Glenn Renfro
 *  @author Patrick Peralta
 *  @author Gary Russell
 *  @author Eric Bottard
 */
public class DataFlowTemplate implements DataFlowOperations {

	/**
	 * A template used for http interaction.
	 */
	protected final RestTemplate restTemplate;

	/**
	 * Holds discovered URLs of the API.
	 */
	protected final Map<String, UriTemplate> resources = new HashMap<String, UriTemplate>();

	/**
	 * REST client for stream operations.
	 */
	private final StreamOperations streamOperations;

	/**
	 * REST client for counter operations.
	 */
	private final CounterOperations counterOperations;

	/**
	 * REST client for field value counter operations.
	 */
	private final FieldValueCounterOperations fieldValueCounterOperations;

	/**
	 * REST client for aggregate counter operations.
	 */
	private final AggregateCounterOperations aggregateCounterOperations;

	/**
	 * REST client for task operations.
	 */
	private final TaskOperations taskOperations;

	/**
	 * REST client for job operations.
	 */
	private final JobOperations jobOperations;

	/**
	 * REST client for app registry operations.
	 */
	private final AppRegistryOperations appRegistryOperations;

	/**
	 * REST client for completion operations.
	 */
	private final CompletionOperations completionOperations;

	/**
	 * REST Client for runtime operations.
	 */
	private final RuntimeOperations runtimeOperations;


	public DataFlowTemplate(URI baseURI, RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		ResourceSupport resourceSupport = restTemplate.getForObject(baseURI, ResourceSupport.class);
		if (resourceSupport.hasLink(StreamTemplate.DEFINITIONS_REL)) {
			this.streamOperations = new StreamTemplate(restTemplate, resourceSupport);
			this.runtimeOperations = new RuntimeTemplate(restTemplate, resourceSupport);
		}
		else {
			this.streamOperations = null;
			this.runtimeOperations = null;
		}
		if (resourceSupport.hasLink(CounterTemplate.COUNTER_RELATION)) {
			this.counterOperations = new CounterTemplate(restTemplate, resourceSupport);
			this.fieldValueCounterOperations = new FieldValueCounterTemplate(restTemplate, resourceSupport);
			this.aggregateCounterOperations = new AggregateCounterTemplate(restTemplate, resourceSupport);
		}
		else {
			this.counterOperations = null;
			this.fieldValueCounterOperations = null;
			this.aggregateCounterOperations = null;
		}
		if (resourceSupport.hasLink(TaskTemplate.DEFINITIONS_RELATION)) {
			this.taskOperations = new TaskTemplate(restTemplate, resourceSupport);
			this.jobOperations = new JobTemplate(restTemplate, resourceSupport);
		}
		else {
			this.taskOperations = null;
			this.jobOperations = null;
		}
		this.appRegistryOperations = new AppRegistryTemplate(restTemplate, resourceSupport);
		this.completionOperations = new CompletionTemplate(restTemplate, resourceSupport.getLink("completions/stream"));
	}

	public Link getLink(ResourceSupport resourceSupport, String rel) {
		Link link = resourceSupport.getLink(rel);
		if (link == null) {
			throw new DataFlowServerException("Server did not return a link for '" + rel + "', links: '"
					+ resourceSupport + "'");
		}
		return link;
	}

	@Override
	public StreamOperations streamOperations() {
		return streamOperations;
	}

	@Override
	public CounterOperations counterOperations() {
		return counterOperations;
	}

	@Override
	public FieldValueCounterOperations fieldValueCounterOperations() {
		return fieldValueCounterOperations;
	}

	@Override
	public AggregateCounterOperations aggregateCounterOperations() {
		return aggregateCounterOperations;
	}

	@Override
	public TaskOperations taskOperations() {
		return taskOperations;
	}

	@Override
	public JobOperations jobOperations() {
		return jobOperations;
	}

	@Override
	public AppRegistryOperations appRegistryOperations() {
		return appRegistryOperations;
	}

	@Override
	public CompletionOperations completionOperations() {
		return completionOperations;
	}

	@Override
	public RuntimeOperations runtimeOperations() {
		return runtimeOperations;
	}
}
