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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.core.io.Resource;

/**
 * The REST operation request types.
 *
 * @author Steve Powell
 * @author Eric Bottard
 */
class Requests {
	/**
	 * A type used by RestTemplate requests that is made generic so that it can be extended.
	 * @param <T> The request subtype returned from with...() chained methods.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	static class Application<T extends Application<T>> {

		private String name;

		private Integer instances;

		private Integer memory;

		private String spaceId;

		private String state;

		private String buildpack;

		private Map<String, String> environment;

		public String getName() {
			return name;
		}

		public T withName(String name) {
			this.name = name;
			@SuppressWarnings("unchecked") T thisT = (T) this;
			return thisT;
		}

		public Integer getInstances() {
			return instances;
		}

		public T withInstances(int instances) {
			this.instances = instances;
			@SuppressWarnings("unchecked") T thisT = (T) this;
			return thisT;
		}

		public Integer getMemory() {
			return memory;
		}

		public T withMemory(int memory) {
			this.memory = memory;
			@SuppressWarnings("unchecked") T thisT = (T) this;
			return thisT;
		}

		@JsonProperty("space_guid")
		public String getSpaceId() {
			return spaceId;
		}

		public T withSpaceId(String spaceId) {
			this.spaceId = spaceId;
			@SuppressWarnings("unchecked") T thisT = (T) this;
			return thisT;
		}

		public String getState() {
			return state;
		}

		public T withState(String state) {
			this.state = state;
			@SuppressWarnings("unchecked") T thisT = (T) this;
			return thisT;
		}

		public String getBuildpack() {
			return buildpack;
		}

		public T withBuildpack(String buildpack) {
			this.buildpack = buildpack;
			@SuppressWarnings("unchecked") T thisT = (T) this;
			return thisT;
		}

		@JsonProperty("environment_json")
		public Map<String, String> getEnvironment() {
			return environment;
		}

		public T withEnvironment(Map<String, String> environment) {
			this.environment = environment;
			@SuppressWarnings("unchecked") T thisT = (T) this;
			return thisT;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			Application<?> that = (Application<?>) o;

			if (name != null ? !name.equals(that.name) : that.name != null) { return false; }
			if (instances != null ? !instances.equals(that.instances) : that.instances != null) { return false; }
			if (spaceId != null ? !spaceId.equals(that.spaceId) : that.spaceId != null) { return false; }
			if (state != null ? !state.equals(that.state) : that.state != null) { return false; }
			return !(environment != null ? !environment.equals(that.environment) : that.environment != null);
		}

		@Override
		public int hashCode() {
			int result = name != null ? name.hashCode() : 0;
			result = 31 * result + (instances != null ? instances.hashCode() : 0);
			result = 31 * result + (spaceId != null ? spaceId.hashCode() : 0);
			result = 31 * result + (state != null ? state.hashCode() : 0);
			result = 31 * result + (environment != null ? environment.hashCode() : 0);
			return result;
		}
	}

	/**
	 * Request for REST operation {@link CloudControllerOperations#createApplication(CreateApplication) createApplication()}.
	 */
	static class CreateApplication extends Application<CreateApplication> {
	}

	/**
	 * Request for REST operation {@link CloudControllerOperations#createRoute(CreateRoute) createRoute()}.
	 */
	static class CreateRoute {

		private String domainId;

		private String spaceId;

		private String host;

		public String getHost() {
			return host;
		}

		public CreateRoute withHost(String host) {
			this.host = host;
			return this;
		}

		@JsonProperty("domain_guid")
		public String getDomainId() {
			return domainId;
		}

		public CreateRoute withDomainId(String domainId) {
			this.domainId = domainId;
			return this;
		}

		@JsonProperty("space_guid")
		public String getSpaceId() {
			return spaceId;
		}

		public CreateRoute withSpaceId(String spaceId) {
			this.spaceId = spaceId;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			CreateRoute that = (CreateRoute) o;

			if (domainId != null ? !domainId.equals(that.domainId) : that.domainId != null) { return false; }
			if (spaceId != null ? !spaceId.equals(that.spaceId) : that.spaceId != null) { return false; }
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

	/**
	 * Request for REST operation {@link CloudControllerOperations#createServiceBinding(CreateServiceBinding) createServiceBinding()}.
	 */
	static class CreateServiceBinding {

		private String appId;

		private Map<String, String> parameters = new HashMap<>();

		private String serviceInstanceId;

		@JsonProperty("app_guid")
		public String getAppId() {
			return appId;
		}

		public CreateServiceBinding withAppId(String appId) {
			this.appId = appId;
			return this;
		}

		public Map<String, String> getParameters() {
			return parameters;
		}

		public CreateServiceBinding withParameters(Map<String, String> parameters) {
			this.parameters.putAll(parameters);
			return this;
		}

		public CreateServiceBinding withParameter(String key, String value) {
			this.parameters.put(key, value);
			return this;
		}

		@JsonProperty("service_instance_guid")
		public String getServiceInstanceId() {
			return serviceInstanceId;
		}

		public CreateServiceBinding withServiceInstanceId(String serviceInstanceId) {
			this.serviceInstanceId = serviceInstanceId;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			CreateServiceBinding that = (CreateServiceBinding) o;

			if (appId != null ? !appId.equals(that.appId) : that.appId != null) { return false; }
			if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) { return false; }
			return !(serviceInstanceId != null ? !serviceInstanceId.equals(that.serviceInstanceId) : that.serviceInstanceId != null);
		}

		@Override
		public int hashCode() {
			int result = appId != null ? appId.hashCode() : 0;
			result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
			result = 31 * result + (serviceInstanceId != null ? serviceInstanceId.hashCode() : 0);
			return result;
		}
	}

	/**
	 * Request for REST operation {@link CloudControllerOperations#deleteApplication(DeleteApplication) deleteApplication()}.
	 */
	static class DeleteApplication {

		private volatile String id;

		public String getId() {
			return id;
		}

		public DeleteApplication withId(String id) {
			this.id = id;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			DeleteApplication that = (DeleteApplication) o;

			return !(id != null ? !id.equals(that.id) : that.id != null);
		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}
	}

	/**
	 * Request for REST opertation {@link CloudControllerOperations#deleteRoute(DeleteRoute) deleteRoute()}.
	 */
	static class DeleteRoute {
		private String id;

		public String getId() {
			return id;
		}

		public DeleteRoute withId(String id) {
			this.id = id;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			DeleteRoute that = (DeleteRoute) o;

			return !(id != null ? !id.equals(that.id) : that.id != null);
		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}
	}

	/**
	 * Request for REST operation {@link CloudControllerOperations#getApplicationEnvironment(GetApplicationEnvironment) getApplicationEnvironment()}.
	 */
	static class GetApplicationEnvironment {

		private volatile String id;

		public String getId() {
			return id;
		}

		public GetApplicationEnvironment withId(String id) {
			this.id = id;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			GetApplicationEnvironment that = (GetApplicationEnvironment) o;

			return !(id != null ? !id.equals(that.id) : that.id != null);
		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}
	}

	/**
	 * Request for REST operation {@link CloudControllerOperations#getApplicationStatistics(GetApplicationStatistics) getApplicationStatistics()}.
	 */
	static class GetApplicationStatistics {

		private volatile String id;

		public String getId() {
			return id;
		}

		public GetApplicationStatistics withId(String id) {
			this.id = id;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			GetApplicationStatistics that = (GetApplicationStatistics) o;

			return !(id != null ? !id.equals(that.id) : that.id != null);
		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}
	}

	/**
	 * Request for REST operation {@link CloudControllerOperations#listApplications(ListApplications) listApplications()}.
	 */
	static class ListApplications {

		private volatile String name;

		private volatile String spaceId;

		public String getName() {
			return name;
		}

		public String getSpaceId() {
			return spaceId;
		}

		public ListApplications withName(String name) {
			this.name = name;
			return this;
		}

		public ListApplications withSpaceId(String spaceId) {
			this.spaceId = spaceId;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			ListApplications that = (ListApplications) o;

			if (name != null ? !name.equals(that.name) : that.name != null) { return false; }
			return !(spaceId != null ? !spaceId.equals(that.spaceId) : that.spaceId != null);
		}

		@Override
		public int hashCode() {
			int result = name != null ? name.hashCode() : 0;
			result = 31 * result + (spaceId != null ? spaceId.hashCode() : 0);
			return result;
		}
	}

	/**
	 * Request for REST operation {@link CloudControllerOperations#listOrganizations(ListOrganizations) listOrganizations()}.
	 */
	static class ListOrganizations {

		private volatile String name;

		public String getName() {
			return name;
		}

		public ListOrganizations withName(String name) {
			this.name = name;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			ListOrganizations that = (ListOrganizations) o;

			return !(name != null ? !name.equals(that.name) : that.name != null);
		}

		@Override
		public int hashCode() {
			return name != null ? name.hashCode() : 0;
		}
	}

	/**
	 * Request for REST operation {@link CloudControllerOperations#listRoutes(ListRoutes) listRoutes()}.
	 */
	static class ListRoutes {

		private String domainId;

		private String host;

		public String getDomainId() {
			return domainId;
		}

		public ListRoutes withDomainId(String domainId) {
			this.domainId = domainId;
			return this;
		}

		public String getHost() {
			return host;
		}

		public ListRoutes withHost(String host) {
			this.host = host;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			ListRoutes that = (ListRoutes) o;

			if (domainId != null ? !domainId.equals(that.domainId) : that.domainId != null) { return false; }
			return !(host != null ? !host.equals(that.host) : that.host != null);
		}

		@Override
		public int hashCode() {
			int result = domainId != null ? domainId.hashCode() : 0;
			result = 31 * result + (host != null ? host.hashCode() : 0);
			return result;
		}
	}

	/**
	 * Request for REST operation {@link CloudControllerOperations#listServiceBindings(ListServiceBindings) listServiceBindings()}.
	 */
	static class ListServiceBindings {

		private String appId;

		public String getAppId() {
			return appId;
		}

		public ListServiceBindings withAppId(String appId) {
			this.appId = appId;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			ListServiceBindings that = (ListServiceBindings) o;

			return !(appId != null ? !appId.equals(that.appId) : that.appId != null);
		}

		@Override
		public int hashCode() {
			return appId != null ? appId.hashCode() : 0;
		}
	}

	/**
	 * Request for REST operation {@link CloudControllerOperations#listServiceInstances(ListServiceInstances) listServiceInstances()}.
	 */
	static class ListServiceInstances {
		private String name;

		private String spaceId;

		public String getName() {
			return name;
		}

		public ListServiceInstances withName(String name) {
			this.name = name;
			return this;
		}

		public String getSpaceId() {
			return spaceId;
		}

		public ListServiceInstances withSpaceId(String spaceId) {
			this.spaceId = spaceId;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			ListServiceInstances that = (ListServiceInstances) o;

			if (name != null ? !name.equals(that.name) : that.name != null) { return false; }
			return !(spaceId != null ? !spaceId.equals(that.spaceId) : that.spaceId != null);
		}

		@Override
		public int hashCode() {
			int result = name != null ? name.hashCode() : 0;
			result = 31 * result + (spaceId != null ? spaceId.hashCode() : 0);
			return result;
		}
	}

	/**
	 * Request for REST operation {@link CloudControllerOperations#listSharedDomains(ListSharedDomains) listSharedDomains()}.
	 */
	static class ListSharedDomains {

		private String name;

		public String getName() {
			return name;
		}

		public ListSharedDomains withName(String name) {
			this.name = name;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			ListSharedDomains that = (ListSharedDomains) o;

			return !(name != null ? !name.equals(that.name) : that.name != null);
		}

		@Override
		public int hashCode() {
			return name != null ? name.hashCode() : 0;
		}
	}

	/**
	 * Request for REST operation {@link CloudControllerOperations#listSpaces(ListSpaces) listSpaces()}.
	 */
	static class ListSpaces {

		private volatile String name;

		private volatile String orgId;

		public String getName() {
			return name;
		}

		public String getOrgId() {
			return orgId;
		}

		public ListSpaces withName(String name) {
			this.name = name;
			return this;
		}

		public ListSpaces withOrgId(String orgId) {
			this.orgId = orgId;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			ListSpaces that = (ListSpaces) o;

			if (name != null ? !name.equals(that.name) : that.name != null) { return false; }
			return !(orgId != null ? !orgId.equals(that.orgId) : that.orgId != null);
		}

		@Override
		public int hashCode() {
			int result = name != null ? name.hashCode() : 0;
			result = 31 * result + (orgId != null ? orgId.hashCode() : 0);
			return result;
		}
	}

	/**
	 * Request for REST operation {@link CloudControllerOperations#removeServiceBinding(RemoveServiceBinding) removeServiceBinding()} .
	 */
	static class RemoveServiceBinding {
		private String bindingId;

		private String appId;

		public String getAppId() {
			return appId;
		}

		public RemoveServiceBinding withAppId(String id) {
			this.appId = id;
			return this;
		}

		public String getBindingId() {
			return bindingId;
		}

		public RemoveServiceBinding withBindingId(String bindingId) {
			this.bindingId = bindingId;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			RemoveServiceBinding that = (RemoveServiceBinding) o;

			if (bindingId != null ? !bindingId.equals(that.bindingId) : that.bindingId != null) { return false; }
			return !(appId != null ? !appId.equals(that.appId) : that.appId != null);
		}

		@Override
		public int hashCode() {
			int result = bindingId != null ? bindingId.hashCode() : 0;
			result = 31 * result + (appId != null ? appId.hashCode() : 0);
			return result;
		}
	}

	/**
	 * Request for REST operations {@link CloudControllerOperations#mapRoute(RouteMapping) mapRoute()}
	 * and {@link CloudControllerOperations#unmapRoute(RouteMapping) unmapRoute()}.
	 */
	static class RouteMapping {

		private String appId;

		private String routeId;

		public String getAppId() {
			return appId;
		}

		public RouteMapping withAppId(String appId) {
			this.appId = appId;
			return this;
		}

		public String getRouteId() {
			return routeId;
		}

		public RouteMapping withRouteId(String routeId) {
			this.routeId = routeId;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			RouteMapping that = (RouteMapping) o;

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

	/**
	 * Request for REST operation {@link CloudControllerOperations#updateApplication(UpdateApplication) updateApplication()}.
	 */
	static class UpdateApplication extends Application<UpdateApplication> {

		private String id;

		public String getId() {
			return id;
		}

		public UpdateApplication withId(String id) {
			this.id = id;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }
			if (!super.equals(o)) { return false; }

			UpdateApplication that = (UpdateApplication) o;

			return !(id != null ? !id.equals(that.id) : that.id != null);
		}

		@Override
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (id != null ? id.hashCode() : 0);
			return result;
		}
	}

	/**
	 * Request for REST operation {@link CloudControllerOperations#uploadBits(UploadBits) uploadBits()}.
	 */
	static class UploadBits {

		private volatile String id;

		private volatile Resource resource;

		public Resource getResource() {
			return resource;
		}

		public UploadBits withResource(Resource resource) {
			this.resource = resource;
			return this;
		}

		public String getId() {
			return id;
		}

		public UploadBits withId(String id) {
			this.id = id;
			return this;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			UploadBits that = (UploadBits) o;

			if (id != null ? !id.equals(that.id) : that.id != null) {
				return false;
			}
			return !(resource != null ? !resource.equals(that.resource) : that.resource != null);
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (resource != null ? resource.hashCode() : 0);
			return result;
		}
	}
}
