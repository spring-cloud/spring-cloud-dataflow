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

package org.springframework.cloud.dataflow.rest.resource.about;

import java.util.HashMap;
import java.util.Map;

import org.springframework.hateoas.RepresentationModel;

/**
 * Provides meta-information about the Spring Cloud Data Flow server.
 *
 * @author Gunnar Hillert
 * @author Felipe Gutierrez
 */
public class AboutResource extends RepresentationModel {

	private FeatureInfo featureInfo = new FeatureInfo();

	private VersionInfo versionInfo = new VersionInfo();

	private SecurityInfo securityInfo = new SecurityInfo();

	private RuntimeEnvironment runtimeEnvironment = new RuntimeEnvironment();

	private MonitoringDashboardInfo monitoringDashboardInfo = new MonitoringDashboardInfo();

	private Map<String,Object> gitAndBuildInfo = new HashMap<>();

	/**
	 * Default constructor for serialization frameworks.
	 */
	public AboutResource() {
	}

	public FeatureInfo getFeatureInfo() {
		return featureInfo;
	}

	public void setFeatureInfo(FeatureInfo featureInfo) {
		this.featureInfo = featureInfo;
	}

	public VersionInfo getVersionInfo() {
		return versionInfo;
	}

	public void setVersionInfo(VersionInfo versionInfo) {
		this.versionInfo = versionInfo;
	}

	public SecurityInfo getSecurityInfo() {
		return securityInfo;
	}

	public void setSecurityInfo(SecurityInfo securityInfo) {
		this.securityInfo = securityInfo;
	}

	public RuntimeEnvironment getRuntimeEnvironment() {
		return runtimeEnvironment;
	}

	public void setRuntimeEnvironment(RuntimeEnvironment runtimeEnvironment) {
		this.runtimeEnvironment = runtimeEnvironment;
	}

	public MonitoringDashboardInfo getMonitoringDashboardInfo() {
		return monitoringDashboardInfo;
	}

	public void setMonitoringDashboardInfo(MonitoringDashboardInfo monitoringDashboardInfo) {
		this.monitoringDashboardInfo = monitoringDashboardInfo;
	}

	public Map<String, Object> getGitAndBuildInfo() {
		return gitAndBuildInfo;
	}

	public void setGitAndBuildInfo(Map<String, Object> gitAndBuildInfo) {
		this.gitAndBuildInfo = gitAndBuildInfo;
	}
}
