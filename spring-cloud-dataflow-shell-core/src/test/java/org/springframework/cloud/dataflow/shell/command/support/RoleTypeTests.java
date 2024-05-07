/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.shell.command.support;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
public class RoleTypeTests {

	@Test
	public void testGetRoleFromNullKey() {
		try {
			RoleType.fromKey(null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("Parameter role must not be null or empty.", e.getMessage());
			return;
		}
		fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	public void testGetRoleFromEmptyKey() {
		try {
			RoleType.fromKey("   ");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Parameter role must not be null or empty.", e.getMessage());
			return;
		}
		fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	public void testGetRoleFromNonExistingKey() {
		try {
			RoleType.fromKey("role_does_not_exist");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Unable to map role role_does_not_exist", e.getMessage());
			return;
		}
		fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	public void testGetRolesFromExistingKeys() {

		assertEquals(RoleType.CREATE, RoleType.fromKey("ROLE_CREATE"));
		assertEquals(RoleType.DEPLOY, RoleType.fromKey("ROLE_DEPLOY"));
		assertEquals(RoleType.DESTROY, RoleType.fromKey("ROLE_DESTROY"));
		assertEquals(RoleType.MANAGE, RoleType.fromKey("ROLE_MANAGE"));
		assertEquals(RoleType.MODIFY, RoleType.fromKey("ROLE_MODIFY"));
		assertEquals(RoleType.SCHEDULE, RoleType.fromKey("ROLE_SCHEDULE"));
		assertEquals(RoleType.VIEW, RoleType.fromKey("ROLE_VIEW"));

	}

}
