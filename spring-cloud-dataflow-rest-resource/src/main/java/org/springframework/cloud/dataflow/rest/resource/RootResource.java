/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.resource;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.cloud.dataflow.rest.Version;
import org.springframework.hateoas.RepresentationModel;

/**
 * Describes the other available resource endpoints, as well as provides information about
 * the server itself, such as API revision number.
 *
 * @author Eric Bottard
 */
public class RootResource extends RepresentationModel<RootResource> {

	private Integer apiRevision;

	// For JSON un-marshalling
	private RootResource() {
	}

	public RootResource(int apiRevision) {
		this.apiRevision = apiRevision;
	}

	@JsonProperty(Version.REVISION_KEY)
	public Integer getApiRevision() {
		return apiRevision;
	}

	public void setApiRevision(int apiRevision) {
		this.apiRevision = apiRevision;
	}
}
