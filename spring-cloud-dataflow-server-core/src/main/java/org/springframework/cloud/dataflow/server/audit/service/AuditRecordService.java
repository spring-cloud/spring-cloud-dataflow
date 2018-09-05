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
package org.springframework.cloud.dataflow.server.audit.service;

import java.util.Map;

import org.springframework.cloud.dataflow.server.audit.domain.AuditActionType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditOperationType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditRecord;
import org.springframework.cloud.dataflow.server.audit.repository.AuditRecordRepository;

/**
 * Main interface to interact with the Spring Cloud Data Flow auditing service. Methods will create
 * {@link AuditRecord} instances and persist those to the underlying data store, e.g. {@link AuditRecordRepository}.
 *
 * Callers have the choice to persist just a String as the data to be audited or, alternatively, callers can
 * also provide a {@link Map} whose elements will be serialized to JSON using Jackson.
 *
 * @author Gunnar Hillert
 *
 */
public interface AuditRecordService {

	/**
	 * With a mandatory set of parameters, this method will create a {@link AuditRecord} instance and
	 * persist it to the underlying data store, e.g. {@link AuditRecordRepository}.
	 *
	 * @param auditActionType Must not be null
	 * @param auditOperationType Must not be null
	 * @param correlationId Id to identify the record. Must not be null or empty
	 * @param data The data as String to be audited. Must not be null or empty
	 */
	void populateAndSaveAuditRecord(
			AuditActionType auditActionType,
			AuditOperationType auditOperationType,
			String correlationId,
			String data);

	/**
	 * Similar to {@link #populateAndSaveAuditRecord(AuditActionType, AuditOperationType, String, String)} but
	 * instead of a String users can provide a {@link Map} as the Audit Data parameter, which will be persisted
	 * as a JSON a structure. Callers should therefore make sure that the provided Map can indeed be serialized
	 * to JSON via Jackson.
	 *
	 * @param auditActionType Must not be null
	 * @param auditOperationType Must not be null
	 * @param correlationId Id to identify the record. Must not be null or empty
	 * @param data The data as String to be audited. Must not be null or empty
	 *
	 * @throws IllegalStateException In case the Map containing the audit data cannot be serialized to JSON.
	 */
	void populateAndSaveAuditRecordUsingMapData(
			AuditActionType auditActionType,
			AuditOperationType auditOperationType,
			String correlationId,
			Map<String, Object> data);

}
