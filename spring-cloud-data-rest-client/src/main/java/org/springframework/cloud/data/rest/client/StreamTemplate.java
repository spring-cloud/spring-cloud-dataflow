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

import java.util.Map;

import org.springframework.cloud.data.rest.resource.StreamDefinitionResource;
import org.springframework.hateoas.UriTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * @author Ilayaperumal Gopinathan
 */
public class StreamTemplate extends AbstractRestTemplate implements StreamOperations {

	StreamTemplate(RestTemplate restTemplate, Map<String, UriTemplate> resources) {
		super(restTemplate, resources);
	}

	@Override
	public StreamDefinitionResource createStream(String name, String definition, boolean deploy) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("name", name);
		values.add("definition", definition);
		values.add("deploy", Boolean.toString(deploy));

		StreamDefinitionResource stream = restTemplate.postForObject(resources.get("streams/definitions").expand(),
				values,
				StreamDefinitionResource.class);
		return stream;
	}

	@Override
	public StreamDefinitionResource.Page list() {
		String uriTemplate = resources.get("streams/definitions").toString();
		uriTemplate = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uriTemplate, StreamDefinitionResource.Page.class);
	}

}
