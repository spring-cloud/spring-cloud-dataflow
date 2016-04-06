/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * Redis implementation of an {@link AppDeploymentRepository}.
 *
 * @author Janne Valkealahti
 */
public class RedisAppDeploymentRepository implements AppDeploymentRepository {

	private final BoundHashOperations<String, String, String> hashOperations;

	/**
	 * Instantiates a new redis app deployment repository.
	 *
	 * @param hashKey the hash key
	 * @param redisConnectionFactory the redis connection factory
	 */
	public RedisAppDeploymentRepository(String hashKey, RedisConnectionFactory redisConnectionFactory) {
		StringRedisTemplate redisTemplate = new StringRedisTemplate(redisConnectionFactory);
		hashOperations = redisTemplate.boundHashOps(hashKey);
	}

	@Override
	public AppDeploymentKey save(AppDeploymentKey key, String id) {
		// as identifer from key is a unique composition we can
		// use it as a key to backmap id
		hashOperations.put(key.getId(), id);
		return key;
	}

	@Override
	public String findOne(AppDeploymentKey key) {
		return hashOperations.get(key.getId());
	}
}
