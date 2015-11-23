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
package org.springframework.cloud.dataflow.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Admin configuration properties.
 *
 * @author Ilayaperumal Gopinathan
 */
@ConfigurationProperties
public class AdminProperties {

	/**
	 * File path to a locally available maven repository, where modules will be downloaded.
	 */
	private String localRepository;

	/**
	 * Location of comma separated remote maven repositories from which modules will be downloaded, if not available locally.
	 */
	private String remoteRepositories;

	/**
	 * Whether the resolver should operate in offline mode. true to set to offline mode.
	 */
	private String offline;

	public void setRemoteRepositories(String remoteRepositories) {
		this.remoteRepositories = remoteRepositories;
	}

	public String getRemoteRepositories() {
		return remoteRepositories;
	}
	public void setLocalRepository(String localRepository) {
		this.localRepository = localRepository;
	}

	public String getLocalRepository() {
		return localRepository;
	}

	public String getOffline() {
		return offline;
	}

	public void setOffline(String offline) {
		this.offline = offline;
	}
}
