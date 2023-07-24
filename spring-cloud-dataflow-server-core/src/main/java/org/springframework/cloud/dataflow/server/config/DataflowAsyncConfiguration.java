package org.springframework.cloud.dataflow.server.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

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
