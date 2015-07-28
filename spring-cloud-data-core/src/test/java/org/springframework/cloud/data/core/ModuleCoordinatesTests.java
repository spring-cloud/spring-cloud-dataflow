package org.springframework.cloud.data.core;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Patrick Peralta
 */
public class ModuleCoordinatesTests {
	public static final String GROUP_ID = "org.springframework";

	public static final String ARTIFACT_ID = "spring-core";

	public static final String VERSION = "5.0.0";

	@Test
	public void testParse() {
		validateModuleCoordinates(ModuleCoordinates.parse(
				String.format("%s:%s:%s", GROUP_ID, ARTIFACT_ID, VERSION)));
	}

	@Test
	public void testBuilder() {
		validateModuleCoordinates(new ModuleCoordinates.Builder()
				.setGroupId(GROUP_ID)
				.setArtifactId(ARTIFACT_ID)
				.setVersion(VERSION)
				.build());
	}

	private void validateModuleCoordinates(ModuleCoordinates coordinates) {
		assertEquals(GROUP_ID, coordinates.getGroupId());
		assertEquals(ARTIFACT_ID, coordinates.getArtifactId());
		assertEquals(VERSION, coordinates.getVersion());
	}

}