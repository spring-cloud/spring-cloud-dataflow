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

import java.sql.Types;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.JdbcTypeCode;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Represents an audit entry. Used to record audit trail information.
 *
 * @author Gunnar Hillert
 * @author Christian Tzolov
 */
@Entity
@Table(name = "AuditRecords")
@EntityListeners(AuditingEntityListener.class)
public class AuditRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(name = "created_by")
	@CreatedBy
	private String createdBy;

	@Column(name = "correlation_id")
	private String correlationId;

	@Lob
	@JdbcTypeCode(Types.LONGVARCHAR)
	@Column(name = "audit_data")
	private String auditData;

	@CreatedDate
	@Column(name = "created_on")
	private Instant createdOn;

	@NotNull
	@Convert(converter = AuditActionTypeConverter.class)
	@Column(name = "audit_action")
	private AuditActionType auditAction;

	@NotNull
	@Convert(converter = AuditOperationTypeConverter.class)
	@Column(name = "audit_operation")
	private AuditOperationType auditOperation;

	@Column(name = "platformName")
	private String platformName;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Instant getCreatedDateTime() {
		return createdOn;
	}

	public AuditActionType getAuditAction() {
		return auditAction;
	}

	public void setAuditAction(AuditActionType auditAction) {
		this.auditAction = auditAction;
	}

	public AuditOperationType getAuditOperation() {
		return auditOperation;
	}

	public void setAuditOperation(AuditOperationType auditOperation) {
		this.auditOperation = auditOperation;
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

	public void setAuditData(String data) {
		this.auditData = data;
	}

	public Instant getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Instant createdOn) {
		this.createdOn = createdOn;
	}

	public String getPlatformName() {
		return platformName;
	}

	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	@Override
	public String toString() {
		return "AuditRecord [id=" + id + ", createdOn=" + createdOn + ", auditAction=" + auditAction
				+ ", auditOperation=" + auditOperation + ", platformName=" + platformName + "]";
	}
}
