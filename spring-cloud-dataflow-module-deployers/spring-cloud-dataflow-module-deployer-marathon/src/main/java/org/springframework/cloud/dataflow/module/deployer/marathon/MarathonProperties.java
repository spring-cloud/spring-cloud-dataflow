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

package org.springframework.cloud.dataflow.module.deployer.marathon;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for connecting to a Marathon installation.
 *
 * @author Eric Bottard
 */
@ConfigurationProperties("marathon")
public class MarathonProperties {

	/**
	 * The location of the Marathon REST endpoint.
	 */
	private String apiEndpoint = "http://10.141.141.10:8080";

	/**
	 * The docker image to use for launching modules.
	 */
	private String image = "springcloud/stream-module-launcher";

	/**
	 * Additional arguments to pass to the module launcher.
	 */
	private Map<String, String> launcherProperties = new HashMap<>();

	/**
	 * How much memory to allocate per module, can be overridden at deployment time.
	 */
	private double memory = 128.0D;

	/**
	 * How many CPUs to allocate per module, can be overridden at deployment time.
	 */
	private double cpu = 0.5D;

	public double getMemory() {
		return memory;
	}

	public void setMemory(double memory) {
		this.memory = memory;
	}

	public double getCpu() {
		return cpu;
	}

	public void setCpu(double cpu) {
		this.cpu = cpu;
	}

	public String getApiEndpoint() {
		return apiEndpoint;
	}

	public void setApiEndpoint(String apiEndpoint) {
		this.apiEndpoint = apiEndpoint;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public Map<String, String> getLauncherProperties() {
		return launcherProperties;
	}

	public void setLauncherProperties(Map<String, String> launcherProperties) {
		this.launcherProperties = launcherProperties;
	}
}
