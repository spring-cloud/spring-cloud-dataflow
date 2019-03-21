/*
 * Copyright 2017 the original author or authors.
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

import org.springframework.cloud.dataflow.core.StreamDeployment;

/**
 * Repository for storing deployment information for Stream.
 *
 * @author Ilayaperumal Gopinathan
 */
public interface StreamDeploymentRepository {

	/**
	 * Store Stream deployment entry.
	 *
	 * @param streamDeployment the stream deployment to save
	 * @return the streamDeployment that is saved
	 */
	StreamDeployment save(StreamDeployment streamDeployment);

	/**
	 * Find a specific StreamDeployment by stream name.
	 * @param streamName the stream name corresponding to the stream deployment
	 * @return the stream deployment
	 */
	StreamDeployment findOne(String streamName);

	/**
	 * Get all Stream deployment entries
	 *
	 * @return the iterable StreamDeployment entries
	 */
	Iterable<StreamDeployment> findAll();

	/**
	 * Delete a Stream deployment identified by the given stream name.
	 *
	 * @param streamName the stream name corresponding to the stream deployment
	 */
	void delete(String streamName);

}
