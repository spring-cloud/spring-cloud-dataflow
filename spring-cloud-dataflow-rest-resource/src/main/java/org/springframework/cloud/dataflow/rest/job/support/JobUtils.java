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

package org.springframework.cloud.dataflow.rest.job.support;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.util.Assert;

/**
 * Provides a common utility methods for job-related tasks.
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
public final class JobUtils {

	/**
	 * Prevent instantiation.
	 */
	private JobUtils() {
		throw new AssertionError();
	}

	/**
	 * Determine whether the provided {@link JobExecution} is restartable.
	 *
	 * @param jobExecution Must not be null and its {@link BatchStatus} must not be null
	 * either.
	 * @return Never returns null
	 */
	public static boolean isJobExecutionRestartable(JobExecution jobExecution) {
		Assert.notNull(jobExecution, "The provided jobExecution must not be null.");

		final BatchStatus batchStatus = jobExecution.getStatus();
		Assert.notNull(batchStatus, "The BatchStatus of the provided jobExecution must not be null.");

		return batchStatus.isGreaterThan(BatchStatus.STOPPING) && batchStatus.isLessThan(BatchStatus.ABANDONED);
	}

	/**
	 * Determine whether the provided {@link JobExecution} is abandonable.
	 *
	 * @param jobExecution Must not be null and its {@link BatchStatus} must not be null
	 * either.
	 * @return Never returns null
	 */
	public static boolean isJobExecutionAbandonable(JobExecution jobExecution) {
		Assert.notNull(jobExecution, "The provided jobExecution must not be null.");

		final BatchStatus batchStatus = jobExecution.getStatus();
		Assert.notNull(batchStatus, "The BatchStatus of the provided jobExecution must not be null.");

		return batchStatus.isGreaterThan(BatchStatus.STARTED) && batchStatus != BatchStatus.ABANDONED;
	}

	/**
	 * Determine whether the provided {@link JobExecution} is stoppable.
	 *
	 * @param jobExecution Must not be null and its {@link BatchStatus} must not be null
	 * either.
	 * @return Never returns null
	 */
	public static boolean isJobExecutionStoppable(JobExecution jobExecution) {
		Assert.notNull(jobExecution, "The provided jobExecution must not be null.");

		final BatchStatus batchStatus = jobExecution.getStatus();
		Assert.notNull(batchStatus, "The BatchStatus of the provided jobExecution must not be null.");

		return batchStatus.isLessThan(BatchStatus.STOPPING) && batchStatus != BatchStatus.COMPLETED;
	}
}
