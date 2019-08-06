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

import org.springframework.hateoas.RepresentationModel;

/**
 * Provides meta-information about the Spring Cloud Skipper server.
 *
 * @author Janne Valkealahti
 *
 */
public class AboutResource extends RepresentationModel {

	private VersionInfo versionInfo = new VersionInfo();

	/**
	 * Default constructor for serialization frameworks.
	 */
	public AboutResource() {
	}

	public VersionInfo getVersionInfo() {
		return versionInfo;
	}

	public void setVersionInfo(VersionInfo versionInfo) {
		this.versionInfo = versionInfo;
	}

	@Override
	public String toString() {
		return getVersionInfo().getServer().getName() + " v" + getVersionInfo().getServer().getVersion();
	}
}
