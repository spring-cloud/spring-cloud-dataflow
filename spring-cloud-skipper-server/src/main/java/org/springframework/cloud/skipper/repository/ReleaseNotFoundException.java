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
package org.springframework.cloud.skipper.repository;

import org.springframework.cloud.skipper.SkipperException;

/**
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings("serial")
public class ReleaseNotFoundException extends SkipperException {

	public ReleaseNotFoundException(String releaseName) {
		super(getExceptionMessage(releaseName));
	}

	public ReleaseNotFoundException(String releaseName, int version) {
		super(getExceptionMessage(releaseName, version));
	}

	public ReleaseNotFoundException(String releaseName, Throwable cause) {
		super(getExceptionMessage(releaseName), cause);
	}

	public static String getExceptionMessage(String releaseName) {
		return String.format("Release with the name [%s] doesn't exist", releaseName);
	}

	public static String getExceptionMessage(String releaseName, int version) {
		return String.format("Release with the name [%s] and version [%s] doesn't exist", releaseName, version);
	}
}
