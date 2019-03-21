/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.dataflow.rest;

import java.util.Map;

import org.springframework.cloud.skipper.domain.PackageIdentifier;

/**
 * Captures the required data for updating a stream using Skipper.
 * @author Mark Pollack
 */
public class UpdateStreamRequest {

	private String releaseName;

	private PackageIdentifier packageIdentifier;

	private Map<String, String> updateProperties;

	public UpdateStreamRequest() {
	}
	

	public UpdateStreamRequest(String releaseName, PackageIdentifier packageIdentifier,
			Map<String, String> updateProperties) {
		this.releaseName = releaseName;
		this.packageIdentifier = packageIdentifier;
		this.updateProperties = updateProperties;
	}

	public String getReleaseName() {
		return releaseName;
	}

	public void setReleaseName(String releaseName) {
		this.releaseName = releaseName;
	}

	public PackageIdentifier getPackageIdentifier() {
		return packageIdentifier;
	}

	public void setPackageIdentifier(PackageIdentifier packageIdentifier) {
		this.packageIdentifier = packageIdentifier;
	}

	public Map<String, String> getUpdateProperties() {
		return updateProperties;
	}

	public void setUpdateProperties(Map<String, String> updateProperties) {
		this.updateProperties = updateProperties;
	}

	@Override
	public String toString() {
		return "UpdateStreamRequest{" +
				"releaseName='" + releaseName + '\'' +
				", packageIdentifier=" + packageIdentifier +
				", updateProperties=" + updateProperties +
				'}';
	}
}
