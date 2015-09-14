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

import org.springframework.core.io.Resource;

/**
 * Request for REST operation {@link CloudControllerOperations#uploadBits(UploadBitsRequest) uploadBits()}.
 *
 * @author Steve Powell
 */
class UploadBitsRequest {

	private volatile String id;

	private volatile Resource resource;

	public Resource getResource() {
		return resource;
	}

	public UploadBitsRequest withResource(Resource resource) {
		this.resource = resource;
		return this;
	}

	public String getId() {
		return id;
	}

	public UploadBitsRequest withId(String id) {
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

		UploadBitsRequest that = (UploadBitsRequest) o;

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
