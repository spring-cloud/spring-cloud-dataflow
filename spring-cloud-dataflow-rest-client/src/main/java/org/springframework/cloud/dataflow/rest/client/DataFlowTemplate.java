/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.UriTemplate;
import org.springframework.web.client.RestTemplate;

/**
 *  @author Ilayaperumal Gopinathan
 *  @author Mark Fisher
 *  @author Glenn Renfro
 *  @author Patrick Peralta
 *  @author Gary Russell
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
	 * REST client for task operations.
	 */
	private final TaskOperations taskOperations;

	/**
	 * REST client for module operations.
	 */
	private final ModuleOperations moduleOperations;

	/**
	 * REST client for completion operations.
	 */
	private final CompletionOperations completionOperations;


	public DataFlowTemplate(URI baseURI, RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		ResourceSupport resourceSupport = restTemplate.getForObject(baseURI, ResourceSupport.class);
		Link link = getLink(resourceSupport, "streams");
		resources.put("streams/definitions", new UriTemplate(link.getHref() + "/definitions"));
		resources.put("streams/deployments", new UriTemplate(link.getHref() + "/deployments"));
		link = getLink(resourceSupport, "tasks");
		resources.put("tasks/definitions", new UriTemplate(link.getHref() + "/definitions"));
		resources.put("tasks/deployments", new UriTemplate(link.getHref() + "/deployments"));

		this.streamOperations = new StreamTemplate(restTemplate, resources);
		this.counterOperations = new CounterTemplate(restTemplate, resourceSupport);
		this.taskOperations = new TaskTemplate(restTemplate, resources);
		this.moduleOperations = new ModuleTemplate(restTemplate, resourceSupport);
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
	public TaskOperations taskOperations() {
		return taskOperations;
	}

	@Override
	public ModuleOperations moduleOperations() {
		return moduleOperations;
	}

	@Override
	public CompletionOperations completionOperations() {
		return completionOperations;
	}
}
