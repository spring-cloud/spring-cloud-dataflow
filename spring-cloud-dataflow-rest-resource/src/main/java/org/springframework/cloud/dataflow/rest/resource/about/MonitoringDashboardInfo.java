/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.resource.about;

import java.util.Locale;

import org.springframework.util.StringUtils;

/**
 * This entity will contains the dashboard configuration information exposed by the SCDF server.
 *
 * @author Christian Tzolov
 */
public class MonitoringDashboardInfo {

	/**
	 * Root URL to access the monitoring dashboards.
	 */
	private String url = "";

	/**
	 * Dashboard refresh interval in Seconds.
	 */
	private int refreshInterval = 15;

	/**
	 * Type of the monitoring dashboard system.
	 */
	private MonitoringDashboardType dashboardType = MonitoringDashboardType.NONE;

	/**
	 * Unique identifier of the SCDF installation within the monitoring system.
	 */
	private String source = "default-scdf-source";

	/**
	 * Default constructor for serialization frameworks.
	 */
	public MonitoringDashboardInfo() {
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getRefreshInterval() {
		return refreshInterval;
	}

	public void setRefreshInterval(int refreshInterval) {
		this.refreshInterval = refreshInterval;
	}

	public MonitoringDashboardType getDashboardType() {
		return dashboardType;
	}

	public void setDashboardType(MonitoringDashboardType dashboardType) {
		this.dashboardType = dashboardType;
	}

	public String getSource() {
		return StringUtils.isEmpty(source) ? source : source.toLowerCase(Locale.ROOT);
	}

	public void setSource(String source) {
		this.source = source;
	}
}
