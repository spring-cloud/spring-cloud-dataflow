/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.cloud.common.security.support;

import java.util.Arrays;

import org.springframework.util.Assert;

/**
 * Defines the core security roles supported by Spring Cloud Security.
 *
 * @author Gunnar Hillert
 */
public enum CoreSecurityRoles {

	CREATE("CREATE", "role for create operations"),
	DEPLOY("DEPLOY", "role for deploy operations"),
	DESTROY("DESTROY", "role for destroy operations"),
	MANAGE("MANAGE", "role for the boot management endpoints"),
	MODIFY("MODIFY", "role for modify operations"),
	SCHEDULE("SCHEDULE", "role for scheduling operations"),
	VIEW("VIEW", "view role");

	private String key;

	private String name;

	CoreSecurityRoles(final String key, final String name) {
		this.key = key;
		this.name = name;
	}

	public static CoreSecurityRoles fromKey(String role) {

		Assert.hasText(role, "Parameter role must not be null or empty.");

		for (CoreSecurityRoles roleType : CoreSecurityRoles.values()) {
			if (roleType.getKey().equals(role)) {
				return roleType;
			}
		}

		return null;
	}

	/**
	 * Helper class that will return all role names as a string array.
	 *
	 * @return Never null
	 */
	public static String[] getAllRolesAsStringArray() {
		return Arrays.stream(CoreSecurityRoles.values()).map(CoreSecurityRoles::getKey)
				.toArray(size -> new String[size]);
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

}
