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
package org.springframework.cloud.skipper;

/**
 * A {@link SkipperException} indicating a missing {@code Skipper} release.
 *
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 */
@SuppressWarnings("serial")
public class ReleaseNotFoundException extends SkipperException {

	private final String releaseName;
	private final Integer releaseVersion;

	/**
	 * Instantiates a new {@code ReleaseNotFoundException}.
	 *
	 * @param releaseName the release name
	 */
	public ReleaseNotFoundException(String releaseName) {
		super(getExceptionMessage(releaseName));
		this.releaseName = releaseName;
		this.releaseVersion = null;
	}

	/**
	 * Instantiates a new {@code ReleaseNotFoundException}.
	 *
	 * @param releaseName the release name
	 * @param version the version
	 */
	public ReleaseNotFoundException(String releaseName, int version) {
		super(getExceptionMessage(releaseName, version));
		this.releaseName = releaseName;
		this.releaseVersion = version;
	}

	/**
	 * Instantiates a new {@code ReleaseNotFoundException}.
	 *
	 * @param releaseName the release name
	 * @param cause the cause
	 */
	public ReleaseNotFoundException(String releaseName, Throwable cause) {
		super(getExceptionMessage(releaseName), cause);
		this.releaseName = releaseName;
		this.releaseVersion = null;
	}

	/**
	 * Gets the release name.
	 *
	 * @return the release name
	 */
	public String getReleaseName() {
		return releaseName;
	}

	/**
	 * Gets the release version.
	 *
	 * @return the release version
	 */
	public Integer getReleaseVersion() {
		return releaseVersion;
	}

	private static String getExceptionMessage(String releaseName) {
		return String.format("Release with the name [%s] doesn't exist", releaseName);
	}

	private static String getExceptionMessage(String releaseName, int version) {
		return String.format("Release with the name [%s] and version [%s] doesn't exist", releaseName, version);
	}
}
