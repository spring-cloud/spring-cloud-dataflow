/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.dataflow.core;

import com.fasterxml.jackson.annotation.JsonFormat;

import org.springframework.util.Assert;

/**
 * Represent the various actions possible for Auditing events.
 *
 * @author Gunnar Hillert
 *
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum AuditActionType {

	CREATE(100L, "Create", "Create an Entity"),
	DELETE(200L, "Delete", "Delete an Entity"),
	DEPLOY(300L, "Deploy", "Deploy an Entity"),
	ROLLBACK(400L, "Rollback", "Rollback an Entity"),
	UNDEPLOY(500L, "Undeploy", "Undeploy an Entity"),
	UPDATE(600L, "Update", "Update an Entity"),
	LOGIN_SUCCESS(700L, "SuccessfulLogin", "Successful login");

	private Long id;

	private String name;

	private String description;

	/**
	 * Constructor.
	 *
	 */
	private AuditActionType(final Long id, final String name, final String description) {
		this.id = id;
		this.name = name;
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getKey() {
		return name();
	}

	public String getDescription() {
		return description;
	}

	public String getNameWithDescription() {
		return name + " (" + description + ")";
	}

	public static AuditActionType fromId(Long auditActionTypeId) {

		Assert.notNull(auditActionTypeId, "Parameter auditActionTypeId, must not be null.");

		for (AuditActionType auditActionType : AuditActionType.values()) {
			if (auditActionType.getId().equals(auditActionTypeId)) {
				return auditActionType;
			}
		}

		return null;
	}

}
