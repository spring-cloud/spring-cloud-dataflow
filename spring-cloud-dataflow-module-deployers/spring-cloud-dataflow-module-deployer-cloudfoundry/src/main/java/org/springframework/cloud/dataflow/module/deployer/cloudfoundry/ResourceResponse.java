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
 * Part or the whole of a number of responses returned from list operations, get operations,
 * and create and update operations.
 *
 * @param <Ent> entity type embedded in response
 *
 * @author Steve Powell
 */
class ResourceResponse<Ent, R extends ResourceResponse<Ent, R>> {

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

	public Ent getEntity() {
		return entity;
	}

	public R withEntity(Ent entity) {
		this.entity = entity;
		@SuppressWarnings("unchecked") R rthis = (R) this;
		return rthis;
	}

}
