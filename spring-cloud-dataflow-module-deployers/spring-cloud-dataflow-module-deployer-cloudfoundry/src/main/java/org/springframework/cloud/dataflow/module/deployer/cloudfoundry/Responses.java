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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The REST operations response types.
 *
 * @author Steve Powell
 * @author Eric Bottard
 */
class Responses {
	/**
	 * Element of list responses.
	 */
	static class ApplicationResource extends Resource<Entities.ApplicationEntity, ApplicationResource> {
	}

	/**
	 * Element of list response {@link ListServiceBindings}.
	 */
	static class BindingResource extends Resource<Entities.ServiceBindingEntity, BindingResource> {
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#createApplication(Requests.CreateApplication) createApplication()}.
	 */
	static class CreateApplication extends Resource<Entities.ApplicationEntity, CreateApplication> {
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#createRoute(Requests.CreateRoute) createRoute()}.
	 */
	static class CreateRoute extends Resource<Entities.RouteEntity, CreateRoute> {
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#createServiceBinding(Requests.CreateServiceBinding) createServiceBinding()}.
	 */
	static class CreateServiceBinding extends Resource<Entities.ServiceBindingEntity, CreateServiceBinding> {
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#deleteApplication(Requests.DeleteApplication) deleteApplication()}.
	 */
	static class DeleteApplication {

		private volatile boolean deleted;

		boolean isDeleted() {
			return deleted;
		}

		DeleteApplication withDeleted(boolean deleted) {
			this.deleted = deleted;
			return this;
		}
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#deleteRoute(Requests.DeleteRoute) deleteRoute()}.
	 */
	static class DeleteRoute {
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#getApplicationStatistics(Requests.GetApplicationStatistics) getApplicationStatistics()}.
	 */
	static class GetApplicationStatistics extends HashMap<String, ApplicationInstanceStatus> {
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#listApplications(Requests.ListApplications) listApplications()}.
	 */
	static class ListApplications extends AbstractPaginated<ApplicationResource, ListApplications> {
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#listOrganizations(Requests.ListOrganizations) listOrganizations()}.
	 */
	static class ListOrganizations extends AbstractPaginated<NamedResource, ListOrganizations> {
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#listRoutes(Requests.ListRoutes) listRoutes()}.
	 */
	static class ListRoutes extends AbstractPaginated<RouteResource, ListRoutes> {
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#listServiceBindings(Requests.ListServiceBindings) listServiceBindings()}.
	 */
	static class ListServiceBindings extends AbstractPaginated<BindingResource, ListServiceBindings> {
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#listServiceInstances(Requests.ListServiceInstances) listServiceInstances()}.
	 */
	static class ListServiceInstances extends AbstractPaginated<NamedResource, ListServiceInstances> {
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#listSharedDomains(Requests.ListSharedDomains) listSharedDomains()}.
	 */
	static class ListSharedDomains extends AbstractPaginated<NamedResource, ListSharedDomains> {
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#listSpaces(Requests.ListSpaces) listSpaces()}.
	 */
	static class ListSpaces extends AbstractPaginated<NamedResource, ListSpaces> {
	}

	/**
	 * Generic named resource response (used for organizations, spaces, etc.).
	 */
	static class NamedResource extends Resource<Entities.NamedEntity, NamedResource> {
	}

	/**
	 * REST response for operation {@link CloudControllerOperations#removeServiceBinding(Requests.RemoveServiceBinding) removeServiceBinding()}.
	 */
	static class RemoveServiceBinding {
	}

	/**
	 * REST response for operations {@link CloudControllerOperations#mapRoute(Requests.RouteMapping) mapRoute()}
	 * and {@link CloudControllerOperations#unmapRoute(Requests.RouteMapping) unmapRoute()}.
	 */
	static class RouteMapping extends Resource<Entities.BaseEntity, RouteMapping> {
	}

	/**
	 * Part of REST response for operation {@link CloudControllerOperations#listRoutes(Requests.ListRoutes) listRoutes()}.
	 */
	static class RouteResource extends Resource<Entities.RouteEntity, RouteResource> {
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#updateApplication(Requests.UpdateApplication) updateApplication()}.
	 */
	static class UpdateApplication extends Resource<Entities.BaseEntity, UpdateApplication> {
	}

	/**
	 * Response from REST operation {@link CloudControllerOperations#uploadBits(Requests.UploadBits) uploadBits()}.
	 */
	static class UploadBits {
	}

	/**
	 * Part or the whole of a number of responses returned from list, get, create and update operations.
	 *
	 * @param <Ent> entity type embedded in response
	 * @param <R>   return resource subtype of chained with...() methods
	 */
	static class Resource<Ent, R extends Resource<Ent, R>> {

		private volatile Metadata metadata;

		private volatile Ent entity;

		public Metadata getMetadata() {
			return metadata;
		}

		public R withMetadata(Metadata metadata) {
			this.metadata = metadata;
			@SuppressWarnings("unchecked") R rthis = (R) this;
			return rthis;
		}

		public Ent getEntity() {
			return entity;
		}

		public R withEntity(Ent entity) {
			this.entity = entity;
			@SuppressWarnings("unchecked") R rthis = (R) this;
			return rthis;
		}
	}

	/**
	 * Standard part of a resource response.
	 */
	static class Metadata {

		private volatile String id;

		private volatile String url;

		private volatile String createdAt;

		private volatile String updatedAt;

		public String getId() {
			return id;
		}

		@JsonProperty("guid")
		public Metadata withId(String id) {
			this.id = id;
			return this;
		}

		public String getUrl() {
			return url;
		}

		public Metadata withUrl(String url) {
			this.url = url;
			return this;
		}

		public String getCreatedAt() {
			return createdAt;
		}

		@JsonProperty("created_at")
		public Metadata withCreatedAt(String createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		public String getUpdatedAt() {
			return updatedAt;
		}

		@JsonProperty("updated_at")
		public Metadata withUpdatedAt(String updatedAt) {
			this.updatedAt = updatedAt;
			return this;
		}
	}

	/**
	 * The page structure around a standard response.
	 * @param <Res>    The resource type delivered in the page lists.
	 * @param <R>	   The subclass return type for chaining with...() methods.
	 */
	abstract static class AbstractPaginated<Res, R extends AbstractPaginated<Res, R>> {

		private volatile int totalResults;

		private volatile int totalPages;

		private volatile String previousUrl;

		private volatile String nextUrl;

		private volatile List<Res> resources;

		public final int getTotalResults() {
			return totalResults;
		}

		@JsonProperty("total_results")
		public final R withTotalResults(int totalResults) {
			this.totalResults = totalResults;
			@SuppressWarnings("unchecked") R rthis = (R) this;
			return rthis;
		}

		public final int getTotalPages() {
			return totalPages;
		}

		@JsonProperty("total_pages")
		public final R withTotalPages(int totalPages) {
			this.totalPages = totalPages;
			@SuppressWarnings("unchecked") R rthis = (R) this;
			return rthis;
		}

		public final String getPreviousUrl() {
			return previousUrl;
		}

		@JsonProperty("prev_url")
		public final R withPreviousUrl(String previousUrl) {
			this.previousUrl = previousUrl;
			@SuppressWarnings("unchecked") R rthis = (R) this;
			return rthis;
		}

		public final String getNextUrl() {
			return nextUrl;
		}

		@JsonProperty("next_url")
		public final R withNextUrl(String nextUrl) {
			this.nextUrl = nextUrl;
			@SuppressWarnings("unchecked") R rthis = (R) this;
			return rthis;
		}

		public final List<Res> getResources() {
			return resources;
		}

		public final R withResources(List<Res> resources) {
			this.resources = resources;
			@SuppressWarnings("unchecked") R rthis = (R) this;
			return rthis;
		}
	}

	/**
	 * Part of responses with the status and properties of an application instance.
	 */
	static class ApplicationInstanceStatus {

		private volatile String state;

		private volatile Statistics statistics;

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

		public Statistics getStatistics() {
			return statistics;
		}

		@JsonProperty("stats")
		public void setStatistics(Statistics statistics) {
			this.statistics = statistics;
		}

		static class Statistics {

			private volatile Usage usage;

			private volatile String name;

			private volatile List<String> uris;

			private volatile String host;

			private volatile Integer port;

			private volatile Integer uptime;

			private volatile Integer memoryQuota;

			private volatile Integer diskQuota;

			private volatile Integer fdsQuota;

			public Usage getUsage() {
				return usage;
			}

			public void setUsage(Usage usage) {
				this.usage = usage;
			}

			static class Usage {

				private volatile Integer disk;

				private volatile Integer memory;

				private volatile Double cpu;

				private volatile String time;

				public Integer getDisk() {
					return disk;
				}

				public void setDisk(Integer disk) {
					this.disk = disk;
				}

				public Integer getMemory() {
					return memory;
				}

				@JsonProperty("mem")
				public void setMemory(Integer memory) {
					this.memory = memory;
				}

				public Double getCpu() {
					return cpu;
				}

				public void setCpu(Double cpu) {
					this.cpu = cpu;
				}

				public String getTime() {
					return time;
				}

				public void setTime(String time) {
					this.time = time;
				}
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public List<String> getUris() {
				return uris;
			}

			public void setUris(List<String> uris) {
				this.uris = uris;
			}

			public String getHost() {
				return host;
			}

			public void setHost(String host) {
				this.host = host;
			}

			public Integer getPort() {
				return port;
			}

			public void setPort(Integer port) {
				this.port = port;
			}

			public Integer getUptime() {
				return uptime;
			}

			public void setUptime(Integer uptime) {
				this.uptime = uptime;
			}

			public Integer getMemoryQuota() {
				return memoryQuota;
			}

			@JsonProperty("mem_quota")
			public void setMemoryQuota(Integer memoryQuota) {
				this.memoryQuota = memoryQuota;
			}

			public Integer getDiskQuota() {
				return diskQuota;
			}

			@JsonProperty("disk_quota")
			public void setDiskQuota(Integer diskQuota) {
				this.diskQuota = diskQuota;
			}

			public Integer getFdsQuota() {
				return fdsQuota;
			}

			@JsonProperty("fds_quota")
			public void setFdsQuota(Integer fdsQuota) {
				this.fdsQuota = fdsQuota;
			}
		}
	}
}
