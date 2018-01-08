/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.server.deployer;

import com.google.common.base.Joiner;
import com.google.common.collect.MapDifference;

import org.springframework.cloud.skipper.domain.deployer.ApplicationManifestDifference;

/**
 * Generate a summary description of an {@link ApplicationManifestDifference}.
 * @author Mark Pollack
 */
public class ApplicationManifestDifferenceSummaryGenerator {

	private Joiner.MapJoiner mapJoiner = Joiner.on(",").withKeyValueSeparator("=");

	/**
	 * Generate a textual summary of the ApplicationManifestDifference
	 * @param applicationManifestDifference the ApplicationManifestDifference
	 * @return a textual summary
	 */
	public String generateSummary(ApplicationManifestDifference applicationManifestDifference) {
		StringBuffer stringBuffer = new StringBuffer();

		if (applicationManifestDifference.areEqual()) {
			stringBuffer.append("Existing and Replacing Applications are equal for Application Name=["
					+ applicationManifestDifference.getApplicationName() + "]\n\n");
		}
		else {
			stringBuffer.append("Existing and Replacing Applications are different for Application Name=["
					+ applicationManifestDifference.getApplicationName() + "]\n\n");

			if (!applicationManifestDifference.getApiAndKindDifference().areEqual()) {
				stringBuffer.append("API and Kind\n");
				stringBuffer.append("============\n");
				printMapDifference(stringBuffer, applicationManifestDifference.getApiAndKindDifference());
			}

			if (!applicationManifestDifference.getMetadataDifference().areEqual()) {
				stringBuffer.append("Metadata\n");
				stringBuffer.append("============\n");
				printMapDifference(stringBuffer, applicationManifestDifference.getMetadataDifference());
			}

			if (!applicationManifestDifference.getResourceAndVersionDifference().areEqual()) {
				stringBuffer.append("Resource and Version\n");
				stringBuffer.append("====================\n");
				printMapDifference(stringBuffer, applicationManifestDifference.getResourceAndVersionDifference());
			}

			if (!applicationManifestDifference.getApplicationPropertiesDifference().areEqual()) {
				stringBuffer.append("Application Properties\n");
				stringBuffer.append("======================\n");
				printMapDifference(stringBuffer, applicationManifestDifference.getApplicationPropertiesDifference());
			}

			if (!applicationManifestDifference.getDeploymentPropertiesDifference().areEqual()) {
				stringBuffer.append("Deployment Properties\n");
				stringBuffer.append("=====================\n");
				printMapDifference(stringBuffer, applicationManifestDifference.getDeploymentPropertiesDifference());
			}
		}

		return stringBuffer.toString();
	}

	private void printMapDifference(StringBuffer stringBuffer, MapDifference mapDifference) {
		if (!mapDifference.entriesDiffering().isEmpty()) {
			stringBuffer.append("Entries Differing\n");
			stringBuffer.append("-----------------\n");
			stringBuffer.append(mapJoiner.join(mapDifference.entriesDiffering()) + "\n\n");
		}
		if (!mapDifference.entriesOnlyOnLeft().isEmpty()) {
			stringBuffer.append("Entries only in existing app\n");
			stringBuffer.append("----------------------------\n");
			stringBuffer.append(mapJoiner.join(mapDifference.entriesOnlyOnLeft()) + "\n\n");
		}
		if (!mapDifference.entriesOnlyOnRight().isEmpty()) {
			stringBuffer.append("Entries only in replacing app\n");
			stringBuffer.append("-----------------------------\n");
			stringBuffer.append(mapJoiner.join(mapDifference.entriesOnlyOnRight()) + "\n\n");
		}
		if (!mapDifference.entriesInCommon().isEmpty()) {
			stringBuffer.append("Common Properties\n");
			stringBuffer.append("-----------------\n");
			stringBuffer.append(mapJoiner.join(mapDifference.entriesInCommon()) + "\n\n");
		}
		stringBuffer.append("\n");
	}
}
