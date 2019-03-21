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

/**
 * Provides runtime environment details for either a deployer or task launcher.
 *
 * @author Gunnar Hillert
 * @see RuntimeEnvironment
 */
public class RuntimeEnvironmentDetails {

	/**
	 * The implementation version of this deployer.
	 */
	private String deployerImplementationVersion;

	/**
	 * The name of this deployer (could be simple class name).
	 */
	private String deployerName;

	/**
	 * The SPI version used by this deployer.
	 */
	private String deployerSpiVersion;

	/**
	 * The Java version used by this deployer.
	 */
	private String javaVersion;

	/**
	 * The deployment platform API for this deployer.
	 */
	private String platformApiVersion;

	/**
	 * The client library version used by this deployer.
	 */
	private String platformClientVersion;

	/**
	 * The version running on the host of the platform used by this deployer.
	 */
	private String platformHostVersion;

	/**
	 * Platform specific properties
	 */
	private Map<String, String> platformSpecificInfo = new HashMap<>();

	/**
	 * The deployment platform for this deployer.
	 */
	private String platformType;

	/**
	 * The Spring Boot version used by this deployer.
	 */
	private String springBootVersion;

	/**
	 * The Spring Framework version used by this deployer.
	 */
	private String springVersion;

	/**
	 * Default constructor for serialization frameworks.
	 */
	public RuntimeEnvironmentDetails() {
	}

	public String getDeployerImplementationVersion() {
		return deployerImplementationVersion;
	}

	public void setDeployerImplementationVersion(String deployerImplementationVersion) {
		this.deployerImplementationVersion = deployerImplementationVersion;
	}

	public String getDeployerName() {
		return deployerName;
	}

	public void setDeployerName(String deployerName) {
		this.deployerName = deployerName;
	}

	public String getDeployerSpiVersion() {
		return deployerSpiVersion;
	}

	public void setDeployerSpiVersion(String deployerSpiVersion) {
		this.deployerSpiVersion = deployerSpiVersion;
	}

	public String getJavaVersion() {
		return javaVersion;
	}

	public void setJavaVersion(String javaVersion) {
		this.javaVersion = javaVersion;
	}

	public String getPlatformApiVersion() {
		return platformApiVersion;
	}

	public void setPlatformApiVersion(String platformApiVersion) {
		this.platformApiVersion = platformApiVersion;
	}

	public String getPlatformClientVersion() {
		return platformClientVersion;
	}

	public void setPlatformClientVersion(String platformClientVersion) {
		this.platformClientVersion = platformClientVersion;
	}

	public String getPlatformHostVersion() {
		return platformHostVersion;
	}

	public void setPlatformHostVersion(String platformHostVersion) {
		this.platformHostVersion = platformHostVersion;
	}

	public Map<String, String> getPlatformSpecificInfo() {
		return platformSpecificInfo;
	}

	public void setPlatformSpecificInfo(Map<String, String> platformSpecificInfo) {
		this.platformSpecificInfo = platformSpecificInfo;
	}

	public String getPlatformType() {
		return platformType;
	}

	public void setPlatformType(String platformType) {
		this.platformType = platformType;
	}

	public String getSpringBootVersion() {
		return springBootVersion;
	}

	public void setSpringBootVersion(String springBootVersion) {
		this.springBootVersion = springBootVersion;
	}

	public String getSpringVersion() {
		return springVersion;
	}

	public void setSpringVersion(String springVersion) {
		this.springVersion = springVersion;
	}

}
