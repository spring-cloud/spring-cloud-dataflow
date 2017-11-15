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

	public String getImplementationUrl() {
		return implementationUrl;
	}

	public void setImplementationUrl(String implementationUrl) {
		this.implementationUrl = implementationUrl;
	}

	public String getCoreUrl() {
		return coreUrl;
	}

	public void setCoreUrl(String coreUrl) {
		this.coreUrl = coreUrl;
	}

	public String getDashboardUrl() {
		return dashboardUrl;
	}

	public void setDashboardUrl(String dashboardUrl) {
		this.dashboardUrl = dashboardUrl;
	}

	public String getShellUrl() {
		return shellUrl;
	}

	public void setShellUrl(String shellUrl) {
		this.shellUrl = shellUrl;
	}

	public String getDataflowCoreVersion() {
		return dataflowCoreVersion;
	}

	public void setDataflowCoreVersion(String dataflowCoreVersion) {
		this.dataflowCoreVersion = dataflowCoreVersion;
	}

	public String getDataflowDashboardVersion() {
		return dataflowDashboardVersion;
	}

	public void setDataflowDashboardVersion(String dataflowDashboardVersion) {
		this.dataflowDashboardVersion = dataflowDashboardVersion;
	}

	public String getDataflowShellVersion() {
		return dataflowShellVersion;
	}

	public void setDataflowShellVersion(String dataflowShellVersion) {
		this.dataflowShellVersion = dataflowShellVersion;
	}
}
