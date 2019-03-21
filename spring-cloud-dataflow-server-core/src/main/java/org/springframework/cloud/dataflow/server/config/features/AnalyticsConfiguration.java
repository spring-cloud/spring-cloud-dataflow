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

import org.springframework.analytics.metrics.AggregateCounterRepository;
import org.springframework.analytics.metrics.FieldValueCounterRepository;
import org.springframework.analytics.metrics.redis.RedisAggregateCounterRepository;
import org.springframework.analytics.metrics.redis.RedisFieldValueCounterRepository;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.actuate.metrics.repository.redis.RedisMetricRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.retry.support.RetryTemplate;

/**
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@ConditionalOnProperty(prefix = FeaturesProperties.FEATURES_PREFIX, name = FeaturesProperties.ANALYTICS_ENABLED, matchIfMissing = true)
public class AnalyticsConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public MetricRepository metricRepository(RedisConnectionFactory redisConnectionFactory) {
		return new RedisMetricRepository(redisConnectionFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	public FieldValueCounterRepository fieldValueCounterReader(RedisConnectionFactory redisConnectionFactory) {
		return new RedisFieldValueCounterRepository(redisConnectionFactory, new RetryTemplate());
	}

	@Bean
	@ConditionalOnMissingBean
	public AggregateCounterRepository aggregateCounterReader(RedisConnectionFactory redisConnectionFactory) {
		return new RedisAggregateCounterRepository(redisConnectionFactory, new RetryTemplate());
	}
}
