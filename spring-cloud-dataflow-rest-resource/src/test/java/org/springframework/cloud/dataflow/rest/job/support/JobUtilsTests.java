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

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 * @since 1.0
 */
class JobUtilsTests {

	/**
	 * Test method for
	 * {@link JobUtils#isJobExecutionRestartable(org.springframework.batch.core.JobExecution)}.
	 */
	@Test
	void testIsJobExecutionRestartable() {
		final JobExecution jobExecution = new JobExecution(1L);
		assertThat(JobUtils.isJobExecutionRestartable(jobExecution)).isFalse();
	}

	/**
	 * Test method for
	 * {@link JobUtils#isJobExecutionAbandonable(org.springframework.batch.core.JobExecution)}.
	 */
	@Test
	void testIsJobExecutionAbandonable() {
		final JobExecution jobExecution = new JobExecution(1L);
		assertThat(JobUtils.isJobExecutionAbandonable(jobExecution)).isFalse();
	}

	/**
	 * Test method for
	 * {@link JobUtils#isJobExecutionStoppable(org.springframework.batch.core.JobExecution)}.
	 */
	@Test
	void testIsJobExecutionStoppable() {
		final JobExecution jobExecution = new JobExecution(1L);
		assertThat(JobUtils.isJobExecutionStoppable(jobExecution)).isTrue();
	}

	@Test
	void isJobExecutionRestartableWithNullJobExecution() {
		try {
			JobUtils.isJobExecutionRestartable(null);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("The provided jobExecution must not be null.");
		}
	}

	@Test
	void isJobExecutionAbandonableWithNullJobExecution() {
		try {
			JobUtils.isJobExecutionAbandonable(null);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("The provided jobExecution must not be null.");
		}
	}

	@Test
	void isJobExecutionStoppableWithNullJobExecution() {
		try {
			JobUtils.isJobExecutionStoppable(null);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("The provided jobExecution must not be null.");
		}
	}

	@Test
	void isJobExecutionRestartableWithNullBatchStatus() {
		try {
			final JobExecution jobExecution = new JobExecution(1L);
			jobExecution.setStatus(null);
			JobUtils.isJobExecutionRestartable(jobExecution);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("The BatchStatus of the provided jobExecution must not be null.");
		}
	}

	@Test
	void isJobExecutionAbandonableWithNullBatchStatus() {
		try {
			final JobExecution jobExecution = new JobExecution(1L);
			jobExecution.setStatus(null);
			JobUtils.isJobExecutionAbandonable(jobExecution);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("The BatchStatus of the provided jobExecution must not be null.");
		}
	}

	@Test
	void isJobExecutionStoppableWithNullBatchStatus() {
		try {
			final JobExecution jobExecution = new JobExecution(1L);
			jobExecution.setStatus(null);
			JobUtils.isJobExecutionStoppable(jobExecution);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("The BatchStatus of the provided jobExecution must not be null.");
		}
	}

	@Test
	void isJobExecutionStoppableWithCompletedBatchStatus() {
		final JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setStatus(BatchStatus.COMPLETED);
		assertThat(JobUtils.isJobExecutionStoppable(jobExecution)).isFalse();
	}

	@Test
	void isJobExecutionRestartableWithCompletedBatchStatus() {
		final JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setStatus(BatchStatus.COMPLETED);
		assertThat(JobUtils.isJobExecutionRestartable(jobExecution)).isFalse();
	}

	@Test
	void isJobExecutionAbandonableWithCompletedBatchStatus() {
		final JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setStatus(BatchStatus.COMPLETED);
		assertThat(JobUtils.isJobExecutionAbandonable(jobExecution)).isFalse();
	}

	@Test
	void isJobExecutionStoppableWithFailedBatchStatus() {
		final JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setStatus(BatchStatus.FAILED);
		assertThat(JobUtils.isJobExecutionStoppable(jobExecution)).isFalse();
	}

	@Test
	void isJobExecutionRestartableWithFailedBatchStatus() {
		final JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setStatus(BatchStatus.FAILED);
		assertThat(JobUtils.isJobExecutionRestartable(jobExecution)).isTrue();
	}

	@Test
	void isJobExecutionAbandonableWithFailedBatchStatus() {
		final JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setStatus(BatchStatus.FAILED);
		assertThat(JobUtils.isJobExecutionAbandonable(jobExecution)).isTrue();
	}

}
