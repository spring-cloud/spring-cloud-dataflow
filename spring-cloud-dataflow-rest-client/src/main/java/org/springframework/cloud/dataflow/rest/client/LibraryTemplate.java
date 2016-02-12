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

import org.springframework.cloud.dataflow.rest.resource.LibraryRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.ModuleRegistrationResource;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.UriTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of {@link LibraryOperations} that uses {@link RestTemplate}
 * to issue commands to the Data Flow server.
 * @author Eric Bottard
 */
public class LibraryTemplate implements LibraryOperations {

	/**
	 * Template used for http interaction.
	 */
	protected RestTemplate restTemplate;

	/**
	 * Template for URI creation.
	 */
	private final UriTemplate uriTemplate;

	/**
	 * Construct a {@code LibraryTemplate} object.
	 * @param restTemplate    template for HTTP/rest commands
	 * @param resourceSupport HATEOAS link support
	 */
	public LibraryTemplate(RestTemplate restTemplate, ResourceSupport resourceSupport) {
		this.restTemplate = restTemplate;
		this.uriTemplate = new UriTemplate(resourceSupport.getLink("libraries").getHref());
	}

	@Override
	public PagedResources<LibraryRegistrationResource> list() {
		String uri = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uri, LibraryRegistrationResource.Page.class);
	}

	@Override
	public void unregister(String name) {
		String uri = uriTemplate.toString() + "/{name}";
		restTemplate.delete(uri, name);
	}

	@Override
	public LibraryRegistrationResource info(String name) {
		String uri = uriTemplate.toString() + "/{name}";
		return restTemplate.getForObject(uri, LibraryRegistrationResource.class, name);
	}

	@Override
	public ModuleRegistrationResource register(String name,
	                                           String coordinates, boolean force) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("coordinates", coordinates);
		values.add("force", Boolean.toString(force));

		String uri = uriTemplate.toString() + "/{name}";
		return restTemplate.postForObject(uri, values, LibraryRegistrationResource.class, name);
	}
}
