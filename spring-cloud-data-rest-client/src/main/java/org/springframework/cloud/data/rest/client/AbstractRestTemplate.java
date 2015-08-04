/*
 * Copyright 2013-2015 the original author or authors.
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

import org.springframework.hateoas.UriTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * Base class for sub-parts of the API, allows sharing configured objects like the {@link RestTemplate}.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 *
 */
class AbstractRestTemplate {

	/**
	 * REST template used for http interaction.
	 */
	protected final RestTemplate restTemplate;

	/**
	 * Holds discovered URLs of the API.
	 */
	protected final Map<String, UriTemplate> resources;

	/**
	 * Basic constructor, used solely by entry point to the API
	 */
	AbstractRestTemplate(RestTemplate restTemplate,  Map<String, UriTemplate> resources) {
		this.restTemplate = restTemplate;
		this.resources = resources;
	}
}
