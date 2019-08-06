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

import org.springframework.cloud.dataflow.rest.resource.CompletionProposalsResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.UriTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of {@link CompletionOperations} that uses a {@link RestTemplate} to
 * issue commands to the Data Flow server.
 *
 * @author Eric Bottard
 */
public class CompletionTemplate implements CompletionOperations {

	private final RestTemplate restTemplate;

	private final UriTemplate streamCompletionUriTemplate;

	private final UriTemplate taskCompletionUriTemplate;

	public CompletionTemplate(RestTemplate restTemplate, Link streamLink, Link taskLink) {
		this.restTemplate = restTemplate;
		this.streamCompletionUriTemplate = UriTemplate.of(streamLink.getHref());
		this.taskCompletionUriTemplate = UriTemplate.of(taskLink.getHref());
	}

	@Override
	public CompletionProposalsResource streamCompletions(String prefix, int levelOfDetail) {
		return restTemplate.getForObject(streamCompletionUriTemplate.expand(prefix, levelOfDetail),
				CompletionProposalsResource.class);
	}

	@Override
	public CompletionProposalsResource taskCompletions(String prefix, int levelOfDetail) {
		return restTemplate.getForObject(taskCompletionUriTemplate.expand(prefix, levelOfDetail),
				CompletionProposalsResource.class);
	}

}
