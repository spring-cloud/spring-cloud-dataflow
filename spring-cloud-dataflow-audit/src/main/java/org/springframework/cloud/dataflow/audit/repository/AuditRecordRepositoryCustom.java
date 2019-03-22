/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.audit.repository;

import java.time.Instant;

import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repository interface for complex {@link AuditRecord} queries.
 *
 * @author Daniel Serleg
 * @author Gunnar Hillert
 */
public interface AuditRecordRepositoryCustom {

	/**
	 *
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
	Page<AuditRecord> findByActionTypeAndOperationTypeAndDate(AuditOperationType[] operations,
			AuditActionType[] actions, Instant fromDate, Instant toDate, Pageable pageable);
}
