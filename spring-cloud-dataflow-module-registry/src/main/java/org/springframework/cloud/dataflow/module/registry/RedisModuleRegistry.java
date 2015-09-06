/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.module.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.core.ModuleCoordinates;
import org.springframework.cloud.dataflow.core.ModuleType;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * {@link ModuleRegistry} implementation backed by Redis.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class RedisModuleRegistry implements ModuleRegistry {

	/**
	 * Prefix for keys used for storing module coordinates.
	 */
	public static final String KEY_PREFIX = "spring.cloud.module.";

	/**
	 * Redis operations template.
	 */
	private final RedisOperations<String, String> redisOperations;


	/**
	 * Construct a {@code RedisModuleRegistry} with the provided
	 * {@link RedisConnectionFactory}.
	 *
	 * @param redisConnectionFactory connection factory for Redis
	 */
	public RedisModuleRegistry(RedisConnectionFactory redisConnectionFactory) {
		redisOperations = new StringRedisTemplate(redisConnectionFactory);
	}

	@Override
	public ModuleRegistration find(String name, ModuleType type) {
		String coordinates = redisOperations.<String, String>boundHashOps(KEY_PREFIX + type).get(name);
		return (coordinates == null ? null :
				new ModuleRegistration(name, type, ModuleCoordinates.parse(coordinates)));
	}

	@Override
	public List<ModuleRegistration> findAll() {
		List<ModuleRegistration> list = new ArrayList<>();

		for (ModuleType type : ModuleType.values()) {
			for (Map.Entry<String, String> entry :
					redisOperations.<String, String>boundHashOps(KEY_PREFIX + type)
							.entries().entrySet()) {
				list.add(new ModuleRegistration(entry.getKey(), type,
						ModuleCoordinates.parse(entry.getValue())));
			}
		}

		return list;
	}

	@Override
	public void save(ModuleRegistration registration) {
		redisOperations.boundHashOps(KEY_PREFIX + registration.getType())
				.put(registration.getName(), registration.getCoordinates().toString());
	}

	@Override
	public void delete(String name, ModuleType type) {
		redisOperations.boundHashOps(KEY_PREFIX + type).delete(name);
	}

}
