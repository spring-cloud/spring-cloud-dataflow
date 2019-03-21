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

package org.springframework.cloud.dataflow.registry;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;

/**
 * {@link UriRegistry} implementation backed by Redis.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 */
@Deprecated
public class RedisUriRegistry implements UriRegistry {

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
	public RedisUriRegistry(RedisConnectionFactory redisConnectionFactory) {
		redisOperations = new StringRedisTemplate(redisConnectionFactory);
	}

	@Override
	public URI find(String key) {
		String uri = hashOps().get(key);
		Assert.notNull(uri, String.format("No URI registered for %s", key));
		return toUri(uri);
	}

	@Override
	public Map<String, URI> findAll() {
		Map<String, URI> map = new HashMap<>();
		for (Map.Entry<String, String> entry : hashOps().entries().entrySet()) {
			map.put(entry.getKey(), toUri(entry.getValue()));
		}
		return map;
	}

	@Override
	public void register(String key, URI uri) {
		hashOps().put(key, uri.toString());
	}

	@Override
	public void unregister(String key) {
		hashOps().delete(key);
	}

	/**
	 * Convert the provided string to a {@link URI}.
	 *
	 * @param s string to convert to URI
	 * @return URI for string
	 * @throws IllegalStateException if URI creation throws {@link URISyntaxException}
	 */
	private URI toUri(String s) {
		try {
			return new URI(s);
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Return the {@link BoundHashOperations} operation for Redis.
	 *
	 * @return {@code BoundHashOperations} for Redis
	 */
	private BoundHashOperations<String, String, String> hashOps() {
		return this.redisOperations.boundHashOps("spring.cloud.resource.uri");
	}

}
