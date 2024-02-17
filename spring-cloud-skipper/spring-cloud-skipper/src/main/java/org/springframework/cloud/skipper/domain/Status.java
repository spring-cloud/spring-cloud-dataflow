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
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

import org.springframework.cloud.dataflow.common.persistence.type.DatabaseAwareLobUserType;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;

/**
 * Status contains release's status from the release management platform and the
 * corresponding deployment platform status.
 *
 * @author Mark Pollack
 */
@Entity
@Table(name = "SkipperStatus")
public class Status extends NonVersionedAbstractEntity {

	// Status from the Release managment platform
	@Enumerated(EnumType.STRING)
	private StatusCode statusCode;

	// Status from the underlying platform
	@Lob
	@Type(DatabaseAwareLobUserType.class)
	private String platformStatus;

	public Status() {
	}

	public StatusCode getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(StatusCode statusCode) {
		this.statusCode = statusCode;
	}

	public String getPlatformStatus() {
		return platformStatus;
	}

	public void setPlatformStatus(String platformStatus) {
		this.platformStatus = platformStatus;
	}

	@JsonIgnore
	public void setPlatformStatusAsAppStatusList(List<AppStatus> appStatusList) {
		ObjectMapper objectMapper = new ObjectMapper();
		// Avoids serializing objects such as OutputStreams in LocalDeployer.
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		try {
			this.platformStatus = objectMapper.writeValueAsString(appStatusList);
		}
		catch (JsonProcessingException e) {
			// TODO replace with SkipperException when it moves to domain module.
			throw new IllegalArgumentException("Could not serialize list of Application Status", e);
		}
	}

	@JsonIgnore
	public String getPlatformStatusPrettyPrint() {
		List<AppStatus> appStatusList = getAppStatusList();
		StringBuffer statusMsg = new StringBuffer();

		for (AppStatus appStatus : appStatusList) {
			statusMsg.append("[" + appStatus.getDeploymentId() + "]");
			if (appStatus.getInstances().isEmpty()) {
				statusMsg.append(", State = [" + appStatus.getState() + "]\n");
			}
			else {
				statusMsg.append(", State = [");
				for (AppInstanceStatus appInstanceStatus : appStatus.getInstances().values()) {
					statusMsg.append(appInstanceStatus.getId() + "=" + appInstanceStatus.getState() + "\n");
				}
				statusMsg.setLength(statusMsg.length() - 1);
				statusMsg.append("]\n");
			}
		}
		return statusMsg.toString();
	}

	@JsonIgnore
	public List<DeploymentState> getDeploymentStateList() {
		return getAppStatusList().stream().map(appStatus -> appStatus.getState()).collect(Collectors.toList());
	}

	@JsonIgnore
	public List<AppStatus> getAppStatusList() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.addMixIn(AppStatus.class, AppStatusMixin.class);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			SimpleModule module = new SimpleModule("CustomModel", Version.unknownVersion());
			SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
			resolver.addMapping(AppInstanceStatus.class, AppInstanceStatusImpl.class);
			module.setAbstractTypes(resolver);
			mapper.registerModule(module);
			TypeReference<List<AppStatus>> typeRef = new TypeReference<List<AppStatus>>() {
			};
			if (this.platformStatus != null) {
				return mapper.readValue(this.platformStatus, typeRef);
			}
			return new ArrayList<AppStatus>();
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Could not parse Skipper Platfrom Status JSON:" + platformStatus, e);
		}
	}

	@Override
	public String toString() {
		return "Status{" + "statusCode=" + statusCode + ", platformStatus='" + platformStatus + '\'' + '}';
	}
}
