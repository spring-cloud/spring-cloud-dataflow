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
package org.springframework.cloud.dataflow.audit.repository;

import java.util.List;

import org.springframework.cloud.dataflow.core.AuditRecord;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository interface for managing the {@link AuditRecord} class.
 *
 * @author Gunnar Hillert
 * @author Daniel Serleg
 */
@Transactional
@Repository
public interface AuditRecordRepository
		extends PagingAndSortingRepository<AuditRecord, Long>, AuditRecordRepositoryCustom {

	@Override
	<S extends AuditRecord> S save(S s);

	@Override
	List<AuditRecord> findAll();

}
