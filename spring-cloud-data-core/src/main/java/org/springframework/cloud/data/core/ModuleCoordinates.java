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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The {@code ModuleCoordinates} class contains <a href="https://maven.apache.org/pom.html#Maven_Coordinates">
 * Maven coordinates</a> for a jar file containing a module.
 * <p>
 * To create a new instance, either use {@link Builder} to set the individual fields:
 * <pre>
 * new ModuleCoordinates.Builder()
 *     .setGroupId("org.springframework.cloud.stream.module")
 *     .setArtifactId("time-source")
 *     .setExtension("jar") //optional
 *     .setClassifier("exec") //optional
 *     .setVersion("2.0.0")
 *     .build()
 * </pre>
 * ...or use {@link #parse(String)} to parse the coordinates as a colon delimited string:
 * <code>&lt;groupId&gt;:&lt;artifactId&gt;[:&lt;extension&gt;[:&lt;classifier&gt;]]:&lt;version&gt;</code>
 * <pre>
 * ModuleCoordinates.parse("org.springframework.cloud.stream.module:some-module:2.0.0);
 * ModuleCoordinates.parse("org.springframework.cloud.stream.module:time-source:jar:exec:2.0.0);
 * </pre>
 * </p>
 * @author David Turanski
 * @author Mark Fisher
 * @author Patrick Peralta
 */
public class ModuleCoordinates {

	/**
	 * The default extension for the artifact.
	 */
	final static String DEFAULT_EXTENSION = "jar";

	/**
	 * String representing an empty classifier.
	 */
	final static String EMPTY_CLASSIFIER = "";

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
	 * Extension of the artifact.
	 */
	private final String extension;

	/**
	 * Classifier of the artifact.
	 */
	private final String classifier;

	/**
	 * Version of the artifact.
	 */
	private final String version;


	/**
	 * Construct a {@code ModuleCoordinates} object.
	 * @param groupId group ID for artifact
	 * @param artifactId artifact ID
	 * @param extension the file extension
	 * @param classifier artifact classifier - can be null
	 * @param version artifact version
	 */
	private ModuleCoordinates(String groupId, String artifactId, String extension, String classifier, String version) {
		Assert.hasText(groupId, "'groupId' cannot be blank");
		Assert.hasText(artifactId, "'artifactId' cannot be blank");
		Assert.hasText(extension, "'extension' cannot be blank");
		Assert.hasText(version, "'version' cannot be blank");

		this.groupId = groupId;
		this.artifactId = artifactId;
		this.extension = extension;
		this.classifier = classifier == null ? EMPTY_CLASSIFIER : classifier;
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
	 * @see #extension
	 */
	public String getExtension() {
		return extension;
	}

	/**
	 * @see #version
	 */
	public String getClassifier() {
		return classifier;
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
				this.extension.equals(that.extension) &&
				this.classifier.equals(that.classifier) &&
				this.version.equals(that.version);
	}

	@Override
	public int hashCode() {
		int result = groupId.hashCode();
		result = 31 * result + artifactId.hashCode();
		result = 31 * result + extension.hashCode();
		if (StringUtils.hasLength(classifier)) {
			result = 31 * result + classifier.hashCode();
		}
		result = 31 * result + version.hashCode();
		return result;
	}

	/**
	 * Returns the coordinates encoded as
	 * &lt;groupId&gt;:&lt;artifactId&gt;[:&lt;extension&gt;[:&lt;classifier&gt;]]:&lt;version&gt;,
	 * conforming to the <a href="http://www.eclipse.org/aether">Aether</a> convention.
	 */
	@Override
	public String toString() {
		return StringUtils.hasLength(classifier) ?
				String.format("%s:%s:%s:%s:%s", groupId, artifactId, extension, classifier, version) :
				String.format("%s:%s:%s:%s", groupId, artifactId, extension, version);
	}

	/**
	 * Parse coordinates given as a colon delimited string.
	 *
	 * @param coordinates coordinates encoded as <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>,
	 * conforming to the <a href="http://www.eclipse.org/aether">Aether</a> convention.
	 * @return the instance
	 */
	public static ModuleCoordinates parse(String coordinates) {
		Assert.hasText(coordinates);

		Pattern p = Pattern.compile("([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?:([^: ]+)");
		Matcher m = p.matcher(coordinates);
		Assert.isTrue(m.matches(), "Bad artifact coordinates " + coordinates
				+ ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");

		String groupId = m.group(1);
		String artifactId = m.group(2);
		String extension = StringUtils.hasLength(m.group(4)) ? m.group(4) : DEFAULT_EXTENSION;
		String classifier = StringUtils.hasLength(m.group(6)) ? m.group(6) : EMPTY_CLASSIFIER;
		String version = m.group(7);

		return new ModuleCoordinates(groupId, artifactId, extension, classifier, version);
	}


	public static class Builder {

		private String groupId;

		private String artifactId;

		private String extension = DEFAULT_EXTENSION;

		private String classifier = EMPTY_CLASSIFIER;

		private String version;

		public Builder setGroupId(String groupId) {
			this.groupId = groupId;
			return this;
		}

		public Builder setArtifactId(String artifactId) {
			this.artifactId = artifactId;
			return this;
		}

		public Builder setExtension(String extension) {
			this.extension = extension;
			return this;
		}

		public Builder setClassifier(String classifier) {
			this.classifier = classifier;
			return this;
		}


		public Builder setVersion(String version) {
			this.version = version;
			return this;
		}

		public ModuleCoordinates build() {
			return new ModuleCoordinates(groupId, artifactId, extension, classifier, version);
		}
	}

}
