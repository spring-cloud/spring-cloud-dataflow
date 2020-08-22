/*
 * Copyright 2016-2017 the original author or authors.
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

/**
 * Provides features information.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
public class FeatureInfo {

	private boolean analyticsEnabled = true;

	private boolean streamsEnabled = true;

	private boolean tasksEnabled = true;

	private boolean schedulesEnabled = false;

	/**
	 * This enum shows what SCDF monitoring dashboard configuration is exposed.
	 * It supports wavefront and grafana dashboards and none stands for no configuration provided.
	 */
	private MonitoringDashboardType monitoringDashboardType = MonitoringDashboardType.NONE;

	/**
	 * Default constructor for serialization frameworks.
	 */
	public FeatureInfo() {
	}

	public boolean isAnalyticsEnabled() {
		return this.analyticsEnabled;
	}

	public void setAnalyticsEnabled(boolean analyticsEnabled) {
		this.analyticsEnabled = analyticsEnabled;
	}

	public boolean isStreamsEnabled() {
		return this.streamsEnabled;
	}

	public void setStreamsEnabled(boolean streamsEnabled) {
		this.streamsEnabled = streamsEnabled;
	}

	public boolean isTasksEnabled() {
		return this.tasksEnabled;
	}

	public void setTasksEnabled(boolean tasksEnabled) {
		this.tasksEnabled = tasksEnabled;
	}

	public boolean isSchedulesEnabled() {
		return schedulesEnabled;
	}

	public void setSchedulesEnabled(boolean schedulesEnabled) {
		this.schedulesEnabled = schedulesEnabled;
	}

	public MonitoringDashboardType getMonitoringDashboardType() {
		return monitoringDashboardType;
	}

	public void setMonitoringDashboardType(MonitoringDashboardType monitoringDashboardType) {
		this.monitoringDashboardType = monitoringDashboardType;
	}
}
