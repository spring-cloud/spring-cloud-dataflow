/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.domain;

/**
 * This contains all the request attributes for rollback operation.
 *
 * @author Janne Valkealahti
 *
 */
public class RollbackRequest {

	private String releaseName;
	private Integer version;
	private Long timeout;

	public RollbackRequest() {
	}

	public RollbackRequest(String releaseName, Integer version) {
		this(releaseName, version, null);
	}

	public RollbackRequest(String releaseName, Integer version, Long timeout) {
		this.releaseName = releaseName;
		this.version = version;
		this.timeout = timeout;
	}

	public String getReleaseName() {
		return releaseName;
	}

	public void setReleaseName(String releaseName) {
		this.releaseName = releaseName;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public Long getTimeout() {
		return timeout;
	}

	public void setTimeout(Long timeout) {
		this.timeout = timeout;
	}

	@Override
	public String toString() {
		return "RollbackRequest [releaseName=" + releaseName + ", version=" + version + ", timeout=" + timeout + "]";
	}
}
