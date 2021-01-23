package org.springframework.cloud.dataflow.registry.repository;

import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomAppRegistrationRepository {
  public Page<AppRegistration> findAllByTypeAndNameIsLikeAndVersionAndDefaultVersion(
      ApplicationType type,
      String name, String version, boolean defaultVersion, Pageable pageable);
}
