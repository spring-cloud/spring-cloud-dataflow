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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;

/**
 * Used to configure the {@link ThreadPoolTaskExecutor} in {@link DataflowAsyncConfiguration}. For more information
 * of the fields see {@link ThreadPoolTaskExecutor} and {@link ExecutorConfigurationSupport}
 *
 * @author Tobias Soloschenko
 */
@ConfigurationProperties(prefix = AsyncConfigurationProperties.ASYNC_PREFIX)
public class AsyncConfigurationProperties {

	public static final String ASYNC_PREFIX = DataFlowPropertyKeys.PREFIX + "async";

	private int corePoolSize = 1;

	private int maxPoolSize = Integer.MAX_VALUE;

	private int keepAliveSeconds = 60;

	private int queueCapacity = Integer.MAX_VALUE;

	private boolean allowCoreThreadTimeOut = false;

	private boolean prestartAllCoreThreads = false;

	private boolean waitForTasksToCompleteOnShutdown = false;

	private long awaitTerminationMillis = 0L;

	private String threadNamePrefix = "scdf-async-";

	public int getQueueCapacity() {
		return queueCapacity;
	}

	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public int getKeepAliveSeconds() {
		return keepAliveSeconds;
	}

	public void setKeepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = keepAliveSeconds;
	}

	public void setQueueCapacity(int queueCapacity) {
		this.queueCapacity = queueCapacity;
	}

	public int getCorePoolSize() {
		return corePoolSize;
	}

	public boolean isAllowCoreThreadTimeOut() {
		return allowCoreThreadTimeOut;
	}

	public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
		this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
	}

	public boolean isPrestartAllCoreThreads() {
		return prestartAllCoreThreads;
	}

	public void setPrestartAllCoreThreads(boolean prestartAllCoreThreads) {
		this.prestartAllCoreThreads = prestartAllCoreThreads;
	}

	public boolean isWaitForTasksToCompleteOnShutdown() {
		return waitForTasksToCompleteOnShutdown;
	}

	public void setWaitForTasksToCompleteOnShutdown(boolean waitForTasksToCompleteOnShutdown) {
		this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
	}

	public long getAwaitTerminationMillis() {
		return awaitTerminationMillis;
	}

	public void setAwaitTerminationMillis(long awaitTerminationMillis) {
		this.awaitTerminationMillis = awaitTerminationMillis;
	}

	public String getThreadNamePrefix() {
		return threadNamePrefix;
	}

	public void setThreadNamePrefix(String threadNamePrefix) {
		this.threadNamePrefix = threadNamePrefix;
	}
}
