package org.springframework.cloud.dataflow.registry.repository;

import java.util.List;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author Christian Tzolov
 */
public interface AppRegistrationRepository extends PagingAndSortingRepository<AppRegistration, Long> {

	AppRegistration findAppRegistrationByNameAndTypeAndVersion(String name, ApplicationType type, String version);

	AppRegistration findAppRegistrationByNameAndTypeAndDefaultVersionIsTrue(String name, ApplicationType type);

	void deleteAppRegistrationByNameAndTypeAndVersion(String name, ApplicationType type, String version);

	Page<AppRegistration> findAllByTypeAndNameIsLike(ApplicationType type, String name, Pageable pageable);

	Page<AppRegistration> findAllByType(ApplicationType type, Pageable pageable);

	@Override
	<S extends AppRegistration> S save(S s);

	@Override
	List<AppRegistration> findAll();
}
