/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.cloud.skipper.server.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable properties of the server.
 *
 * @author Mark Pollack
 * @author Eric Bottard
 */
@ConfigurationProperties("spring.cloud.skipper.server")
public class SkipperServerProperties {

	/**
	 * Map of Package Repositories configurations.
	 */
	private Map<String, PackageRepository> packageRepositories = new HashMap<>();

	/**
	 * Flag indicating to sync the local contents of the index directory with the database on
	 * startup.
	 */
	private boolean synchonizeIndexOnContextRefresh = true;

	/**
	 * Flag indicating if the ReleaseStateUpdateService, which has a
	 * {@link org.springframework.scheduling.annotation.Scheduled} method, should be created.
	 */
	private boolean enableReleaseStateUpdateService;

	public Map<String, PackageRepository> getPackageRepositories() {
		return packageRepositories;
	}

	public void setPackageRepositories(Map<String, PackageRepository> packageRepositories) {
		this.packageRepositories = packageRepositories;
	}

	public boolean isSynchonizeIndexOnContextRefresh() {
		return synchonizeIndexOnContextRefresh;
	}

	public void setSynchonizeIndexOnContextRefresh(boolean synchonizeIndexOnContextRefresh) {
		this.synchonizeIndexOnContextRefresh = synchonizeIndexOnContextRefresh;
	}

	public boolean isEnableReleaseStateUpdateService() {
		return enableReleaseStateUpdateService;
	}

	public void setEnableReleaseStateUpdateService(boolean enableReleaseStateUpdateService) {
		this.enableReleaseStateUpdateService = enableReleaseStateUpdateService;
	}

	public static class PackageRepository {

		private String url;
		private String sourceUrl;
		private Boolean local = false;
		private String description;
		private Integer repoOrder;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getSourceUrl() {
			return sourceUrl;
		}

		public void setSourceUrl(String sourceUrl) {
			this.sourceUrl = sourceUrl;
		}

		public Boolean getLocal() {
			return local;
		}

		public void setLocal(Boolean local) {
			this.local = local;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Integer getRepoOrder() {
			return repoOrder;
		}

		public void setRepoOrder(Integer repoOrder) {
			this.repoOrder = repoOrder;
		}
	}
}
