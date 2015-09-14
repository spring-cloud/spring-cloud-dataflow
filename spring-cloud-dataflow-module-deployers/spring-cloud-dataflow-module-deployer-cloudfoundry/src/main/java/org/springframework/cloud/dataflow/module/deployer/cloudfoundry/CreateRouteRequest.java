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

package org.springframework.cloud.dataflow.module.deployer.cloudfoundry;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Holds request information for creating a route.
 *
 * @author Eric Bottard
 */
class CreateRouteRequest {

	private String domainId;

	private String spaceId;

	private String host;

	public String getHost() {
		return host;
	}

	public CreateRouteRequest withHost(String host) {
		this.host = host;
		return this;
	}

	@JsonProperty("domain_guid")
	public String getDomainId() {
		return domainId;
	}

	public CreateRouteRequest withDomainId(String domainId) {
		this.domainId = domainId;
		return this;
	}

	@JsonProperty("space_guid")
	public String getSpaceId() {
		return spaceId;
	}

	public CreateRouteRequest withSpaceId(String spaceId) {
		this.spaceId = spaceId;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		CreateRouteRequest that = (CreateRouteRequest) o;

		if (domainId != null ? !domainId.equals(that.domainId) : that.domainId != null) return false;
		if (spaceId != null ? !spaceId.equals(that.spaceId) : that.spaceId != null) return false;
		return !(host != null ? !host.equals(that.host) : that.host != null);

	}

	@Override
	public int hashCode() {
		int result = domainId != null ? domainId.hashCode() : 0;
		result = 31 * result + (spaceId != null ? spaceId.hashCode() : 0);
		result = 31 * result + (host != null ? host.hashCode() : 0);
		return result;
	}
}
