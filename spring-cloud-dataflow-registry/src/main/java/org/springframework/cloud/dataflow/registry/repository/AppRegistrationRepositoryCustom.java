package org.springframework.cloud.dataflow.registry.repository;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Siddhant Sorann
 */
@Component
public class AppRegistrationRepositoryCustom {

	@Autowired
	private EntityManager entityManager;

	public AppRegistrationRepositoryCustom(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public Page<AppRegistration> findAllByTypeAndNameIsLikeAndVersionAndDefaultVersion(ApplicationType type,
			String name, String version, boolean defaultVersion, Pageable pageable) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<AppRegistration> cq = cb.createQuery(AppRegistration.class);
		Root<AppRegistration> appRegistrationRoot = cq.from(AppRegistration.class);
		final List<Predicate> predicates = new ArrayList<>();
		if (type != null) {
			predicates.add(cb.equal(appRegistrationRoot.get("type"), type));
		}
		if (StringUtils.hasText(name)) {
			predicates.add(cb.like(cb.lower(appRegistrationRoot.get("name")), name.toLowerCase()));
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
		return new PageImpl<>(resultList, pageable, getTotalCount(cb, predicates.toArray(new Predicate[0])));
	}

	private Long getTotalCount(CriteriaBuilder criteriaBuilder, Predicate[] predicateArray) {
		CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
		Root<AppRegistration> root = criteriaQuery.from(AppRegistration.class);

		criteriaQuery.select(criteriaBuilder.count(root));
		criteriaQuery.where(predicateArray);

		return entityManager.createQuery(criteriaQuery).getSingleResult();
	}
}
