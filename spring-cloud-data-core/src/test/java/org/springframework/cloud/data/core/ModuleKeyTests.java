package org.springframework.cloud.data.core;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Patrick Peralta
 */
public class ModuleKeyTests {

	@Test
	public void testParse() throws Exception {
		String group = "ticktock";
		String label = "time-0";
		ModuleDefinition definition = new ModuleDefinition.Builder()
				.setGroup(group)
				.setName("time")
				.setLabel(label)
				.setBinding("output", "output")
				.build();
		String id = String.format("%s.%s", group,label);

		ModuleKey key = ModuleKey.fromModuleDefinition(definition);
		assertEquals(id, key.toString());
		assertEquals(key, ModuleKey.parse(id));
	}
}