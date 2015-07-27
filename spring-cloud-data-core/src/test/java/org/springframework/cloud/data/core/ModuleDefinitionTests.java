package org.springframework.cloud.data.core;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Patrick Peralta
 */
public class ModuleDefinitionTests {

	@Test
	public void testBuilder() {
		ModuleDefinition definition = new ModuleDefinition.Builder()
				.setGroup("ticktock")
				.setIndex(0)
				.setModuleName("time")
				.setModuleLabel("label")
				.setBinding("output", "channel").build();

		assertEquals("ticktock", definition.getGroup());
		assertEquals("time", definition.getModuleName());
		assertEquals("label", definition.getModuleLabel());
		assertEquals(0, definition.getIndex());
		assertEquals(1, definition.getBindings().size());
		assertEquals("channel", definition.getBindings().get("output"));
	}

}