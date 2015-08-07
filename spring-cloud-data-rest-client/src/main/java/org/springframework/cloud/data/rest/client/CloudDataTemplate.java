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

package org.springframework.cloud.data.rest.client;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.UriTemplate;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 *  @author Ilayaperumal Gopinathan
 *  @author Mark Fisher
 */
public class CloudDataTemplate implements CloudDataOperations {

	/**
	 * A template used for http interaction.
	 */
	protected final RestTemplate restTemplate;

	/**
	 * Holds discovered URLs of the API.
	 */
	protected Map<String, UriTemplate> resources = new HashMap<String, UriTemplate>();

	/**
	 * REST client template for stream operations
	 */
	private final StreamTemplate streamTemplate;


	public CloudDataTemplate(URI baseURI) {
		this.restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		restTemplate.setErrorHandler(new VndErrorResponseErrorHandler(restTemplate.getMessageConverters()));
		ResourceSupport resourceSupport = restTemplate.getForObject(baseURI, ResourceSupport.class);
		resources.put("streams/definitions", new UriTemplate(resourceSupport.getLink("streams").getHref() + "/definitions"));
		resources.put("streams/deployments", new UriTemplate(resourceSupport.getLink("streams").getHref() + "/deployments"));
		this.streamTemplate = new StreamTemplate(restTemplate, resources);
	}

	@Override
	public StreamOperations streamOperations() {
		return streamTemplate;
	}
}
