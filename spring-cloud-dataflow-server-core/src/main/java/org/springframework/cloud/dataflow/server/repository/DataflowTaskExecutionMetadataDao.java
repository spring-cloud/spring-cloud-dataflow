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
package org.springframework.cloud.dataflow.server.repository;

import java.util.Map;
import java.util.Set;

import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.task.repository.TaskExecution;

/**
 * Data access object used for manipulating task manifests
 *
 * @author Michael Minella
 * @author Corneil du Plessis
 * @since 2.3
 */
public interface DataflowTaskExecutionMetadataDao {


	/**
	 * Saves a {@code TaskManifest} related to the supplied {@code TaskExecution}
	 *
	 * @param taskExecution execution this manifest is associated with
	 * @param manifest manifest
	 */
	void save(TaskExecution taskExecution, TaskManifest manifest);

	/**
	 * Returns the manifest for the most recently launched instance of the task name requested.
	 *
	 * @param taskName name of task defintion
	 * @return {@code TaskManifest}
	 */
	TaskManifest getLatestManifest(String taskName);

	/**
	 * Returns the manifest for the given execution id.
	 * @param id execution id
	 * @return {@code TaskManifest}
	 */
	TaskManifest findManifestById(Long id);

	/**
	 * Returns a collection of manifests mapped by id for the supplied ids.
	 * @param ids list of task execution ids.
	 * @return map of manifests with id as key.
	 */
	Map<Long, TaskManifest> findManifestByIds(Set<Long> ids);

	/**
	 * Deletes the task manifest records associated with the collection of task execution ids provided.
	 *
	 * @param taskExecutionIds collection of ids to delete the manifests for
	 * @return number of manifests deleted
	 */
	int deleteManifestsByTaskExecutionIds(Set<Long> taskExecutionIds);
}
