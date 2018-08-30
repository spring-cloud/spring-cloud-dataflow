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

package org.springframework.cloud.dataflow.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.rest.resource.AuditRecordResource;
import org.springframework.cloud.dataflow.server.audit.domain.AuditActionType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditOperationType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditRecord;
import org.springframework.cloud.dataflow.server.audit.service.AuditRecordService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for retrieving {@link AuditRecord}s.
 *
 * @author Gunnar Hillert
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
	 * @param action Optional. For which {@link AuditActionType} do you want to retrieve {@link AuditRecord}s
	 * @param operation Optional. For which {@link AuditOperationType} do you want to retrieve {@link AuditRecord}s
	 * @return list of audit records
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<AuditRecordResource> list(Pageable pageable,
			@RequestParam(required = false) AuditActionType action,
			@RequestParam(required = false) AuditOperationType operation,
			PagedResourcesAssembler<AuditRecord> assembler) {
		Page<AuditRecord> auditRecords = this.auditRecordService
			.findAuditRecordByAuditOperationTypeAndAuditActionType(pageable, action, operation);
		return assembler.toResource(auditRecords, new Assembler(auditRecords));
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation that converts
	 * {@link AuditRecord}s to {@link AuditRecordResource}s.
	 */
	class Assembler extends ResourceAssemblerSupport<AuditRecord, AuditRecordResource> {

		public Assembler(Page<AuditRecord> auditRecords) {
			super(AuditRecordController.class, AuditRecordResource.class);
		}

		@Override
		public AuditRecordResource toResource(AuditRecord auditRecord) {
			try {
				return createResourceWithId(auditRecord.getId(), auditRecord);
			}
			catch (IllegalStateException e) {
				logger.warn("Failed to create StreamDefinitionResource. " + e.getMessage());
			}
			return null;
		}

		@Override
		public AuditRecordResource instantiateResource(AuditRecord auditRecord) {
			final AuditRecordResource resource = new AuditRecordResource();
			resource.setAuditAction(auditRecord.getAuditAction().getName());
			resource.setAuditData(auditRecord.getAuditData());
			resource.setAuditOperation(auditRecord.getAuditOperation().getName());
			resource.setCorrelationId(auditRecord.getCorrelationId());
			resource.setCreatedBy(auditRecord.getCreatedBy());
			resource.setCreatedOn(auditRecord.getCreatedOn());
			resource.setServerHost(auditRecord.getServerHost());
			return resource;
		}

	}
}
