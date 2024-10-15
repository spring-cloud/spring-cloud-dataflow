/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.cloud.dataflow.registry.repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of the
 * {@link org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepositoryCustom}
 * interface.
 * @author Siddhant Sorann
 */
public class AppRegistrationRepositoryImpl implements AppRegistrationRepositoryCustom {

	private final EntityManager entityManager;

	@Autowired
	private ObjectProvider<AppRegistrationRepository> appRegistrationRepository;

	public AppRegistrationRepositoryImpl(EntityManager entityManager) {
		Assert.notNull(entityManager, "Entity manager cannot be null");
		this.entityManager = entityManager;
	}

	@Override
	public Page<AppRegistration> findAllByTypeAndNameIsLikeAndVersionAndDefaultVersion(
			ApplicationType type,
			String name, String version, boolean defaultVersion, Pageable pageable) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<AppRegistration> cq = cb.createQuery(AppRegistration.class);
		Root<AppRegistration> appRegistrationRoot = cq.from(AppRegistration.class);
		final List<Predicate> predicates = new ArrayList<>();
		if (type != null) {
			predicates.add(cb.equal(appRegistrationRoot.get("type"), type));
		}
		if (StringUtils.hasText(name)) {
			predicates.add(cb.like(cb.lower(appRegistrationRoot.get("name")), "%" + name.toLowerCase() + "%"));
		}
		if (StringUtils.hasText(version)) {
			predicates.add(cb.equal(cb.lower(appRegistrationRoot.get("version")), version.toLowerCase()));
		}
		if (defaultVersion) {
			predicates.add(cb.isTrue(appRegistrationRoot.get("defaultVersion")));
		}
		cq.where(predicates.toArray(new Predicate[0]));
		cq.orderBy(QueryUtils.toOrders(pageable.getSort(), appRegistrationRoot, cb));
		TypedQuery<AppRegistration> query = entityManager.createQuery(cq);
		query.setFirstResult((int) pageable.getOffset());
		query.setMaxResults(pageable.getPageSize());
		final List<AppRegistration> resultList = query.getResultList();
		if (defaultVersion) {
			resultList.forEach(appRegistration -> {
				HashSet<String> versions = appRegistrationRepository.getIfAvailable().findAllByName(appRegistration.getName()).stream()
						.filter(ar -> ar.getType() == appRegistration.getType())
						.map(AppRegistration::getVersion).collect(Collectors.toCollection(HashSet::new));
				appRegistration.setVersions(versions);
			});
		}
		return new PageImpl<>(resultList, pageable, getTotalCount(cq));
	}

	private Long getTotalCount(CriteriaQuery<AppRegistration> criteriaQuery) {
		return (Long) entityManager.createQuery(((SqmSelectStatement)criteriaQuery).createCountQuery()).getSingleResult();
	}

}
