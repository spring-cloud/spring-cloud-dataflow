/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config;

import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Class to override the executor at the application level. It also enables async executions for the Spring Cloud Data Flow Server.
 *
 * @author Tobias Soloschenko
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableConfigurationProperties(AsyncConfigurationProperties.class)
public class DataflowAsyncConfiguration implements AsyncConfigurer {

	private final AsyncConfigurationProperties asyncConfigurationProperties;

	public DataflowAsyncConfiguration(AsyncConfigurationProperties asyncConfigurationProperties) {
		this.asyncConfigurationProperties = asyncConfigurationProperties;
	}

	@Override
	public Executor getAsyncExecutor() {
		ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
		threadPoolTaskExecutor.setQueueCapacity(asyncConfigurationProperties.getQueueCapacity());
		threadPoolTaskExecutor.setCorePoolSize(asyncConfigurationProperties.getCorePoolSize());
		threadPoolTaskExecutor.setMaxPoolSize(asyncConfigurationProperties.getMaxPoolSize());
		threadPoolTaskExecutor.setKeepAliveSeconds(asyncConfigurationProperties.getKeepAliveSeconds());
		threadPoolTaskExecutor.setAllowCoreThreadTimeOut(asyncConfigurationProperties.isAllowCoreThreadTimeOut());
		threadPoolTaskExecutor.setPrestartAllCoreThreads(asyncConfigurationProperties.isPrestartAllCoreThreads());
		threadPoolTaskExecutor.setAwaitTerminationMillis(asyncConfigurationProperties.getAwaitTerminationMillis());
		threadPoolTaskExecutor.setThreadNamePrefix(asyncConfigurationProperties.getThreadNamePrefix());
		threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(asyncConfigurationProperties.isWaitForTasksToCompleteOnShutdown());
		threadPoolTaskExecutor.initialize();
		return threadPoolTaskExecutor;
	}
}
