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

package org.springframework.cloud.data.core;


import org.springframework.util.Assert;

/**
 * The {@code ModuleCoordinates} class contains <a href="https://maven.apache.org/pom.html#Maven_Coordinates">
 * Maven coordinates</a> for a jar file containing a module.
 * <p>
 * To create a new instance, either use {@link Builder} to set the individual fields:
 * <pre>
 * new ModuleCoordinates.Builder()
 *     .setGroupId("org.springframework")
 *     .setArtifactId("spring-core")
 *     .setVersion("5.0.0")
 *     .build()
 * </pre>
 * ...or use {@link #parse(String)} to parse the coordinates as a colon delimited string:
 * <p>
 * <pre>
 * ModuleCoordinates.parse("org.springframework:spring-core:5.0.0);
 * </pre>
 *
 * @author David Turanski
 * @author Mark Fisher
 * @author Patrick Peralta
 */
public class ModuleCoordinates {

	/**
	 * Group ID for artifact; generally this includes the name of the
	 * organization that generated the artifact.
	 */
	private final String groupId;

	/**
	 * Artifact ID; generally this includes the name of the module.
	 */
	private final String artifactId;

	/**
	 * Version of the artifact.
	 */
	private final String version;

	/**
	 * Construct a {@code ModuleCoordinates} object.
	 *
	 * @param groupId     group ID for artifact
	 * @param artifactId  artifact ID
	 * @param version     artifact version
	 */
	private ModuleCoordinates(String groupId, String artifactId, String version) {
		Assert.hasLength(groupId, "'groupId' cannot be blank");
		Assert.hasLength(artifactId, "'artifactId' cannot be blank");
		Assert.hasLength(version, "'version' cannot be blank");
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

	/**
	 * @see #groupId
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * @see #artifactId
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * @see #version
	 */
	public String getVersion() {
		return version;
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ModuleCoordinates)) {
			return false;
		}

		ModuleCoordinates that = (ModuleCoordinates) o;

		return this.groupId.equals(that.groupId) &&
				this.artifactId.equals(that.artifactId) &&
				this.version.equals(that.version);
	}

	@Override
	public int hashCode() {
		int result = groupId.hashCode();
		result = 31 * result + artifactId.hashCode();
		result = 31 * result + version.hashCode();
		return result;
	}

	public static ModuleCoordinates parse(String id) {
		Assert.hasText(id);
		String[] fields = id.split(":");
		Assert.state(fields.length == 3, "invalid format for Maven coordinates: " + id);

		return new ModuleCoordinates(fields[0], fields[1], fields[2]);
	}

	public static class Builder {
		private String groupId;

		private String artifactId;

		private String version;

		public Builder setGroupId(String groupId) {
			this.groupId = groupId;
			return this;
		}

		public Builder setArtifactId(String artifactId) {
			this.artifactId = artifactId;
			return this;
		}

		public Builder setVersion(String version) {
			this.version = version;
			return this;
		}

		public ModuleCoordinates build() {
			return new ModuleCoordinates(groupId, artifactId, version);
		}
	}

}
