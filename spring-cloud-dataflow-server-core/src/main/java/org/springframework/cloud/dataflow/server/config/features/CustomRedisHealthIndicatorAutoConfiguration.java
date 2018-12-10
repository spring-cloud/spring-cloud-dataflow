/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config.features;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.redis.RedisHealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.redis.RedisReactiveHealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.redis.RedisHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnBean(RedisConnectionFactory.class)
@ConditionalOnEnabledHealthIndicator("redis")
@AutoConfigureBefore({ RedisHealthIndicatorAutoConfiguration.class,
		RedisReactiveHealthIndicatorAutoConfiguration.class })
public class CustomRedisHealthIndicatorAutoConfiguration {

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;

	@Bean
	@ConditionalOnExpression("#{'${" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.ANALYTICS_ENABLED
			+ ":true}'.equalsIgnoreCase('false')}")
	public RedisHealthIndicator redisHealthIndicator() {
		return new CustomRedisHealthIndicator(redisConnectionFactory);
	}

	private class CustomRedisHealthIndicator extends RedisHealthIndicator {

		public CustomRedisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
			super(redisConnectionFactory);
		}

		@Override
		protected void doHealthCheck(Health.Builder builder) throws Exception {
			// do nothing - status UNKNOWN
		}
	}
}
