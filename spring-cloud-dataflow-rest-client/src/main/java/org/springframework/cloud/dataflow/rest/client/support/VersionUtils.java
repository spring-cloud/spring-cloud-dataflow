/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.dataflow.rest.client.support;

import com.vdurmont.semver4j.Semver;

import org.springframework.util.StringUtils;

/**
 * Utility methods for handling client/server version manipulations
 *
 * @author Glenn Renfro
 */
public abstract class VersionUtils {

	private static String SEPARATOR = ".";

	/**
	 * Given a 3 or 4 part version return the 3 part version. Return empty string if supplied a
	 * {@code null} value. If there are not 3 or 4 parts in the provided version, return empty string.
	 * @param fourPartVersion The four part version of the string
	 * @return the three part version number,
	 */
	public static String getThreePartVersion(String fourPartVersion) {
		String threePartVersion = "";
		String[] versionTokens = StringUtils.delimitedListToStringArray(fourPartVersion, SEPARATOR);
		if (versionTokens.length == 3) {
			return fourPartVersion;
		}
		if (versionTokens.length != 4) {
			return threePartVersion;
		}
		return versionTokens[0] + SEPARATOR + versionTokens[1] + SEPARATOR + versionTokens[2];
	}

	/**
	 * Return true if the Data Flow Server version is greater than or equal to a given
	 * version.
	 * @param dataFlowServerVersion the data flow server version in three parts
	 * @param requiredVersion the required version to test against in three parts
	 * @return true if the Data Flow Server version is greater than or equal to a given
	 * version, false otherwise
	 */
	public static boolean isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion(String dataFlowServerVersion,
			String requiredVersion) {
		boolean result = false;
		String[] dataFlowServerVersionTokens = StringUtils.delimitedListToStringArray(dataFlowServerVersion, SEPARATOR);
		String[] requiredVersionTokens = StringUtils.delimitedListToStringArray(requiredVersion, SEPARATOR);
		if (dataFlowServerVersionTokens.length != 3 && requiredVersionTokens.length != 3) {
			return result;
		}
		try {
			Semver requiredVersionSemver = new Semver(requiredVersion);
			Semver dataFlowServerVersionSemver = new Semver(dataFlowServerVersion);
			result = dataFlowServerVersionSemver.isGreaterThanOrEqualTo(requiredVersionSemver);
		}
		catch (Exception e) {
			// always return a value.
		}
		return result;
	}
}
