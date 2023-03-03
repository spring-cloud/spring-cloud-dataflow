/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

import java.util.Arrays;

/**
 * Defines the possible schema versions that currently map to Spring {@code "Boot"}. A registered application can only support one schema version.
 *
 * <p>Each value defines the supported Spring Boot version that represents the changes in the schemas or Spring Batch and Task.</p>
 *
 * @author Chris Bono
 * @author Corneil du Plessis
 */
public enum AppBootSchemaVersion {

	BOOT2("2"),
	BOOT3("3");

	private String bootVersion;

	AppBootSchemaVersion(String bootVersion) {
		this.bootVersion = bootVersion;
	}

	public static AppBootSchemaVersion defaultVersion() {
		return BOOT2;
	}

	public static AppBootSchemaVersion fromBootVersion(String bootVersion) {
		return Arrays.stream(AppBootSchemaVersion.values())
				.filter((bv) -> bv.bootVersion.equals(bootVersion))
				.findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid AppBootSchemaVersion:" + bootVersion));
	}

	public String getBootVersion() {
		return this.bootVersion;
	}

	@Override
	public String toString() {
		return "AppBootVersion{bootVersion='" + this.bootVersion + "'}";
	}
}
