package org.springframework.cloud.data.module;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Patrick Peralta
 */
public class ModuleKeyTests {

	@Test
	public void testUID() throws Exception {
		String group = "group";
		String name = "name";
		String label = "label";

		String uid = String.format("%s.%s.%s", group, name, label);
		ModuleKey key = new ModuleKey(group, name, label);
		assertEquals(uid, key.toUID());

		assertEquals(key, ModuleKey.fromUID(uid));
	}
}