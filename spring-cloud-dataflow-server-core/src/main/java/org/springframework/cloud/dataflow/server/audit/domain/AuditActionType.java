/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.server.audit.domain;

import org.springframework.util.Assert;

/**
 * Represent the various actions possible for Auditing events.
 *
 * @author Gunnar Hillert
 *
 */
public enum AuditActionType {

	CREATE(100L,   "Create", "Create an Entity"),
	UPDATE(200L,   "Update", "Update an Entity"),
	ROLLBACK(210L, "Rollback", "Rollback an Entity"),
	DELETE(300L,   "Delete", "Delete an Entity"),
	DEPLOY(400L,   "Deploy", "Deploy an Entity"),
	UNDEPLOY(500L, "Undeploy", "Undeploy an Entity");

	private Long id;
	private String name;
	private String description;

	/**
	 * Constructor.
	 *
	 */
	AuditActionType(final Long id, final String name, final String description) {
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

	public String getDescription() {
		return description;
	}

	public String getNameWithDescription() {
		return name + " (" + description + ")";
	}

	public static AuditActionType fromId(Long auditTypeId) {

		Assert.notNull(auditTypeId, "Parameter auditTypeId, must not be null.");

		for (AuditActionType auditActionType : AuditActionType.values()) {
			if (auditActionType.getId().equals(auditTypeId)) {
				return auditActionType;
			}
		}

		return null;
	}

}
