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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.springframework.cloud.dataflow.server.config.DataflowAsyncConfiguration.ASYNC_PREFIX;

/**
 * Class to override the executor at the application level. It also enables async executions for the Spring Cloud Data Flow Server.
 *
 * @author Tobias Soloschenko
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = ASYNC_PREFIX, name = "enabled")
@EnableAsync
class DataflowAsyncConfiguration implements AsyncConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(DataflowAsyncConfiguration.class);

	public static final String ASYNC_PREFIX = DataFlowPropertyKeys.PREFIX + "task.cleanup.async";

	private static final String THREAD_NAME_PREFIX = "scdf-async-";

	@Bean(name = "asyncExecutor")
	Executor getAsyncExecutor(TaskExecutorBuilder taskExecutorBuilder) {
		return taskExecutorBuilder.threadNamePrefix(THREAD_NAME_PREFIX).build();
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (throwable, method, objects) -> logger.error("Exception thrown in @Async Method " + method.getName(),
				throwable);
	}
}
