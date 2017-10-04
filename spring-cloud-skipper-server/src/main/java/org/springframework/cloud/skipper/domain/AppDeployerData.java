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
package org.springframework.cloud.skipper.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.Entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cloud.skipper.SkipperException;

/**
 * @author Mark Pollack
 */
@Entity
public class AppDeployerData extends AbstractEntity {

	private String releaseName;

	private Integer releaseVersion;

	// Store deployment Ids associated with the given release.
	private String deploymentData;

	public AppDeployerData() {
	}

	public String getReleaseName() {
		return releaseName;
	}

	public void setReleaseName(String releaseName) {
		this.releaseName = releaseName;
	}

	public Integer getReleaseVersion() {
		return releaseVersion;
	}

	public void setReleaseVersion(Integer releaseVersion) {
		this.releaseVersion = releaseVersion;
	}

	public String getDeploymentData() {
		return deploymentData;
	}

	public void setDeploymentData(String deploymentData) {
		this.deploymentData = deploymentData;
	}

	public Map<String, String> getDeploymentDataAsMap() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {
			};
			HashMap<String, String> result = mapper.readValue(this.deploymentData, typeRef);
			return result;
		}
		catch (Exception e) {
			throw new SkipperException("Could not parse appNameDeploymentIdMap JSON:" + this.deploymentData, e);
		}
	}

	public void setDeploymentDataUsingMap(Map<String, String> appNameDeploymentIdMap) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			setDeploymentData(objectMapper.writeValueAsString(appNameDeploymentIdMap));
		}
		catch (JsonProcessingException e) {
			throw new SkipperException("Could not serialize appNameDeploymentIdMap", e);
		}
	}

	public List<String> getDeploymentIds() {
		if (this.deploymentData != null) {
			Map<String, String> appNameDeploymentIdMap = this.getDeploymentDataAsMap();
			return appNameDeploymentIdMap.values().stream().collect(Collectors.toList());
		}
		else {
			return new ArrayList<>();
		}
	}
}
