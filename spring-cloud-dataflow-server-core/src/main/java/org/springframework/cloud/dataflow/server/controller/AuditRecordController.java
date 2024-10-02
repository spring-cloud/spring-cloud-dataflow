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

package org.springframework.cloud.dataflow.server.controller;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.AuditRecord;
import org.springframework.cloud.dataflow.rest.resource.AuditRecordResource;
import org.springframework.cloud.dataflow.server.controller.support.InvalidDateRangeException;
import org.springframework.cloud.dataflow.server.repository.NoSuchAuditRecordException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for retrieving {@link AuditRecord}s.
 *
 * @author Gunnar Hillert
 * @author Daniel Serleg
 */
@RestController
@RequestMapping("/audit-records")
@ExposesResourceFor(AuditRecordResource.class)
public class AuditRecordController {

	private static final Logger logger = LoggerFactory.getLogger(AuditRecordController.class);

	/**
	 * The service that is responsible for retrieving Audit Records.
	 */
	private final AuditRecordService auditRecordService;

	/**
	 * Create a {@code AuditController} that delegates to {@link AuditRecordService}.
	 *
	 * @param auditRecordService the audit record service to use
	 */
	public AuditRecordController(AuditRecordService auditRecordService) {
		Assert.notNull(auditRecordService, "AuditRecordService must not be null");
		this.auditRecordService = auditRecordService;
	}

	/**
	 * Return a page-able list of {@link AuditRecordResource}s.
	 *
	 * @param pageable Pagination information
	 * @param assembler assembler for {@link AuditRecord}
	 * @param actions Optional. For which {@link AuditActionType}s do you want to retrieve
	 *     {@link AuditRecord}s
	 * @param fromDate Optional. The fromDate must be {@link DateTimeFormatter}.ISO_DATE_TIME
	 *     formatted. eg.: 2019-02-03T00:00:30
	 * @param toDate Optional. The toDate must be {@link DateTimeFormatter}.ISO_DATE_TIME
	 *     formatted. eg.: 2019-02-05T23:59:30
	 * @param operations Optional. For which {@link AuditOperationType}s do you want to
	 *     retrieve {@link AuditRecord}s
	 * @return list of audit records
	 */
	@GetMapping("")
	@ResponseStatus(HttpStatus.OK)
	public PagedModel<AuditRecordResource> list(Pageable pageable,
			@RequestParam(required = false) AuditActionType[] actions,
			@RequestParam(required = false) AuditOperationType[] operations,
			@RequestParam(required = false) String fromDate,
			@RequestParam(required = false) String toDate,
			PagedResourcesAssembler<AuditRecord> assembler) {

		final Instant fromDateAsInstant = paresStringToInstant(fromDate);
		final Instant toDateAsInstant = paresStringToInstant(toDate);

		if (fromDate != null && toDate != null && fromDate.compareTo(toDate) > 0) {
			throw new InvalidDateRangeException("The fromDate cannot be after the toDate.");
		}

		final Page<AuditRecord> auditRecords = this.auditRecordService
				.findAuditRecordByAuditOperationTypeAndAuditActionTypeAndDate(pageable, actions, operations,
						fromDateAsInstant,
						toDateAsInstant);
		return assembler.toModel(auditRecords, new Assembler(auditRecords));
	}

	/**
	 * Return a given {@link AuditRecordResource}.
	 *
	 * @param id the id of an existing audit record (required)
	 * @return the audit record or null if the audit record does not exist
	 */
	@GetMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public AuditRecordResource display(@PathVariable Long id) {
		AuditRecord auditRecord = this.auditRecordService.findById(id)
				.orElseThrow(() -> new NoSuchAuditRecordException(id));
		return new Assembler(new PageImpl<>(Collections.singletonList(auditRecord))).toModel(auditRecord);
	}

	/**
	 * Return an array of {@link AuditOperationType}s.
	 *
	 * @return Array of AuditOperationTypes
	 */
	@GetMapping("/audit-operation-types")
	@ResponseStatus(HttpStatus.OK)
	public AuditOperationType[] getAuditOperationTypes() {
		return AuditOperationType.values();
	}

	/**
	 * Return an array of {@link AuditActionType}s.
	 *
	 * @return Array of AuditActionTypes
	 */
	@GetMapping("/audit-action-types")
	@ResponseStatus(HttpStatus.OK)
	public AuditActionType[] getAuditActionTypes() {
		return AuditActionType.values();
	}

	private Instant paresStringToInstant(String textDate) {
		if (textDate == null) {
			return null;
		}

		LocalDateTime localDateTime = LocalDateTime.parse(textDate, DateTimeFormatter.ISO_DATE_TIME);
		return localDateTime.toInstant(ZoneOffset.UTC);
	}

	/**
	 * {@link org.springframework.hateoas.server.ResourceAssembler} implementation that converts
	 * {@link AuditRecord}s to {@link AuditRecordResource}s.
	 */
	class Assembler extends RepresentationModelAssemblerSupport<AuditRecord, AuditRecordResource> {

		public Assembler(Page<AuditRecord> auditRecords) {
			super(AuditRecordController.class, AuditRecordResource.class);
		}

		@Override
		public AuditRecordResource toModel(AuditRecord auditRecord) {
			try {
				return createModelWithId(auditRecord.getId(), auditRecord);
			}
			catch (IllegalStateException e) {
				logger.warn("Failed to create StreamDefinitionResource. " + e.getMessage());
			}
			return null;
		}

		@Override
		public AuditRecordResource instantiateModel(AuditRecord auditRecord) {
			final AuditRecordResource resource = new AuditRecordResource();
			resource.setAuditRecordId(auditRecord.getId());
			resource.setAuditAction(auditRecord.getAuditAction() != null ? auditRecord.getAuditAction().name() : null);
			resource.setAuditData(auditRecord.getAuditData());
			resource.setAuditOperation(
					auditRecord.getAuditOperation() != null ? auditRecord.getAuditOperation().name() : null);
			resource.setCorrelationId(auditRecord.getCorrelationId());
			resource.setCreatedBy(auditRecord.getCreatedBy());
			resource.setCreatedOn(auditRecord.getCreatedOn());
			resource.setPlatformName(auditRecord.getPlatformName());
			return resource;
		}

	}
}
