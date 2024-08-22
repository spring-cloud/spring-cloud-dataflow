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

import java.util.Map;

import org.springframework.cloud.skipper.SkipperException;

/**
 * A kubernetes resource style representation of a Spring Boot application that will be
 * deployed using the Spring Cloud Deployer API.
 *
 * This class is commonly referred to as 'the manifest', meaning the complete list of the
 * application resource, properties, metadata and deployment properties. It is
 * serialized/deserialized from YAML. An example is: {@literal
 * apiVersion: skipper.spring.io/v1
 * kind: SpringCloudDeployerApplication
 * metadata:
 *   name: log-sink
 *   type: sink
 * spec:
 * resource: maven://org.springframework.cloud.stream.app:log-sink-rabbit:5.0.0
 * resourceMetadata: maven://org.springframework.cloud.stream.app:log-sink-rabbit:jar:metadata:5.0.0
 * applicationProperties:
 *  log.level: INFO
 *  log.expression: hellobaby
 * deploymentProperties:
 *   memory: 1024
 *   disk: 2
 * }
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class SpringCloudDeployerApplicationManifest {

	public static final String API_VERSION_STRING = "apiVersion";

	public static final String KIND_STRING = "kind";

	public static final String METADATA_STRING = "metadata";

	public static final String SPEC_STRING = "spec";

	protected SpringCloudDeployerApplicationSpec spec;

	private String apiVersion;

	private String kind;

	private Map<String, String> metadata;

	public SpringCloudDeployerApplicationManifest() {
	}

	public SpringCloudDeployerApplicationSpec getSpec() {
		return spec;
	}

	public void setSpec(SpringCloudDeployerApplicationSpec spec) {
		this.spec = spec;
	}

	public String getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	/**
	 * Return the value of the name property in the application's metadata.
	 * @return The value of the name
	 * @throws SkipperException if the name property can not be found in the metadata.
	 */
	public String getApplicationName() {
		if (!this.metadata.containsKey("name")) {
			throw new SkipperException("SpringCloudDeployerApplicationManifest must define a 'name' property in the metadata");
		}
		return this.metadata.get("name");
	}
}
