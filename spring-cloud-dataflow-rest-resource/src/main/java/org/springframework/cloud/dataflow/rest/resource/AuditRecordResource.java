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

package org.springframework.cloud.dataflow.rest.resource;

import java.time.Instant;

import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;

/**
 * A HATEOAS representation of an {@link org.springframework.cloud.dataflow.core.AuditRecord}.
 * <p>
 * Note: this implementation is not thread safe.
 *
 * @author Gunnar Hillert
 */
public class AuditRecordResource extends RepresentationModel<AuditRecordResource> {

	/**
	 * The id of the audit record
	 */
	private Long auditRecordId;

	/**
	 * By whom was the audit record created. Can be null, in cases of disabled security.
	 */
	private String createdBy;

	/**
	 * An identifier that identifies (in combination with the {@link #auditOperation}) the audited operation.
	 */
	private String correlationId;

	/**
	 * Data associated with the audited operation.
	 */
	private String auditData;

	/**
	 * When was the audit record created?
	 */
	private Instant createdOn;

	/**
	 * What action did the user perform, e.g. create, update, delete, deploy
	 */
	private String auditAction;

	/**
	 * What operation (section of the app) was the user performing, e.g. task, streams
	 */
	private String auditOperation;

	/**
	 * Default constructor for serialization frameworks.
	 */
	public AuditRecordResource() {
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

	public String getAuditData() {
		return auditData;
	}

	public void setAuditData(String auditData) {
		this.auditData = auditData;
	}

	public Instant getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Instant createdOn) {
		this.createdOn = createdOn;
	}

	public String getAuditAction() {
		return auditAction;
	}

	public void setAuditAction(String auditAction) {
		this.auditAction = auditAction;
	}

	public String getAuditOperation() {
		return auditOperation;
	}

	public void setAuditOperation(String auditOperation) {
		this.auditOperation = auditOperation;
	}

	public Long getAuditRecordId() {
		return auditRecordId;
	}

	public void setAuditRecordId(Long auditRecordId) {
		this.auditRecordId = auditRecordId;
	}

	public static class Page extends PagedModel<AuditRecordResource> {
	}

}
