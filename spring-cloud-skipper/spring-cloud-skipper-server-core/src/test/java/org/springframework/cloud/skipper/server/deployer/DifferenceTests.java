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
package org.springframework.cloud.skipper.server.deployer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifest;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifestReader;
import org.springframework.cloud.skipper.domain.deployer.ApplicationManifestDifference;
import org.springframework.cloud.skipper.server.TestResourceUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ApplicationManifestDifferenceFactory.
 * @author Mark Pollack
 * @author Corneil du Plessis
 */
class DifferenceTests {

	private final SpringCloudDeployerApplicationManifestReader applicationManifestReader = new SpringCloudDeployerApplicationManifestReader();

	private final ApplicationManifestDifferenceFactory applicationManifestDifferenceFactory = new ApplicationManifestDifferenceFactory();

	@Test
	void versionDifference() {
		List<SpringCloudDeployerApplicationManifest> applicationManifestsV1 = getManifest("m1-v1.yml");

		List<SpringCloudDeployerApplicationManifest> applicationManifestsV2 = getManifest("m1-v2.yml");

		ApplicationManifestDifference applicationManifestDifference = applicationManifestDifferenceFactory
				.createApplicationManifestDifference(
						applicationManifestsV1.get(0).getApplicationName(),
						applicationManifestsV1.get(0),
						applicationManifestsV2.get(0));

		assertThat(applicationManifestDifference.areEqual()).isFalse();
		assertThat(applicationManifestDifference.getApiAndKindDifference().areEqual()).isTrue();
		assertThat(applicationManifestDifference.getMetadataDifference().areEqual()).isTrue();
		assertThat(applicationManifestDifference.getResourceAndVersionDifference().areEqual()).isFalse();
		assertThat(applicationManifestDifference.getApplicationPropertiesDifference().areEqual()).isTrue();
		assertThat(applicationManifestDifference.getDeploymentPropertiesDifference().areEqual()).isTrue();

		ApplicationManifestDifferenceSummaryGenerator summaryGenerator = new ApplicationManifestDifferenceSummaryGenerator();
		String summary = summaryGenerator.generateSummary(applicationManifestDifference);
		assertThat(summary).contains("version=(1.1.0.RELEASE, 1.2.0.RELEASE)");

	}

	@Test
	void appPropDifference() {
		List<SpringCloudDeployerApplicationManifest> applicationManifestsV1 = getManifest("m2-v1.yml");

		List<SpringCloudDeployerApplicationManifest> applicationManifestsV2 = getManifest("m2-v2.yml");

		for (int i = 0; i < 2; i++) {
			ApplicationManifestDifference applicationManifestDifference = applicationManifestDifferenceFactory
					.createApplicationManifestDifference(
							applicationManifestsV1.get(i).getApplicationName(),
							applicationManifestsV1.get(i),
							applicationManifestsV2.get(i));

			assertThat(applicationManifestDifference.areEqual()).isFalse();
			assertThat(applicationManifestDifference.getApiAndKindDifference().areEqual()).isTrue();
			assertThat(applicationManifestDifference.getMetadataDifference().areEqual()).isTrue();
			assertThat(applicationManifestDifference.getResourceAndVersionDifference().areEqual()).isTrue();
			assertThat(applicationManifestDifference.getApplicationPropertiesDifference().areEqual()).isFalse();
			assertThat(applicationManifestDifference.getDeploymentPropertiesDifference().areEqual()).isTrue();
			ApplicationManifestDifferenceSummaryGenerator summaryGenerator = new ApplicationManifestDifferenceSummaryGenerator();
			String summary = summaryGenerator.generateSummary(applicationManifestDifference);
			if (i == 0) {
				assertThat(summary).contains("log.level=(INFO, DEBUG)");
			}
			else {
				assertThat(summary).contains("log.level=(DEBUG, INFO)");
			}
		}
	}

	@Test
	void deploymentPropDifference() {
		List<SpringCloudDeployerApplicationManifest> applicationManifestsV1 = getManifest("m3-v1.yml");

		List<SpringCloudDeployerApplicationManifest> applicationManifestsV2 = getManifest("m3-v2.yml");

		for (int i = 0; i < 2; i++) {
			ApplicationManifestDifference applicationManifestDifference = applicationManifestDifferenceFactory
					.createApplicationManifestDifference(
							applicationManifestsV1.get(i).getApplicationName(),
							applicationManifestsV1.get(i),
							applicationManifestsV2.get(i));

			assertThat(applicationManifestDifference.areEqual()).isFalse();
			assertThat(applicationManifestDifference.getApiAndKindDifference().areEqual()).isTrue();
			assertThat(applicationManifestDifference.getMetadataDifference().areEqual()).isTrue();
			assertThat(applicationManifestDifference.getResourceAndVersionDifference().areEqual()).isTrue();
			assertThat(applicationManifestDifference.getApplicationPropertiesDifference().areEqual()).isTrue();
			assertThat(applicationManifestDifference.getDeploymentPropertiesDifference().areEqual()).isFalse();
			ApplicationManifestDifferenceSummaryGenerator summaryGenerator = new ApplicationManifestDifferenceSummaryGenerator();
			String summary = summaryGenerator.generateSummary(applicationManifestDifference);
			if (i == 0) {
				assertThat(summary).contains("memory=(1024, 2048)");
			}
			else {
				assertThat(summary).contains("memory=(2048, 1024)");
			}
		}
	}

	private List<SpringCloudDeployerApplicationManifest> getManifest(String filename) {
		String manifest = null;
		try {
			manifest = StreamUtils.copyToString(
					TestResourceUtils.qualifiedResource(getClass(), filename).getInputStream(),
					Charset.defaultCharset());
		}
		catch (IOException e) {
			throw new SkipperException("Error copying manifest", e);
		}
		return this.applicationManifestReader.read(manifest);
	}
}
