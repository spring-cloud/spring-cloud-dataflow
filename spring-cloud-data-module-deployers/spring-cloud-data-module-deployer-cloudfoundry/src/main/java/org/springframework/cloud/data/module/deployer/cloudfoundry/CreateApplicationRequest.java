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

package org.springframework.cloud.data.module.deployer.cloudfoundry;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A type used by RestTemplate requests that is made generic so that it can be extended by other Requests.
 * @param <T> The request type extending this one
 *
 * @author Steve Powell
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class CreateApplicationRequest<T extends CreateApplicationRequest<T>> {

	private String name;

	private Integer instances;

	private Integer memory;

	private String spaceId;

	private String state;

	private String buildpack;

	private Map<String, String> environment;

	public String getName() {
		return name;
	}

	public T withName(String name) {
		this.name = name;
		@SuppressWarnings("unchecked") T thisT = (T) this;
		return thisT;
	}

	public Integer getInstances() {
		return instances;
	}

	public T withInstances(int instances) {
		this.instances = instances;
		@SuppressWarnings("unchecked") T thisT = (T) this;
		return thisT;
	}

	public Integer getMemory() {
		return memory;
	}

	public T withMemory(int memory) {
		this.memory = memory;
		@SuppressWarnings("unchecked") T thisT = (T) this;
		return thisT;
	}

	@JsonProperty("space_guid")
	public String getSpaceId() {
		return spaceId;
	}

	public T withSpaceId(String spaceId) {
		this.spaceId = spaceId;
		@SuppressWarnings("unchecked") T thisT = (T) this;
		return thisT;
	}

	public String getState() {
		return state;
	}

	public T withState(String state) {
		this.state = state;
		@SuppressWarnings("unchecked") T thisT = (T) this;
		return thisT;
	}

	public String getBuildpack() {
		return buildpack;
	}

	public T withBuildpack(String buildpack) {
		this.buildpack = buildpack;
		@SuppressWarnings("unchecked") T thisT = (T) this;
		return thisT;
	}

	@JsonProperty("environment_json")
	public Map<String, String> getEnvironment() {
		return environment;
	}

	public T withEnvironment(Map<String, String> environment) {
		this.environment = environment;
		@SuppressWarnings("unchecked") T thisT = (T) this;
		return thisT;
	}
}
