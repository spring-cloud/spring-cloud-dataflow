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

package org.springframework.cloud.data.module.deployer.cloudfoundry;

/**
 * Request for REST operations
 * {@link CloudControllerRestClient#mapRoute(RouteMappingRequest)} and
 * {@link CloudControllerRestClient#unmapRoute(RouteMappingRequest)}.
 *
 * @author Eric Bottard
 */
public class RouteMappingRequest {

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
}
