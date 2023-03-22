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

/**
 *
 * This class is commonly referred to as 'the manifest', meaning the complete list of the
 * application resource, properties, metadata and deployment properties. It is
 * serialized/deserialized from YAML.
 * An example is: {@literal
 * apiVersion: skipper.spring.io/v1
 * kind: CloudFoundryApplication
 * spec:
 *   manifest:
 *     memory: 2048m
 * }
 *
 * @author Ilayaperumal Gopinathan
 */
public class CloudFoundryApplicationSkipperManifest {

	public static final String API_VERSION_STRING = "apiVersion";

	public static final String KIND_STRING = "kind";

	public static final String METADATA_STRING = "metadata";

	public static final String SPEC_STRING = "spec";

	protected CloudFoundryApplicationSpec spec;

	private String apiVersion;

	private String kind;

	public CloudFoundryApplicationSkipperManifest() {
	}

	public CloudFoundryApplicationSpec getSpec() {
		return spec;
	}

	public void setSpec(CloudFoundryApplicationSpec spec) {
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
}
