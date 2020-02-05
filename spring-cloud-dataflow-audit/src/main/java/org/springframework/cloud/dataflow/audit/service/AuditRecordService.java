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
package org.springframework.cloud.dataflow.audit.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.cloud.dataflow.audit.repository.AuditRecordRepository;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Main interface to interact with the Spring Cloud Data Flow auditing service. Methods
 * will create {@link AuditRecord} instances and persist those to the underlying data
 * store, e.g. {@link AuditRecordRepository}.
 *
 * Callers have the choice to persist just a String as the data to be audited or,
 * alternatively, callers can also provide a {@link Map} whose elements will be serialized
 * to JSON using Jackson.
 *
 * @author Gunnar Hillert
 * @author Daniel Serleg
 */
public interface AuditRecordService {

	/**
	 * With a mandatory set of parameters, this method will create a {@link AuditRecord}
	 * instance and persist it to the underlying data store, e.g.
	 * {@link AuditRecordRepository}.
	 *
	 * @param auditOperationType Must not be null
	 * @param auditActionType Must not be null
	 * @param correlationId Id to identify the record
	 * @param data The data as String to be audited
	 * @param platformName the platform name
	 *
	 * @return newly created AuditRecord
	 */
	AuditRecord populateAndSaveAuditRecord(
			AuditOperationType auditOperationType,
			AuditActionType auditActionType,
			String correlationId,
			String data,
			String platformName);

	/**
	 * Similar to
	 * {@link #populateAndSaveAuditRecord(AuditOperationType, AuditActionType, String, String, String)}
	 * but instead of a String users can provide a {@link Map} as the Audit Data parameter,
	 * which will be persisted as a JSON a structure. Callers should therefore make sure that
	 * the provided Map can indeed be serialized to JSON via Jackson.
	 *
	 * @param auditOperationType Must not be null
	 * @param auditActionType Must not be null
	 * @param correlationId Id to identify the record
	 * @param data The data as a Map to be audited
	 * @param platformName the platform name
	 *
	 * @return newly created AuditRecord
	 */
	AuditRecord populateAndSaveAuditRecordUsingMapData(
			AuditOperationType auditOperationType, AuditActionType auditActionType,
			String correlationId,
			Map<String, Object> data, String platformName);

	/**
	 * Allows for querying of {@link AuditRecord}s.
	 *
	 * @param pageable Contains pagination information. If null, all {@link AuditRecord}s will
	 *     be returned
	 * @param actions Can be null. For which {@link AuditActionType}s shall
	 *     {@link AuditRecord}s be returned
	 * @param operations Can be null. For which {@link AuditOperationType}s shall
	 *     {@link AuditRecord}s be returned
	 * @param fromDate Can be null. The start date of the query records
	 * @param toDate Can be null. The end date of the query records
	 *
	 * @return a {@link Page} of {@link AuditRecord}s
	 */
	Page<AuditRecord> findAuditRecordByAuditOperationTypeAndAuditActionTypeAndDate(
			Pageable pageable,
			AuditActionType[] actions,
			AuditOperationType[] operations,
			Instant fromDate,
			Instant toDate);

	/**
	 * Find a single {@link AuditRecord} by providing a mandatory id.
	 *
	 * @param id Must not be null
	 * @return Audit Record
	 */
	Optional<AuditRecord> findById(Long id);

}
