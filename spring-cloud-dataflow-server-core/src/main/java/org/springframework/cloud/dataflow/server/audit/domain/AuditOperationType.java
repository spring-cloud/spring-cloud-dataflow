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
 * The application area an {@link AuditRecord} is associated with.
 *
 * @author Gunnar Hillert
 *
 */
public enum AuditOperationType {

	STREAM_DEFINITIONS(100L, "Stream Definitions"),
	TASK_DEFINITIONS(  200L, "Task Definitions"),
	APP_REGISTRATIONS( 300L, "App Registrations"),
	SCHEDULES(         400L, "Schedules");

	private Long id;
	private String name;

	/**
	 * Constructor.
	 *
	 */
	AuditOperationType(final Long id, final String name) {
		this.id = id;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public static AuditOperationType fromId(Long auditOperationTypeId) {

		Assert.notNull(auditOperationTypeId, "Parameter auditOperationTypeId, must not be null.");

		for (AuditOperationType auditOperationType : AuditOperationType.values()) {
			if (auditOperationType.getId().equals(auditOperationTypeId)) {
				return auditOperationType;
			}
		}

		return null;
	}

}
