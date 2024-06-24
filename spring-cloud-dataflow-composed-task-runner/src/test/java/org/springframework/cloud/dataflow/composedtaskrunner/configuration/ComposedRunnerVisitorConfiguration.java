/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner.configuration;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.composedtaskrunner.ComposedRunnerJobFactory;
import org.springframework.cloud.dataflow.composedtaskrunner.ComposedRunnerVisitor;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@EnableConfigurationProperties(ComposedTaskProperties.class)
public class ComposedRunnerVisitorConfiguration {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private ComposedTaskProperties composedTaskProperties;

	@Bean
	public ComposedRunnerJobFactory job() {
		return new ComposedRunnerJobFactory(this.composedTaskProperties);
	}

	@Bean
	public ComposedRunnerVisitor composedRunnerStack() {
		return new ComposedRunnerVisitor();
	}

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(this.composedTaskProperties.getSplitThreadCorePoolSize());
		taskExecutor.setMaxPoolSize(this.composedTaskProperties.getSplitThreadMaxPoolSize());
		taskExecutor.setKeepAliveSeconds(this.composedTaskProperties.getSplitThreadKeepAliveSeconds());
		taskExecutor.setAllowCoreThreadTimeOut(
				this.composedTaskProperties.isSplitThreadAllowCoreThreadTimeout());
		taskExecutor.setQueueCapacity(this.composedTaskProperties.getSplitThreadQueueCapacity());
		taskExecutor.setWaitForTasksToCompleteOnShutdown(
				this.composedTaskProperties.isSplitThreadWaitForTasksToCompleteOnShutdown());
		return taskExecutor;
	}

	@Bean
	public Step AAA_0() {
		return createTaskletStep("AAA_0");
	}

	@Bean
	public Step AAA_1() {
		return createTaskletStep("AAA_1");
	}

	@Bean
	public Step AAA_2() {
		return createTaskletStep("AAA_2");
	}

	@Bean
	public Step BBB_0() {
		return createTaskletStep("BBB_0");
	}

	@Bean
	public Step BBB_1() {
		return createTaskletStep("BBB_1");
	}

	@Bean
	public Step CCC_0() {
		return createTaskletStep("CCC_0");
	}

	@Bean
	public Step DDD_0() {
		return createTaskletStep("DDD_0");
	}

	@Bean
	public Step EEE_0() {
		return createTaskletStep("EEE_0");
	}

	@Bean
	public Step FFF_0() {
		return createTaskletStep("FFF_0");
	}

	@Bean
	public Step LABELA() {
		return createTaskletStep("LABELA");
	}


	@Bean
	public Step failedStep_0() {
		return createTaskletStepWithListener("failedStep_0",
				failedStepExecutionListener());
	}

	@Bean
	public Step successStep() {
		return createTaskletStepWithListener("successStep",
				successStepExecutionListener());
	}

	@Bean
	public StepExecutionListener failedStepExecutionListener() {
		return new StepExecutionListener() {
			@Override
			public void beforeStep(StepExecution stepExecution) {

			}

			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				return ExitStatus.FAILED;
			}
		};
	}

	@Bean
	public StepExecutionListener successStepExecutionListener() {
		return new StepExecutionListener() {
			@Override
			public void beforeStep(StepExecution stepExecution) {

			}

			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				return ExitStatus.COMPLETED;
			}
		};
	}

	private Step createTaskletStepWithListener(final String taskName,
			StepExecutionListener stepExecutionListener) {
		StepBuilder stepBuilder = new StepBuilder(taskName, jobRepository);
		return stepBuilder
				.tasklet(new Tasklet() {
					@Override
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
						return RepeatStatus.FINISHED;
					}
				}, this.transactionManager)
				.transactionAttribute(getTransactionAttribute())
				.listener(stepExecutionListener)
				.build();
	}

	private Step createTaskletStep(final String taskName) {
		StepBuilder stepBuilder = new StepBuilder(taskName, jobRepository);
		return stepBuilder
				.tasklet(new Tasklet() {
					@Override
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
						return RepeatStatus.FINISHED;
					}
				}, transactionManager)
				.transactionAttribute(getTransactionAttribute())
				.build();
	}
	/**
	 * Using the default transaction attribute for the job will cause the
	 * TaskLauncher not to see the latest state in the database but rather
	 * what is in its transaction.  By setting isolation to READ_COMMITTED
	 * The task launcher can see latest state of the db.  Since the changes
	 * to the task execution are done by the tasks.

	 * @return DefaultTransactionAttribute with isolation set to READ_COMMITTED.
	 */
	private TransactionAttribute getTransactionAttribute() {
		DefaultTransactionAttribute defaultTransactionAttribute =
				new DefaultTransactionAttribute();
		defaultTransactionAttribute.setIsolationLevel(
				Isolation.READ_COMMITTED.value());
		return defaultTransactionAttribute;
	}
}
