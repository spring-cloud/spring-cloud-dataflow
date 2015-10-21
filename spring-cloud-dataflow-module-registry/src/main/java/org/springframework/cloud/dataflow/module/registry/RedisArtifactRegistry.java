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

import org.springframework.cloud.dataflow.core.ArtifactCoordinates;
import org.springframework.cloud.dataflow.core.ArtifactType;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * {@link ArtifactRegistry} implementation backed by Redis.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class RedisArtifactRegistry implements ArtifactRegistry {

	/**
	 * Prefix for keys used for storing artifact coordinates.
	 */
	public static final String KEY_PREFIX = "spring.cloud.artifact.";

	/**
	 * Redis operations template.
	 */
	private final RedisOperations<String, String> redisOperations;


	/**
	 * Construct a {@code RedisArtifactRegistry} with the provided
	 * {@link RedisConnectionFactory}.
	 *
	 * @param redisConnectionFactory connection factory for Redis
	 */
	public RedisArtifactRegistry(RedisConnectionFactory redisConnectionFactory) {
		redisOperations = new StringRedisTemplate(redisConnectionFactory);
	}

	@Override
	public ArtifactRegistration find(String name, ArtifactType type) {
		String coordinates = redisOperations.<String, String>boundHashOps(KEY_PREFIX + type).get(name);
		return (coordinates == null ? null :
				new ArtifactRegistration(name, type, ArtifactCoordinates.parse(coordinates)));
	}

	@Override
	public List<ArtifactRegistration> findAll() {
		List<ArtifactRegistration> list = new ArrayList<>();

		for (ArtifactType type : ArtifactType.values()) {
			for (Map.Entry<String, String> entry :
					redisOperations.<String, String>boundHashOps(KEY_PREFIX + type)
							.entries().entrySet()) {
				list.add(new ArtifactRegistration(entry.getKey(), type,
						ArtifactCoordinates.parse(entry.getValue())));
			}
		}

		return list;
	}

	@Override
	public void save(ArtifactRegistration registration) {
		redisOperations.boundHashOps(KEY_PREFIX + registration.getType())
				.put(registration.getName(), registration.getCoordinates().toString());
	}

	@Override
	public void delete(String name, ArtifactType type) {
		redisOperations.boundHashOps(KEY_PREFIX + type).delete(name);
	}

}
