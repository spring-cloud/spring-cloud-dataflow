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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.server.audit.domain.AuditActionType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditOperationType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditRecord;
import org.springframework.cloud.dataflow.server.audit.repository.AuditRecordRepository;
import org.springframework.cloud.dataflow.server.controller.support.ArgumentSanitizer;
import org.springframework.cloud.scheduler.spi.core.ScheduleInfo;
import org.springframework.cloud.scheduler.spi.core.ScheduleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link AuditRecordService}.
 *
 * @author Gunnar Hillert
 *
 */
public class DefaultAuditRecordService implements AuditRecordService {

	public static final String STREAM_DEFINITION_DSL_TEXT = "streamDefinitionDslText";
	public static final String TASK_DEFINITION_DSL_TEXT = "taskDefinitionDslText";
	public static final String TASK_DEFINITION_NAME = "taskDefinitionName";
	public static final String TASK_DEFINITION_PROPERTIES = "taskDefinitionProperties";

	public static final String DEPLOYMENT_PROPERTIES = "deploymentProperties";
	public static final String COMMANDLINE_ARGUMENTS = "commandlineArguments";

	private static final Logger logger = LoggerFactory.getLogger(DefaultAuditRecordService.class);

	private final AuditRecordRepository auditRecordRepository;

	private final ObjectMapper objectMapper;
	private final ArgumentSanitizer argumentSanitizer;

	public DefaultAuditRecordService(AuditRecordRepository auditRecordRepository) {
		Assert.notNull(auditRecordRepository, "auditRecordRepository must not be null.");
		this.auditRecordRepository = auditRecordRepository;
		this.objectMapper = new ObjectMapper();
		this.argumentSanitizer = new ArgumentSanitizer();
		this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}

	DefaultAuditRecordService(AuditRecordRepository auditRecordRepository, ObjectMapper objectMapper) {
		Assert.notNull(auditRecordRepository, "auditRecordRepository must not be null.");
		Assert.notNull(objectMapper, "objectMapper must not be null.");
		this.auditRecordRepository = auditRecordRepository;
		this.argumentSanitizer = new ArgumentSanitizer();
		this.objectMapper = objectMapper;
	}

	@Override
	public AuditRecord populateAndSaveAuditRecord(AuditOperationType auditOperationType,
			AuditActionType auditActionType,
			String correlationId, String data) {
		Assert.notNull(auditActionType, "auditActionType must not be null.");
		Assert.notNull(auditOperationType, "auditOperationType must not be null.");

		final AuditRecord auditRecord = new AuditRecord();
		auditRecord.setAuditAction(auditActionType);
		auditRecord.setAuditOperation(auditOperationType);
		auditRecord.setCorrelationId(correlationId);
		auditRecord.setAuditData(data);
		return this.auditRecordRepository.save(auditRecord);
	}

	@Override
	public AuditRecord populateAndSaveAuditRecordUsingMapData(AuditOperationType auditOperationType,
			AuditActionType auditActionType,
			String correlationId, Map<String, Object> data) {
		String dataAsString;
		try {
			dataAsString = objectMapper.writeValueAsString(data);
		}
		catch (JsonProcessingException e) {
			logger.error("Error serializing audit record data.  Data = " + data);
			dataAsString = "Error serializing audit record data.  Data = " + data;
		}
		return this.populateAndSaveAuditRecord(auditOperationType, auditActionType, correlationId, dataAsString);
	}

	@Override
	public Page<AuditRecord> findAuditRecordByAuditOperationTypeAndAuditActionType(
			Pageable pageable,
			AuditActionType[] actions,
			AuditOperationType[] operations) {

		if (actions != null && operations == null) {
			return this.auditRecordRepository.findByAuditActionIn(actions, pageable);
		}
		else if (actions == null && operations != null) {
			return this.auditRecordRepository.findByAuditOperationIn(operations, pageable);
		}
		else if (actions != null && operations != null) {
			return this.auditRecordRepository.findByAuditOperationInAndAuditActionIn(operations, actions, pageable);
		}
		else {
			return this.auditRecordRepository.findAll(pageable);
		}
	}

	@Override
	public AuditRecord findOne(Long id) {
		return this.auditRecordRepository.findOne(id);
	}

	@Override
	public void recordScheduleCreate(ScheduleRequest scheduleRequest) {
		Assert.notNull(scheduleRequest, "scheduleRequest must not be null");
		Assert.hasText(scheduleRequest.getScheduleName(), "The scheduleName of the scheduleRequest must not be null or empty");
		Assert.notNull(scheduleRequest.getDefinition(), "The task definition of the scheduleRequest must not be null");

		final Map<String, Object> auditedData = new HashMap<>(3);
		auditedData.put(TASK_DEFINITION_NAME, scheduleRequest.getDefinition().getName());

		if (scheduleRequest.getDefinition().getProperties() != null) {
			auditedData.put(TASK_DEFINITION_PROPERTIES, argumentSanitizer.sanitizeProperties(scheduleRequest.getDefinition().getProperties()));
		}

		if (scheduleRequest.getDeploymentProperties() != null) {
			auditedData.put(DEPLOYMENT_PROPERTIES, argumentSanitizer.sanitizeProperties(scheduleRequest.getDeploymentProperties()));
		}

		if (scheduleRequest.getCommandlineArguments() != null) {
			auditedData.put(COMMANDLINE_ARGUMENTS, argumentSanitizer.sanitizeArguments(scheduleRequest.getCommandlineArguments()));
		}

		this.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE, scheduleRequest.getScheduleName(), auditedData);
	}

	@Override
	public void recordScheduleDelete(ScheduleInfo scheduleInfo) {
		this.populateAndSaveAuditRecord(
				AuditOperationType.SCHEDULE,
				AuditActionType.DELETE, scheduleInfo.getScheduleName(),
				scheduleInfo.getTaskDefinitionName());
	}
}
