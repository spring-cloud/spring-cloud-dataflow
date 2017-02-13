package org.springframework.cloud.dataflow.shell.command.support;

import org.springframework.util.Assert;

/**
 *
 * @author Gunnar Hillert
 *
 */
public enum RoleType {

	VIEW("ROLE_VIEW", "view role"),
	CREATE("ROLE_CREATE", "role for create operations"),
	MANAGE("ROLE_MANAGE", "role for the boot management endpoints");

	private String key;
	private String name;

	/**
	 * Constructor.
	 *
	 */
	RoleType(final String key, final String name) {
		this.key = key;
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public static RoleType fromKey(String role) {

		Assert.hasText(role, "Parameter role must not be null or empty.");

		for (RoleType roleType : RoleType.values()) {
			if (roleType.getKey().equals(role)) {
				return roleType;
			}
		}

		return null;
	}
}
