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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.springframework.cloud.dataflow.server.config.DataflowAsyncAutoConfiguration.ASYNC_PROPS_PREFIX;

/**
 * Enables async executions for the Spring Cloud Dataflow server.
 * Uses the Spring Boot autoconfigured {@code TaskExecutorBuilder} to create an async executor and register it
 * with name {@link #DATAFLOW_ASYNC_EXECUTOR}.
 *
 * @author Tobias Soloschenko
 */
@AutoConfiguration
@ConditionalOnBean({EnableDataFlowServerConfiguration.Marker.class})
@ConditionalOnProperty(prefix = ASYNC_PROPS_PREFIX, name = "enabled", havingValue = "true")
@AutoConfigureAfter(TaskExecutionAutoConfiguration.class)
@EnableAsync
public class DataflowAsyncAutoConfiguration implements AsyncConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(DataflowAsyncAutoConfiguration.class);

	public static final String ASYNC_PROPS_PREFIX = DataFlowPropertyKeys.PREFIX + "async";

	public static final String DATAFLOW_ASYNC_EXECUTOR = "dataflowAsyncExecutor";

	private static final String THREAD_NAME_PREFIX = "scdf-async-";

	private final ThreadPoolTaskExecutorBuilder taskExecutorBuilder;

	public DataflowAsyncAutoConfiguration(ThreadPoolTaskExecutorBuilder taskExecutorBuilder) {
		this.taskExecutorBuilder = taskExecutorBuilder;
	}

	@Bean(name = DATAFLOW_ASYNC_EXECUTOR)
	@Override
	public Executor getAsyncExecutor() {
		return this.taskExecutorBuilder.threadNamePrefix(THREAD_NAME_PREFIX).build();
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (throwable, method, objects) -> logger.error("Exception thrown in @Async Method " + method.getName(),
				throwable);
	}
}
