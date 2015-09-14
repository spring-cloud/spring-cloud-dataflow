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

/**
 * Request for REST operations
 * {@link CloudControllerOperations#mapRoute(RouteMappingRequest)} and
 * {@link CloudControllerOperations#unmapRoute(RouteMappingRequest)}.
 *
 * @author Eric Bottard
 */
class RouteMappingRequest {

	private String appId;

	private String routeId;

	public String getAppId() {
		return appId;
	}

	public RouteMappingRequest withAppId(String appId) {
		this.appId = appId;
		return this;
	}

	public String getRouteId() {
		return routeId;
	}

	public RouteMappingRequest withRouteId(String routeId) {
		this.routeId = routeId;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) { return true; }
		if (o == null || getClass() != o.getClass()) { return false; }

		RouteMappingRequest that = (RouteMappingRequest) o;

		if (appId != null ? !appId.equals(that.appId) : that.appId != null) { return false; }
		return !(routeId != null ? !routeId.equals(that.routeId) : that.routeId != null);

	}

	@Override
	public int hashCode() {
		int result = appId != null ? appId.hashCode() : 0;
		result = 31 * result + (routeId != null ? routeId.hashCode() : 0);
		return result;
	}
}
