/*
 * Copyright 2016 the original author or authors.
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

import org.springframework.util.Assert;

/**
 * In-memory implementation of a {@link DeploymentIdRepository}.
 *
 * @author Janne Valkealahti
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class InMemoryDeploymentIdRepository implements DeploymentIdRepository {

	private final Map<String, String> deployments = new ConcurrentHashMap<String, String>();

	@Override
	public void save(String key, String id) {
		// we don't care if key or id already exists as
		// repository is used to track last deployment
		Assert.notNull(key, "key must not be null");
		Assert.notNull(id, "id must not be null");
		deployments.put(key, id);
	}

	@Override
	public String findOne(String key) {
		return deployments.get(key);
	}

	@Override
	public void delete(String key) {
		deployments.remove(key);
	}
}
