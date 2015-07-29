/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Patrick Peralta
 * @author David Turanski
 */
public class ModuleCoordinatesTests {

	public static final String GROUP_ID = "org.springframework.cloud.stream.module";

	public static final String ARTIFACT_ID = "time-source";

	public static final String VERSION = "2.0.0";

	@Test
	public void testParseWithDefaults() {
		ModuleCoordinates expected = new ModuleCoordinates.Builder()
				.setGroupId(GROUP_ID)
				.setArtifactId(ARTIFACT_ID)
				.setVersion(VERSION)
				.build();
		validateModuleCoordinates(expected, ModuleCoordinates.parse(
				String.format("%s:%s:%s", GROUP_ID, ARTIFACT_ID, VERSION)));
	}

	@Test
	public void testParseWithExtensionAndClassifier() {
		ModuleCoordinates expected = new ModuleCoordinates.Builder()
				.setGroupId(GROUP_ID)
				.setArtifactId(ARTIFACT_ID)
				.setExtension(ModuleCoordinates.DEFAULT_EXTENSION)
				.setClassifier("exec")
				.setVersion(VERSION)
				.build();
		validateModuleCoordinates(expected, ModuleCoordinates.parse(
				String.format("%s:%s:%s:%s:%s", GROUP_ID, ARTIFACT_ID, ModuleCoordinates.DEFAULT_EXTENSION, "exec",
						VERSION)));
	}

	@Test
	public void testParseWithExtension() {
		ModuleCoordinates expected = new ModuleCoordinates.Builder()
				.setGroupId(GROUP_ID)
				.setArtifactId(ARTIFACT_ID)
				.setExtension("zip")
				.setVersion(VERSION)
				.build();
		validateModuleCoordinates(expected, ModuleCoordinates.parse(
				String.format("%s:%s:%s:%s", GROUP_ID, ARTIFACT_ID, "zip", VERSION)));
	}

	@Test
	public void testBuilder() {
		validateModuleCoordinates(
				new ModuleCoordinates.Builder()
						.setGroupId(GROUP_ID)
						.setArtifactId(ARTIFACT_ID)
						.setExtension(ModuleCoordinates.DEFAULT_EXTENSION)
						.setClassifier(ModuleCoordinates.EMPTY_CLASSIFIER)
						.setVersion(VERSION)
						.build(),
				new ModuleCoordinates.Builder()
						.setGroupId(GROUP_ID)
						.setArtifactId(ARTIFACT_ID)
						.setVersion(VERSION)
						.build());
	}

	private void validateModuleCoordinates(ModuleCoordinates expected, ModuleCoordinates actual) {
		assertEquals(expected.getGroupId(), actual.getGroupId());
		assertEquals(expected.getArtifactId(), actual.getArtifactId());
		assertEquals(expected.getVersion(), actual.getVersion());
		assertEquals(expected.getExtension(), actual.getExtension());
		assertEquals(expected.getClassifier(), actual.getClassifier());
		assertEquals(expected, actual);
	}

}
