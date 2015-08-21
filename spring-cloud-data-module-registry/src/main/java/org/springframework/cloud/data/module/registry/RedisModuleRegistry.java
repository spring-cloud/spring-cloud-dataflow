/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.module.registry;

import org.springframework.cloud.data.core.ModuleCoordinates;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * {@link ModuleRegistry} implementation backed by Redis.
 *
 * @author Patrick Peralta
 */
public class RedisModuleRegistry implements ModuleRegistry {

	/**
	 * Prefix for keys used for storing module coordinates.
	 */
	public static final String KEY_PREFIX = "spring.cloud.module";

	/**
	 * Group ID for default modules.
	 */
	private static final String GROUP_ID = "org.springframework.cloud.stream.module";

	/**
	 * Version number for default modules.
	 */
	private static final String VERSION = "1.0.0.BUILD-SNAPSHOT";

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
		populateDefaults();
	}

	/**
	 * Populate the registry with default module coordinates; will
	 * not overwrite existing values.
	 */
	private void populateDefaults() {
		populateDefault("http", "source");
		populateDefault("time", "source");
		populateDefault("filter", "processor");
		populateDefault("groovy-filter", "processor");
		populateDefault("groovy-transform", "processor");
		populateDefault("transform", "processor");
		populateDefault("counter", "sink");
		populateDefault("log", "sink");
	}

	/**
	 * Populate the registry with default values for the provided
	 * module name and type; will not overwrite existing values.
	 *
	 * @param name module name
	 * @param type module type
	 */
	private void populateDefault(String name, String type) {
		redisOperations.boundValueOps(keyFor(name, type))
				.setIfAbsent(defaultCoordinatesFor(name + '-' + type).toString());
	}

	/**
	 * Return the default coordinates for the provided module name.
	 *
	 * @param moduleName module name for which to provide default coordinates
	 * @return default coordinates for the provided module
	 */
	private ModuleCoordinates defaultCoordinatesFor(String moduleName) {
		return ModuleCoordinates.parse(String.format("%s:%s:%s", GROUP_ID, moduleName, VERSION));
	}

	@Override
	public ModuleCoordinates findByNameAndType(String name, String type) {
		String coordinates = redisOperations.boundValueOps(keyFor(name, type)).get();
		return (coordinates == null ? null : ModuleCoordinates.parse(coordinates));
	}

	/**
	 * Return the key for the given module name and type.
	 *
	 * @param name module name
	 * @param type module type
	 * @return key for the given module name and type
	 */
	private String keyFor(String name, String type) {
		return String.format("%s:%s:%s", KEY_PREFIX, type, name);
	}

}
