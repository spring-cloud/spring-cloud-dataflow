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
package org.springframework.cloud.dataflow.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;

/**
 * Properties for version information of core dependencies.
 *
 * @author Gunnar Hillert
 * @author Glenn Renfro
 */
@ConfigurationProperties(prefix = VersionInfoProperties.VERSION_INFO_PREFIX)
public class VersionInfoProperties {

	public static final String VERSION_INFO_PREFIX = DataFlowPropertyKeys.PREFIX + "version-info";

	private DependencyStatus dependencyFetch;

	private Dependencies dependencies;

	/**
	 * @return instance of a {@link DependencyStatus} that determines if
	 * checksum information should be returned.
	 */
	public DependencyStatus getDependencyFetch() {
		return dependencyFetch;
	}

	/**
	 * Establish whether checksum information should be returned.
	 *
	 * @param dependencyFetch instance of {@link DependencyStatus}
	 */
	public void setDependencyFetch(DependencyStatus dependencyFetch) {
		this.dependencyFetch = dependencyFetch;
	}

	/**
	 *
	 * @return an instance of {@link Dependencies} containing about information
	 * about dependencies.
	 */
	public Dependencies getDependencies() {
		return dependencies;
	}

	/**
	 * Establishes the {@link Dependencies} instance that contains information
	 * about the dependencies.
	 *
	 * @param dependencies instance of {@link Dependencies}
	 */
	public void setDependencies(Dependencies dependencies) {
		this.dependencies = dependencies;
	}


	/**
	 * Represents whether dependency information should be returned as a part
	 * of the about result set.
	 */
	public static class DependencyStatus {
		private boolean enabled;

		/**
		 * @return true if dependencies should be returned.
		 * False if dependencies should not be returned.
		 */
		public boolean isEnabled() {
			return enabled;
		}

		/**
		 * Establishes if dependencies should returned.
		 * 
		 * @param enabled true if dependencies should be returned, otherwise false.
		 */
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}

	/**
	 * The dependencies that will be returned by the about controller.
	 */
	public static class Dependencies {
		private DependencyAboutInfo springCloudDataflowShell;

		private DependencyAboutInfo springCloudDataflowCore;

		private DependencyAboutInfo springCloudDataflowDashboard;

		private DependencyAboutInfo springCloudDataflowImplementation;

		/**
		 * @return {@link DependencyAboutInfo} for the shell.
		 */
		public DependencyAboutInfo getSpringCloudDataflowShell() {
			return springCloudDataflowShell;
		}

		/**
		 * Establish the {@link DependencyAboutInfo} for the shell.
		 *
		 * @param springCloudDataflowShell the {@link DependencyAboutInfo} containing
		 *                                 information about the shell.
		 */
		public void setSpringCloudDataflowShell(DependencyAboutInfo springCloudDataflowShell) {
			this.springCloudDataflowShell = springCloudDataflowShell;
		}

		/**
		 * @return {@link DependencyAboutInfo} for the core.
		 */
		public DependencyAboutInfo getSpringCloudDataflowCore() {
			return springCloudDataflowCore;
		}

		/**
		 * Establish the {@link DependencyAboutInfo} for the core.
		 *
		 * @param springCloudDataflowCore the {@link DependencyAboutInfo} containing
		 *                                information about the core.
		 */
		public void setSpringCloudDataflowCore(DependencyAboutInfo springCloudDataflowCore) {
			this.springCloudDataflowCore = springCloudDataflowCore;
		}

		/**
		 * @return {@link DependencyAboutInfo} for the dashboard.
		 */
		public DependencyAboutInfo getSpringCloudDataflowDashboard() {
			return springCloudDataflowDashboard;
		}

		/**
		 * Establish the {@link DependencyAboutInfo} for the dashboard.
		 *
		 * @param springCloudDataflowDashboard the {@link DependencyAboutInfo} containing
		 *                                     information about the dashboard.
		 */
		public void setSpringCloudDataflowDashboard(DependencyAboutInfo springCloudDataflowDashboard) {
			this.springCloudDataflowDashboard = springCloudDataflowDashboard;
		}

		/**
		 * @return {@link DependencyAboutInfo} for the implementation.
		 */
		public DependencyAboutInfo getSpringCloudDataflowImplementation() {
			return springCloudDataflowImplementation;
		}

		/**
		 * Establish the {@link DependencyAboutInfo} for the implementation.
		 *
		 * @param springCloudDataflowImplementation the {@link DependencyAboutInfo} containing
		 *                                          information about the implementation.
		 */
		public void setSpringCloudDataflowImplementation(DependencyAboutInfo springCloudDataflowImplementation) {
			this.springCloudDataflowImplementation = springCloudDataflowImplementation;
		}
	}

	/**
	 * Information about the dependency.
	 */
	public static class DependencyAboutInfo {
		private String name;

		private String version;

		private String url;

		private String checksumSha1;

		private String checksumSha1Url;

		private String checksumSha256;

		private String checksumSha256Url;

		/**
		 *
		 * @return the display name of the dependency.
		 */
		public String getName() {
			return name;
		}

		/**
		 * Establish the display name for the dependency.
		 *
		 * @param name String containing the name to be displayed.
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 *
		 * @return the version of the dependency.
		 */
		public String getVersion() {
			return version;
		}

		/**
		 * Establish the version for the dependency.
		 *
		 * @param version String containing the version.
		 */
		public void setVersion(String version) {
			this.version = version;
		}

		/**
		 *
		 * @return the url of the dependency.
		 */
		public String getUrl() {
			return url;
		}

		/**
		 * Establish the url for the dependency.
		 *
		 * @param url String containing the url.
		 */
		public void setUrl(String url) {
			this.url = url;
		}

		/**
		 *
		 * @return the sha1 encoding that should be returned for the dependency.
		 */
		public String getChecksumSha1() {
			return checksumSha1;
		}

		/**
		 * Establish the checksumSha1 for the dependency.
		 *
		 * @param checksumSha1 String containing the checksum value.
		 */
		public void setChecksumSha1(String checksumSha1) {
			this.checksumSha1 = checksumSha1;
		}

		/**
		 *
		 * @return the url to the file that contains the sha1 checksum.
		 */
		public String getChecksumSha1Url() {
			return checksumSha1Url;
		}

		/**
		 * Establish the url where the checksumSha1 file exists..
		 *
		 * @param checksumSha1Url String containing the url.
		 */
		public void setChecksumSha1Url(String checksumSha1Url) {
			this.checksumSha1Url = checksumSha1Url;
		}

		/**
		 *
		 * @return the sha256 encoding that should be returned for the dependency.
		 */
		public String getChecksumSha256() {
			return checksumSha256;
		}

		/**
		 * Establish the checksumSha256 for the dependency.
		 *
		 * @param checksumSha256 String containing the checksum value.
		 */
		public void setChecksumSha256(String checksumSha256) {
			this.checksumSha256 = checksumSha256;
		}

		/**
		 *
		 * @return the url to the file that contains the sha256 checksum.
		 */
		public String getChecksumSha256Url() {
			return checksumSha256Url;
		}

		/**
		 * Establish the url where the checksumSha256 file exists..
		 *
		 * @param checksumSha256Url String containing the url.
		 */
		public void setChecksumSha256Url(String checksumSha256Url) {
			this.checksumSha256Url = checksumSha256Url;
		}
	}
}
