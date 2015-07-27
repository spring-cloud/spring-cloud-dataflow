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

import org.springframework.core.io.Resource;

/**
 * Request for REST operation {@link CloudControllerRestClient#uploadBits(UploadBitsRequest) uploadBits()}.
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
}
