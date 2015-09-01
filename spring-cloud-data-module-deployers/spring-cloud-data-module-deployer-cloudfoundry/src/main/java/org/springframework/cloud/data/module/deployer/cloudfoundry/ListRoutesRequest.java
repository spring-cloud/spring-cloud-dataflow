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
 * Created by ericbottard on 01/09/15.
 */
public class ListRoutesRequest {

	private String domainId;

	private String path;

	private String host;

	public String getDomainId() {
		return domainId;
	}

	public ListRoutesRequest withDomainId(String domainId) {
		this.domainId = domainId;
		return this;
	}

	public String getPath() {
		return path;
	}

	public ListRoutesRequest withPath(String path) {
		this.path = path;
		return this;
	}

	public String getHost() {
		return host;
	}

	public ListRoutesRequest withHost(String host) {
		this.host = host;
		return this;
	}
}
