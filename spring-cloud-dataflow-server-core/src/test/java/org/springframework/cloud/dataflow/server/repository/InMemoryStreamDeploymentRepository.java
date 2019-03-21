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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.server.controller.StreamAlreadyDeployedException;

/**
 * In-memory implementation of {@link StreamDeploymentRepository}.
 *
 * @author Ilayaperumal Gopinathan
 */
public class InMemoryStreamDeploymentRepository implements StreamDeploymentRepository {

	private final Map<String, StreamDeployment> streamDeployments = new ConcurrentHashMap<>();

	@Override
	public StreamDeployment save(StreamDeployment streamDeployment) {
		if (this.streamDeployments.containsKey(streamDeployment.getStreamName())) {
			throw new StreamAlreadyDeployedException(streamDeployment.getStreamName());
		}
		this.streamDeployments.put(streamDeployment.getStreamName(), streamDeployment);
		return streamDeployment;
	}

	@Override
	public StreamDeployment findOne(String name) {
		return this.streamDeployments.get(name);
	}

	@Override
	public Iterable<StreamDeployment> findAll() {
		return this.streamDeployments.values();
	}

	@Override
	public void delete(String name) {
		this.streamDeployments.remove(name);
	}

}
