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
package org.springframework.cloud.dataflow.server.audit.repository;

import java.util.List;

import org.springframework.cloud.dataflow.server.audit.domain.AuditActionType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditOperationType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository interface for managing the {@link AuditRecord} class.
 *
 * @author Gunnar Hillert
 */
@Transactional
public interface AuditRecordRepository
		extends PagingAndSortingRepository<AuditRecord, Long> {

	@Override
	<S extends AuditRecord> S save(S s);

	@Override
	List<AuditRecord> findAll();

	Page<AuditRecord> findByAuditActionIn(AuditActionType[] actions, Pageable page);

	Page<AuditRecord> findByAuditOperationIn(AuditOperationType[] operations, Pageable pageable);

	Page<AuditRecord> findByAuditOperationInAndAuditActionIn(AuditOperationType[] operations, AuditActionType[] actions, Pageable pageable);
}
