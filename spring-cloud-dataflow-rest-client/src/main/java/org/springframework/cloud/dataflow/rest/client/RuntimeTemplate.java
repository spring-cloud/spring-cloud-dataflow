/*
 * Copyright 2015-2019 the original author or authors.
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

import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.StreamStatusResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation for {@link RuntimeOperations}.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Christian Tzolov
 */
public class RuntimeTemplate implements RuntimeOperations {

	private final RestTemplate restTemplate;

	/**
	 * Uri template for accessing status of all apps.
	 */
	private final Link appStatusesUriTemplate;

	/**
	 * Uri template for accessing status of a single app.
	 */
	private final Link appStatusUriTemplate;

	/**
	 * Uri template for accessing runtime status of selected streams, their apps and instances.
	 */
	private final Link streamStatusUriTemplate;

	RuntimeTemplate(RestTemplate restTemplate, RepresentationModel<?> resources) {
		this.restTemplate = restTemplate;
		this.appStatusesUriTemplate = resources.getLink("runtime/apps").get();
		this.appStatusUriTemplate = resources.getLink("runtime/apps/{appId}").get();
		this.streamStatusUriTemplate = resources.getLink("runtime/streams/{streamNames}").get();
	}

	@Override
	public PagedModel<AppStatusResource> status() {
		String uriTemplate = this.appStatusesUriTemplate.expand().getHref();
		uriTemplate = uriTemplate + "?size=2000";
		return this.restTemplate.getForObject(uriTemplate, AppStatusResource.Page.class);
	}

	@Override
	public AppStatusResource status(String deploymentId) {
		return this.restTemplate.getForObject(appStatusUriTemplate.expand(deploymentId).getHref(), AppStatusResource.class);
	}

	@Override
	public PagedModel<StreamStatusResource> streamStatus(String... streamNames) {
		return this.restTemplate.getForObject(streamStatusUriTemplate.expand(streamNames).getHref(),
				StreamStatusResource.Page.class);
	}

}
