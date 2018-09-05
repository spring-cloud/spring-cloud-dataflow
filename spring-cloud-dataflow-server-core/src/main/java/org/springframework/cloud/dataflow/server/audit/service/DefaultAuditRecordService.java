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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cloud.dataflow.server.audit.domain.AuditActionType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditOperationType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditRecord;
import org.springframework.cloud.dataflow.server.audit.repository.AuditRecordRepository;
import org.springframework.util.Assert;


/**
 * Default implementation of the {@link AuditRecordService}.
 *
 * @author Gunnar Hillert
 *
 */
public class DefaultAuditRecordService implements AuditRecordService {

	private final AuditRecordRepository auditRecordRepository;

	private final ObjectMapper objectMapper;

	public DefaultAuditRecordService(AuditRecordRepository auditRecordRepository, ObjectMapper objectMapper) {
		Assert.notNull(auditRecordRepository, "auditRecordRepository must not be null.");
		Assert.notNull(objectMapper, "objectMapper must not be null.");
		this.auditRecordRepository = auditRecordRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public void populateAndSaveAuditRecord(AuditOperationType auditOperationType, AuditActionType auditActionType,
			String correlationId, String data) {
		Assert.notNull(auditActionType, "auditActionType must not be null.");
		Assert.notNull(auditOperationType, "auditOperationType must not be null.");
		Assert.hasText(correlationId, "correlationId must not be null nor empty.");
		Assert.hasText(data, "data must not be null nor empty.");

		final AuditRecord auditRecord = new AuditRecord();
		auditRecord.setAuditAction(auditActionType);;
		auditRecord.setAuditOperation(auditOperationType);;
		auditRecord.setCorrelationId(correlationId);
		auditRecord.setAuditData(data);
		this.auditRecordRepository.save(auditRecord);
	}

	@Override
	public void populateAndSaveAuditRecordUsingMapData(AuditOperationType auditOperationType, AuditActionType auditActionType,
			String correlationId, Map<String, Object> data) {

		Assert.notEmpty(data, "data map must not be null and must contain at least 1 entry.");

		final String dataAsString;

		try {
			dataAsString = objectMapper.writeValueAsString(data);
		}
		catch (JsonProcessingException e) {
			throw new IllegalStateException("Error serializing audit record data.", e);
		}

		this.populateAndSaveAuditRecord(auditOperationType, auditActionType, correlationId, dataAsString);
	}

}
