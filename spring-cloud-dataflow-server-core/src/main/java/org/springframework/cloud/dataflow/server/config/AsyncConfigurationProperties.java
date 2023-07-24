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
