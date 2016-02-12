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

import org.springframework.cloud.dataflow.core.ArtifactType;
import org.springframework.cloud.dataflow.rest.resource.DetailedModuleRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.ModuleRegistrationResource;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.UriTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of {@link ModuleOperations} that uses {@link RestTemplate}
 * to issue commands to the Data Flow server.
 *
 * @author Eric Bottard
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Patrick Peralta
 */
public class ModuleTemplate implements ModuleOperations {

	/**
	 * Template used for http interaction.
	 */
	protected RestTemplate restTemplate;

	/**
	 * Template for URI creation.
	 */
	private final UriTemplate uriTemplate;

	/**
	 * Construct a {@code ModuleTemplate} object.
	 *
	 * @param restTemplate template for HTTP/rest commands
	 * @param resourceSupport HATEOAS link support
	 */
	public ModuleTemplate(RestTemplate restTemplate, ResourceSupport resourceSupport) {
		this.restTemplate = restTemplate;
		this.uriTemplate = new UriTemplate(resourceSupport.getLink("modules").getHref());
	}

	@Override
	public PagedResources<ModuleRegistrationResource> list() {
		return list(/* ArtifactType */null);
	}

	@Override
	public PagedResources<ModuleRegistrationResource> list(ArtifactType type) {
		String uri = uriTemplate + "?size=10000" + ((type == null) ? "" : "&type=" + type.name());
		return restTemplate.getForObject(uri, ModuleRegistrationResource.Page.class);
	}

	@Override
	public void unregister(String name, ArtifactType artifactType) {
		String uri = uriTemplate.toString() + "/{type}/{name}";
		restTemplate.delete(uri, artifactType.name(), name);
	}

	@Override
	public DetailedModuleRegistrationResource info(String name, ArtifactType type) {
		String uri = uriTemplate.toString() + "/{type}/{name}";
		return restTemplate.getForObject(uri, DetailedModuleRegistrationResource.class, type, name);
	}

	@Override
	public ModuleRegistrationResource register(String name, ArtifactType type,
			String coordinates, boolean force) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("coordinates", coordinates);
		values.add("force", Boolean.toString(force));

		String uri = uriTemplate.toString() + "/{type}/{name}";
		return restTemplate.postForObject(uri, values, ModuleRegistrationResource.class,
				type, name);
	}
}
