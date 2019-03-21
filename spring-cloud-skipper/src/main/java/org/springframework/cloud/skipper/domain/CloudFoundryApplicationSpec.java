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
package org.springframework.cloud.skipper.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Specification to handle applications that can be deployed into Cloud Foundry platforms based
 * on their CF manifest configuration. Contained inside a {@link CloudFoundryApplicationSkipperManifest} instance.
 *
 * @author Ilayaperumal Gopinathan
 */
public class CloudFoundryApplicationSpec {

	private String resource;

	private String version;

	private Manifest manifest = new Manifest();

	public String getResource() {
		return this.resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Manifest getManifest() {
		return manifest;
	}

	public void setManifest(Manifest manifest) {
		this.manifest = manifest;
	}

	public static class Manifest {

		private String buildpack;
		private String command;
		private String memory;
		private String diskQuota;
		private Integer timeout;
		private Integer instances;
		private Boolean noHostname;
		private Boolean noRoute;
		private Boolean randomRoute;
		private HealthCheckType healthCheckType;
		private String healthCheckHttpEndpoint;
		private String stack;
		private List<String> services = new ArrayList<>();
		private List<String> domains = new ArrayList<>();
		private List<String> hosts = new ArrayList<>();
		private Map<String, Object> env = new HashMap<>();

		public String getBuildpack() {
			return buildpack;
		}

		public void setBuildpack(String buildpack) {
			this.buildpack = buildpack;
		}

		public String getCommand() {
			return command;
		}

		public void setCommand(String command) {
			this.command = command;
		}

		public List<String> getDomains() {
			return domains;
		}

		public void setDomains(List<String> domains) {
			this.domains = domains;
		}

		public Map<String, Object> getEnv() {
			return env;
		}

		public void setEnv(Map<String, Object> env) {
			this.env = env;
		}

		public HealthCheckType getHealthCheckType() {
			return healthCheckType;
		}

		public void setHealthCheckType(HealthCheckType healthCheckType) {
			this.healthCheckType = healthCheckType;
		}

		public String getHealthCheckHttpEndpoint() {
			return healthCheckHttpEndpoint;
		}

		public void setHealthCheckHttpEndpoint(String healthCheckHttpEndpoint) {
			this.healthCheckHttpEndpoint = healthCheckHttpEndpoint;
		}

		public List<String> getHosts() {
			return hosts;
		}

		public void setHosts(List<String> hosts) {
			this.hosts = hosts;
		}

		public Boolean getNoHostname() {
			return noHostname;
		}

		public void setNoHostname(Boolean noHostname) {
			this.noHostname = noHostname;
		}

		public Boolean getNoRoute() {
			return noRoute;
		}

		public void setNoRoute(Boolean noRoute) {
			this.noRoute = noRoute;
		}

		public Boolean getRandomRoute() {
			return randomRoute;
		}

		public void setRandomRoute(Boolean randomRoute) {
			this.randomRoute = randomRoute;
		}

		public String getMemory() {
			return memory;
		}

		public void setMemory(String memory) {
			this.memory = memory;
		}

		public String getDiskQuota() {
			return diskQuota;
		}

		public void setDiskQuota(String diskQuota) {
			this.diskQuota = diskQuota;
		}

		public Integer getTimeout() {
			return timeout;
		}

		public void setTimeout(Integer timeout) {
			this.timeout = timeout;
		}

		public Integer getInstances() {
			return instances;
		}

		public void setInstances(Integer instances) {
			this.instances = instances;
		}

		public List<String> getServices() {
			return services;
		}

		public void setServices(List<String> services) {
			this.services = services;
		}

		public String getStack() {
			return stack;
		}

		public void setStack(String stack) {
			this.stack = stack;
		}
	}

	public enum HealthCheckType {
		port, process, http;
	}
}
