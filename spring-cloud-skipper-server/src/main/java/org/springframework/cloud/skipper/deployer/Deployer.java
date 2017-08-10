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
package org.springframework.cloud.skipper.deployer;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
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

	@JsonIgnore
	private AppDeployer deployer;

	Deployer() {
	}

	public Deployer(String name, String type, AppDeployer deployer) {
		this.name = name;
		this.type = type;
		this.deployer = deployer;
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

	public AppDeployer getDeployer() {
		return deployer;
	}

	public void setDeployer(AppDeployer deployer) {
		this.deployer = deployer;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
