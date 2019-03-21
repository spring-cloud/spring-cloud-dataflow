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

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis implementation of a {@link DeploymentIdRepository}.
 *
 * @author Janne Valkealahti
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class RedisDeploymentIdRepository implements DeploymentIdRepository {

	private final BoundHashOperations<String, String, String> hashOperations;

	/**
	 * Instantiates a new redis deployment ID repository.
	 *
	 * @param hashKey the hash key
	 * @param redisConnectionFactory the redis connection factory
	 */
	public RedisDeploymentIdRepository(String hashKey, RedisConnectionFactory redisConnectionFactory) {
		StringRedisTemplate redisTemplate = new StringRedisTemplate(redisConnectionFactory);
		hashOperations = redisTemplate.boundHashOps(hashKey);
	}

	@Override
	public void save(String key, String id) {
		hashOperations.put(key, id);
	}

	@Override
	public String findOne(String key) {
		return hashOperations.get(key);
	}

	@Override
	public void delete(String key) {
		hashOperations.delete(key);
	}
}
