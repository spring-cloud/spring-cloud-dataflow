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
package org.springframework.cloud.dataflow.server.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Dataflow Server configuration properties.
 *
 * @author Ilayaperumal Gopinathan
 * @author Eric Bottard
 */
@ConfigurationProperties
public class DataFlowServerProperties {

	/**
	 * File path to a locally available maven repository, where modules will be downloaded.
	 */
	private String localRepository;

	/**
	 * Locations of remote maven repositories from which modules will be downloaded, if not available locally.
	 */
	private String[] remoteRepositories;

	/**
	 * Whether the resolver should operate in offline mode.
	 */
	private Boolean offline;

	/**
	 * Return a String representation of the properties actually set in this object, suitable for
	 * passing to a ModuleLauncher process.
	 */
	public Map<String, String> asStringProperties() {
		Map<String, String> properties = new HashMap<>();
		if (this.getLocalRepository() != null) {
			properties.put("localRepository", this.getLocalRepository());
		}
		if (this.getRemoteRepositories() != null) {
			properties.put("remoteRepositories", StringUtils.arrayToCommaDelimitedString(
					this.getRemoteRepositories()));
		}
		if (this.getOffline() != null) {
			properties.put("offline", String.valueOf(this.getOffline()));
		}
		return properties;
	}


	public void setRemoteRepositories(String[] remoteRepositories) {
		this.remoteRepositories = remoteRepositories;
	}

	public String[] getRemoteRepositories() {
		return remoteRepositories;
	}
	public void setLocalRepository(String localRepository) {
		this.localRepository = localRepository;
	}

	public String getLocalRepository() {
		return localRepository;
	}

	public Boolean getOffline() {
		return offline;
	}

	public void setOffline(Boolean offline) {
		this.offline = offline;
	}
}
