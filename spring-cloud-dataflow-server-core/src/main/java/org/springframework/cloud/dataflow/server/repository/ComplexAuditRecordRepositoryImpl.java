/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.repository;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Default implementation of the {@link ComplexAuditRecordRepository}. The query body is
 * depend on the input parameters after the evaluation, generate a dynamic query.
 *
 * @author Daniel Serleg
 */
public class ComplexAuditRecordRepositoryImpl implements ComplexAuditRecordRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Page<AuditRecord> findByActionTypeAndOperationTypeAndDate(AuditOperationType[] operations,
			AuditActionType[] actions, String fromDate, String toDate, Pageable pageable) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<AuditRecord> query = cb.createQuery(AuditRecord.class);
		Root<AuditRecord> auditRecordRoot = query.from(AuditRecord.class);

		Path<AuditActionType> auditAction = auditRecordRoot.get("auditAction");
		Path<AuditOperationType> auditOperation = auditRecordRoot.get("auditOperation");
		Path<Instant> createdOn = auditRecordRoot.get("createdOn");

		Predicate datePredicate = null;
		if (fromDate != null && toDate == null) {
			datePredicate = cb.greaterThanOrEqualTo(createdOn, convertStringToInstant(fromDate));
		}
		if (fromDate == null && toDate != null) {
			datePredicate = cb.lessThanOrEqualTo(createdOn, convertStringToInstant(toDate));
		}
		if (fromDate != null && toDate != null) {
			datePredicate = cb.between(createdOn, convertStringToInstant(fromDate), convertStringToInstant(toDate));
		}
		if (fromDate == null && toDate == null) {
			datePredicate = cb.and();
		}

		List<Predicate> auditPredicates = new ArrayList<>();
		if (actions != null) {
			for (AuditActionType action : actions) {
				auditPredicates.add(cb.equal(auditAction, action));
			}
		}
		if (operations != null) {
			for (AuditOperationType operation : operations) {
				auditPredicates.add(cb.equal(auditOperation, operation));
			}
		}
		if (actions == null && operations == null) {
			auditPredicates.add(cb.and());
		}

		Predicate[] auditPredicateArray = auditPredicates.toArray(new Predicate[0]);

		query.select(auditRecordRoot)
				.where(cb.and(datePredicate, cb.or(auditPredicateArray)));

		List<AuditRecord> resultList = entityManager.createQuery(query)
				.getResultList();

		return new PageImpl(resultList, pageable, resultList.size());
	}

	private Instant convertStringToInstant(String date) {
		final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
		return Instant.from(formatter.parse(date));
	}
}
