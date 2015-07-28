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
				.setName("time")
				.setLabel("label")
				.setBinding("output", "channel").build();

		assertEquals("ticktock", definition.getGroup());
		assertEquals("time", definition.getName());
		assertEquals("label", definition.getLabel());
		assertEquals(0, definition.getIndex());
		assertEquals(1, definition.getBindings().size());
		assertEquals("channel", definition.getBindings().get("output"));
	}

}