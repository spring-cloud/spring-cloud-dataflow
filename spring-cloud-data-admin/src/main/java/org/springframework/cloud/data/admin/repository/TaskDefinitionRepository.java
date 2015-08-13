package org.springframework.cloud.data.admin.repository;

import org.springframework.cloud.data.core.TaskDefinition;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author Michael Minella
 */
public interface TaskDefinitionRepository extends PagingAndSortingRepository<TaskDefinition, String> {
}
