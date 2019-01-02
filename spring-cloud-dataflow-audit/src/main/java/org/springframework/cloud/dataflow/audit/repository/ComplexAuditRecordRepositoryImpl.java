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
package org.springframework.cloud.dataflow.audit.repository;

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

		List<Predicate> auditActionPredicates = new ArrayList<>();
		if (actions != null) {
			for (AuditActionType action : actions) {
				auditActionPredicates.add(cb.equal(auditAction, action));
			}
		}
		else {
			auditActionPredicates.add(cb.and());
		}

		List<Predicate> auditOperationsPredicates = new ArrayList<>();
		if (operations != null) {
			for (AuditOperationType operation : operations) {
				auditOperationsPredicates.add(cb.equal(auditOperation, operation));
			}
		}
		else {
			auditOperationsPredicates.add(cb.and());
		}

		Predicate[] auditActionsArray = auditActionPredicates.toArray(new Predicate[0]);
		Predicate[] auditOperationsArray = auditOperationsPredicates.toArray(new Predicate[0]);

		query.select(auditRecordRoot)
				.where(cb.and(datePredicate, cb.or(auditActionsArray), cb.or(auditOperationsArray)));

		List<AuditRecord> resultList = entityManager.createQuery(query)
				.getResultList();

		int pageNumber = pageable.getPageNumber();
		int pageSize = pageable.getPageSize();
		List<AuditRecord> records = createPage(pageNumber, pageSize, resultList);

		return new PageImpl<>(records, pageable, records.size());
	}

	private List<AuditRecord> createPage(int pageNumber, int pageSize, List<AuditRecord> collection) {
		int listSize = collection.size();
		int start = pageNumber * pageSize > listSize ? listSize : pageNumber * pageSize;
		int end = (start + pageSize) > listSize ? listSize : (start + pageSize);

		return collection.subList(start, end);
	}

	private Instant convertStringToInstant(String date) {
		final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
		return Instant.from(formatter.parse(date));
	}
}
