/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskExecutor;


/**
 * CTR requires the use of the {@link java.util.concurrent.ThreadPoolExecutor}.
 * As of Batch 5.x DefaultBatchConfiguration is now used to override default beans, however this disables
 * BatchAutoConfiguration.  To work around this CTR creates its own {@link JobLauncher} that uses this {@link TaskExecutor}.
 *
 * @author Glenn Renfro
 */
public class JobLauncherBeanPostProcessor implements BeanPostProcessor, Ordered {
	private static final Logger logger = LoggerFactory.getLogger(JobLauncherBeanPostProcessor.class);

	private ApplicationContext context;

	public JobLauncherBeanPostProcessor(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (beanName.equals("jobLauncher")) {
			logger.debug("Replacing BatchAutoConfiguration's jobLauncher Bean with one provided by composed task runner.");
			bean = jobLauncher(context.getBean("jobRepository", JobRepository.class),
				context.getBean("taskExecutor", TaskExecutor.class));
		}
		return bean;
	}

	private JobLauncher jobLauncher(JobRepository jobRepository, TaskExecutor taskExecutor) {
		TaskExecutorJobLauncher taskExecutorJobLauncher = new TaskExecutorJobLauncher();
		taskExecutorJobLauncher.setJobRepository(jobRepository);
		taskExecutorJobLauncher.setTaskExecutor(taskExecutor);
		try {
			taskExecutorJobLauncher.afterPropertiesSet();
			return taskExecutorJobLauncher;
		} catch (Exception e) {
			throw new BatchConfigurationException("Unable to configure the default job launcher", e);
		}
	}
}
