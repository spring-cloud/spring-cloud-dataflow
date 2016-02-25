/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.app.resolver;

/**
 * Encapsulates Maven coordinates.
 *
 * @author Marius Bogoevici
 * @author Eric Bottard
 */
public class Coordinates {

	private final String groupId;

	private final String artifactId;

	private final String extension;

	private String classifier;

	private final String version;

	/**
	 * @param groupId the groupId
	 * @param artifactId the artifactId
	 * @param extension the file extension
	 * @param classifier classifier
	 * @param version the version
	 */
	public Coordinates(String groupId, String artifactId, String extension, String classifier, String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.extension = extension;
		this.classifier = classifier;
		this.version = version;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getExtension() {
		return extension;
	}

	public String getClassifier() {
		return classifier;
	}

	public String getVersion() {
		return version;
	}
}
