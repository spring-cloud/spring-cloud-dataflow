/*
 * Copyright 2017 the original author or authors.
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

	private String dataflowCoreVersion;

	private String dataflowDashboardVersion;

	private String dataflowShellVersion;

	private String implementationUrl;

	private String coreUrl;

	private String dashboardUrl;

	private String shellUrl;

	/**
	 * Retrieves the current {@link String} for the implementation url.
	 *
	 * @return {@link String} containing implementation url.
	 */
	public String getImplementationUrl() {
		return implementationUrl;
	}

	/**
	 * Establishes the implementation url.
	 *
	 * @param implementationUrl {@String} containing the implementation url.
	 */
	public void setImplementationUrl(String implementationUrl) {
		this.implementationUrl = implementationUrl;
	}

	/**
	 * Retrieves the current {@link String} for the core url.
	 *
	 * @return {@link String} containing core url.
	 */
	public String getCoreUrl() {
		return coreUrl;
	}

	/**
	 * Establishes the core url.
	 *
	 * @param coreUrl {@String} containing the core url.
	 */
	public void setCoreUrl(String coreUrl) {
		this.coreUrl = coreUrl;
	}

	/**
	 * Retrieves the current {@link String} for the dashboard url.
	 *
	 * @return {@link String} containing dashboard url.
	 */
	public String getDashboardUrl() {
		return dashboardUrl;
	}

	/**
	 * Establishes the dashboard url.
	 *
	 * @param dashboardUrl {@String} containing the dashboard url.
	 */
	public void setDashboardUrl(String dashboardUrl) {
		this.dashboardUrl = dashboardUrl;
	}

	/**
	 * Retrieves the current {@link String} for the shell url.
	 *
	 * @return {@link String} containing shell url.
	 */
	public String getShellUrl() {
		return shellUrl;
	}

	/**
	 * Establishes the shell url.
	 *
	 * @param shellUrl {@String} containing the shell url.
	 */
	public void setShellUrl(String shellUrl) {
		this.shellUrl = shellUrl;
	}

	/**
	 * Retrieves the current {@link String} for the core version.
	 *
	 * @return {@link String} containing core version.
	 */
	public String getDataflowCoreVersion() {
		return dataflowCoreVersion;
	}

	/**
	 * Establishes the data flow core version.
	 *
	 * @param dataflowCoreVersion {@String} containing the core version.
	 */
	public void setDataflowCoreVersion(String dataflowCoreVersion) {
		this.dataflowCoreVersion = dataflowCoreVersion;
	}

	/**
	 * Retrieves the current {@link String} for the dashboard version.
	 *
	 * @return {@link String} containing dashboard version.
	 */
	public String getDataflowDashboardVersion() {
		return dataflowDashboardVersion;
	}

	/**
	 * Establishes the data flow dashboard version.
	 *
	 * @param dataflowDashboardVersion {@String} containing the dashboard version.
	 */
	public void setDataflowDashboardVersion(String dataflowDashboardVersion) {
		this.dataflowDashboardVersion = dataflowDashboardVersion;
	}

	/**
	 * Retrieves the current {@link String} for the shell version.
	 *
	 * @return {@link String} containing shell version.
	 */
	public String getDataflowShellVersion() {
		return dataflowShellVersion;
	}

	/**
	 * Establishes the shell version.
	 *
	 * @param dataflowShellVersion {@String} containing the shell version.
	 */
	public void setDataflowShellVersion(String dataflowShellVersion) {
		this.dataflowShellVersion = dataflowShellVersion;
	}
}
