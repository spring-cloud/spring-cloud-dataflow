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

package org.springframework.cloud.dataflow.test;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

/**
 * JUnit {@link org.junit.Rule} that detects the fact that a Redis server is running on localhost.
 *
 * @author Gary Russell
 * @author Eric Bottard
 */
public class RedisTestSupport extends AbstractExternalResourceTestSupport<JedisConnectionFactory> {

	public RedisTestSupport() {
		super("REDIS");
	}

	@Override
	protected void obtainResource() throws Exception {
		resource = new JedisConnectionFactory();
		resource.afterPropertiesSet();
		resource.getConnection().close();
	}

	@Override
	protected void cleanupResource() throws Exception {
		resource.destroy();
	}
}
