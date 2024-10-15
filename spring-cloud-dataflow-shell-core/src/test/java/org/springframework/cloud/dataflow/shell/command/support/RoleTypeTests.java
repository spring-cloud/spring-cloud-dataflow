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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
class RoleTypeTests {

	@Test
	void getRoleFromNullKey() {
		try {
			RoleType.fromKey(null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Parameter role must not be null or empty.");
			return;
		}
		fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	void getRoleFromEmptyKey() {
		try {
			RoleType.fromKey("   ");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Parameter role must not be null or empty.");
			return;
		}
		fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	void getRoleFromNonExistingKey() {
		try {
			RoleType.fromKey("role_does_not_exist");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Unable to map role role_does_not_exist");
			return;
		}
		fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	void getRolesFromExistingKeys() {

		assertThat(RoleType.fromKey("ROLE_CREATE")).isEqualTo(RoleType.CREATE);
		assertThat(RoleType.fromKey("ROLE_DEPLOY")).isEqualTo(RoleType.DEPLOY);
		assertThat(RoleType.fromKey("ROLE_DESTROY")).isEqualTo(RoleType.DESTROY);
		assertThat(RoleType.fromKey("ROLE_MANAGE")).isEqualTo(RoleType.MANAGE);
		assertThat(RoleType.fromKey("ROLE_MODIFY")).isEqualTo(RoleType.MODIFY);
		assertThat(RoleType.fromKey("ROLE_SCHEDULE")).isEqualTo(RoleType.SCHEDULE);
		assertThat(RoleType.fromKey("ROLE_VIEW")).isEqualTo(RoleType.VIEW);

	}

}
