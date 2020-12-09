/*
 * Copyright 2018-2019 the original author or authors.
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
 * The application area an {@link AuditRecord} is associated with.
 *
 * @author Gunnar Hillert
 *
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum AuditOperationType {

	APP_REGISTRATION(100L, "App Registration"),
	SCHEDULE(200L, "Schedule"),
	STREAM(300L, "Stream"),
	TASK(400L, "Task"),
	LOGIN(500L, "Task");

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

	public String getKey() {
		return name();
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
