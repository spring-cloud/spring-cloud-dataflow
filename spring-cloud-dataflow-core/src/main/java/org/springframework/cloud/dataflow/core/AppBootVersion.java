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
 * Defines the possible {@code "Boot"} versions available for registered applications.
 *
 * <p>Each value defines the supported Spring Boot version as well as other information that is variant across the
 * versions and required by the system to handle each version.
 *
 * @author Chris Bono
 */
public enum AppBootVersion {

	BOOT2("Boot 2 Tasks/Jobs", "2", null, null),
	BOOT3("Boot 3 Tasks/Jobs", "3", "BOOT3_TASK_", "BOOT3_BATCH_");

	private String description;

	private String bootVersion;

	private String taskPrefix;

	private String batchPrefix;

	AppBootVersion(String description, String bootVersion, String taskPrefix, String batchPrefix) {
		this.description = description;
		this.bootVersion = bootVersion;
		this.taskPrefix = taskPrefix;
		this.batchPrefix = batchPrefix;
	}

	public static AppBootVersion fromBootVersion(String bootVersion) {
		return Arrays.stream(AppBootVersion.values())
				.filter((bv) -> bv.getBootVersion().equals(bootVersion))
				.findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported bootVersion: " + bootVersion));
	}

	public String getDescription() {
		return this.description;
	}

	public String getBootVersion() {
		return this.bootVersion;
	}

	public String getTaskPrefix() {
		return this.taskPrefix;
	}

	public String getBatchPrefix() {
		return this.batchPrefix;
	}

	@Override
	public String toString() {
		return String.format("AppBootVersion{description='%s', bootVersion='%s', taskPrefix='%s', batchPrefix='%s'}",
				this.description, this.bootVersion, this.taskPrefix, this.batchPrefix);
	}

}
