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
package org.springframework.cloud.dataflow.server.config.features;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.RedisHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.RdbmsDeploymentIdRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Configuration class that imports analytics, stream and task configuration classes. Also
 * holds any common beans on the above configuration classes.
 *
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@Import({ AnalyticsConfiguration.class, StreamConfiguration.class, TaskConfiguration.class })
public class FeaturesConfiguration {

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnExpression("#{'${" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.STREAMS_ENABLED
			+ ":true}'.equalsIgnoreCase('true') || " + "'${" + FeaturesProperties.FEATURES_PREFIX + "."
			+ FeaturesProperties.TASKS_ENABLED + ":true}'.equalsIgnoreCase('true') }")
	public DeploymentIdRepository deploymentIdRepository(DataSource dataSource) {
		return new RdbmsDeploymentIdRepository(dataSource);
	}

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
