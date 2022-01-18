/*
 * Copyright 2017-2019 the original author or authors.
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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.cloud.deployer.spi.app.ActuatorOperations;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.skipper.domain.deployer.ConfigurationMetadataPropertyEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;


/**
 * @author Mark Pollack
 */
@KeySpace("deployer")
public class Deployer {

	@Id
	private String id;

	private String name;

	private String type;

	private String description;

	@JsonIgnore
	private AppDeployer appDeployer;

	@JsonIgnore
	private ActuatorOperations actuatorOperations;

	private List<ConfigurationMetadataPropertyEntity> options = new ArrayList<>();

	Deployer() {
	}

	public Deployer(String name, String type, AppDeployer appDeployer, ActuatorOperations actuatorOperations) {
		this.name = name;
		this.type = type;
		this.appDeployer = appDeployer;
		this.actuatorOperations = actuatorOperations;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AppDeployer getAppDeployer() {
		return appDeployer;
	}

	public void setAppDeployer(AppDeployer deployer) {
		this.appDeployer = deployer;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<ConfigurationMetadataPropertyEntity> getOptions() {
		return options;
	}

	public void setOptions(List<ConfigurationMetadataPropertyEntity> options) {
		this.options = options;
	}

	public ActuatorOperations getActuatorOperations() {
		return actuatorOperations;
	}

	public void setActuatorOperations(ActuatorOperations actuatorOperations) {
		this.actuatorOperations = actuatorOperations;
	}
}
