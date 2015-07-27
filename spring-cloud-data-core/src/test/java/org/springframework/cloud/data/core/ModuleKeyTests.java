package org.springframework.cloud.data.core;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Patrick Peralta
 */
public class ModuleKeyTests {

	@Test
	public void testUID() throws Exception {
		String group = "group";
		String label = "label";

		String uid = String.format("%s.%s", group,label);
		ModuleKey key = new ModuleKey(group, label);
		assertEquals(uid, key.toUID());

		assertEquals(key, ModuleKey.fromUID(uid));
	}
}