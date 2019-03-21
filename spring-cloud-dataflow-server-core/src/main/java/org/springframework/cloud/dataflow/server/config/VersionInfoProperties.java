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
 */
@ConfigurationProperties(prefix = VersionInfoProperties.VERSION_INFO_PREFIX)
public class VersionInfoProperties {

	public static final String VERSION_INFO_PREFIX = DataFlowPropertyKeys.PREFIX + "version-info";

	private String dataflowCoreVersion;

	private String dataflowDashboardVersion;

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

}
