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
package org.springframework.cloud.dataflow.audit.repository.jpa;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.springframework.cloud.dataflow.audit.repository.AuditRecordRepositoryCustom;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.QueryUtils;

/**
 * Default implementation of {@link AuditRecordRepositoryCustom}.
 *
 * @author Daniel Serleg
 * @author Gunnar Hillert
 */
public class AuditRecordRepositoryImpl implements AuditRecordRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Page<AuditRecord> findByActionTypeAndOperationTypeAndDate(AuditOperationType[] operations,
			AuditActionType[] actions, Instant fromDate, Instant toDate, Pageable pageable) {

		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<AuditRecord> query = cb.createQuery(AuditRecord.class);
		final Root<AuditRecord> auditRecordRoot = query.from(AuditRecord.class);

		final Path<AuditActionType> auditAction = auditRecordRoot.get("auditAction");
		final Path<AuditOperationType> auditOperation = auditRecordRoot.get("auditOperation");
		final Path<Instant> createdOn = auditRecordRoot.get("createdOn");

		final Predicate datePredicate;
		if (fromDate != null && toDate == null) {
			datePredicate = cb.greaterThanOrEqualTo(createdOn, fromDate);
		}
		else if (fromDate == null && toDate != null) {
			datePredicate = cb.lessThanOrEqualTo(createdOn, toDate);
		}
		else if (fromDate != null && toDate != null) {
			datePredicate = cb.between(createdOn, fromDate, toDate);
		}
		else {
			datePredicate = null;
		}

		final List<Predicate> auditActionPredicates = new ArrayList<>();
		if (actions != null && actions.length > 0) {
			for (AuditActionType action : actions) {
				auditActionPredicates.add(cb.equal(auditAction, action));
			}
		}

		final List<Predicate> auditOperationsPredicates = new ArrayList<>();
		if (operations != null && operations.length > 0) {
			for (AuditOperationType operation : operations) {
				auditOperationsPredicates.add(cb.equal(auditOperation, operation));
			}
		}

		final List<Predicate> finalQueryPredicates = new ArrayList<>();
		if (!auditActionPredicates.isEmpty()) {
			final Predicate auditActionPredicatesOr = cb.or(auditActionPredicates.toArray(new Predicate[0]));
			finalQueryPredicates.add(auditActionPredicatesOr);
		}
		if (datePredicate != null) {
			finalQueryPredicates.add(datePredicate);
		}
		if (!auditOperationsPredicates.isEmpty()) {
			final Predicate auditOperationsPredicatesOr = cb.or(auditOperationsPredicates.toArray(new Predicate[0]));
			finalQueryPredicates.add(auditOperationsPredicatesOr);
		}

		final CriteriaQuery<AuditRecord> select = query.select(auditRecordRoot);

		if (!finalQueryPredicates.isEmpty()) {
			select.where(finalQueryPredicates.toArray(new Predicate[0]));
		}

		if (pageable.getSort().isUnsorted()) {
			select.orderBy(QueryUtils.toOrders(pageable.getSort().and(Sort.by("id")).ascending(), auditRecordRoot, cb));
		}
		else {
			select.orderBy(QueryUtils.toOrders(pageable.getSort(), auditRecordRoot, cb));
		}

		final TypedQuery<AuditRecord> typedQuery = entityManager.createQuery(select);
		typedQuery.setFirstResult((int) pageable.getOffset());
		typedQuery.setMaxResults(pageable.getPageSize());

		final List<AuditRecord> resultList = typedQuery.getResultList();

		final Long totalCount = (Long)entityManager.createQuery(((SqmSelectStatement)select).createCountQuery())
				  .getSingleResult();

		return new PageImpl<>(resultList, pageable, totalCount);
	}

}
