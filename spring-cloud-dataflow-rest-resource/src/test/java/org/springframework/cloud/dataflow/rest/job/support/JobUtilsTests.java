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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gunnar Hillert
 * @since 1.0
 */
public class JobUtilsTests {

	/**
	 * Test method for
	 * {@link JobUtils#isJobExecutionRestartable(org.springframework.batch.core.JobExecution)}.
	 */
	@Test
	public void testIsJobExecutionRestartable() {
		final JobExecution jobExecution = new JobExecution(1L);
		assertFalse(JobUtils.isJobExecutionRestartable(jobExecution));
	}

	/**
	 * Test method for
	 * {@link JobUtils#isJobExecutionAbandonable(org.springframework.batch.core.JobExecution)}.
	 */
	@Test
	public void testIsJobExecutionAbandonable() {
		final JobExecution jobExecution = new JobExecution(1L);
		assertFalse(JobUtils.isJobExecutionAbandonable(jobExecution));
	}

	/**
	 * Test method for
	 * {@link JobUtils#isJobExecutionStoppable(org.springframework.batch.core.JobExecution)}.
	 */
	@Test
	public void testIsJobExecutionStoppable() {
		final JobExecution jobExecution = new JobExecution(1L);
		assertTrue(JobUtils.isJobExecutionStoppable(jobExecution));
	}

	@Test
	public void testIsJobExecutionRestartableWithNullJobExecution() {
		try {
			JobUtils.isJobExecutionRestartable(null);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertEquals("The provided jobExecution must not be null.", e.getMessage());
		}
	}

	@Test
	public void testIsJobExecutionAbandonableWithNullJobExecution() {
		try {
			JobUtils.isJobExecutionAbandonable(null);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertEquals("The provided jobExecution must not be null.", e.getMessage());
		}
	}

	@Test
	public void testIsJobExecutionStoppableWithNullJobExecution() {
		try {
			JobUtils.isJobExecutionStoppable(null);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertEquals("The provided jobExecution must not be null.", e.getMessage());
		}
	}

	@Test
	public void testIsJobExecutionRestartableWithNullBatchStatus() {
		try {
			final JobExecution jobExecution = new JobExecution(1L);
			jobExecution.setStatus(null);
			JobUtils.isJobExecutionRestartable(jobExecution);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertEquals("The BatchStatus of the provided jobExecution must not be null.", e.getMessage());
		}
	}

	@Test
	public void testIsJobExecutionAbandonableWithNullBatchStatus() {
		try {
			final JobExecution jobExecution = new JobExecution(1L);
			jobExecution.setStatus(null);
			JobUtils.isJobExecutionAbandonable(jobExecution);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertEquals("The BatchStatus of the provided jobExecution must not be null.", e.getMessage());
		}
	}

	@Test
	public void testIsJobExecutionStoppableWithNullBatchStatus() {
		try {
			final JobExecution jobExecution = new JobExecution(1L);
			jobExecution.setStatus(null);
			JobUtils.isJobExecutionStoppable(jobExecution);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertEquals("The BatchStatus of the provided jobExecution must not be null.", e.getMessage());
		}
	}

	@Test
	public void testIsJobExecutionStoppableWithCompletedBatchStatus() {
		final JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setStatus(BatchStatus.COMPLETED);
		assertFalse(JobUtils.isJobExecutionStoppable(jobExecution));
	}

	@Test
	public void testIsJobExecutionRestartableWithCompletedBatchStatus() {
		final JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setStatus(BatchStatus.COMPLETED);
		assertFalse(JobUtils.isJobExecutionRestartable(jobExecution));
	}

	@Test
	public void testIsJobExecutionAbandonableWithCompletedBatchStatus() {
		final JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setStatus(BatchStatus.COMPLETED);
		assertFalse(JobUtils.isJobExecutionAbandonable(jobExecution));
	}

	@Test
	public void testIsJobExecutionStoppableWithFailedBatchStatus() {
		final JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setStatus(BatchStatus.FAILED);
		assertFalse(JobUtils.isJobExecutionStoppable(jobExecution));
	}

	@Test
	public void testIsJobExecutionRestartableWithFailedBatchStatus() {
		final JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setStatus(BatchStatus.FAILED);
		assertTrue(JobUtils.isJobExecutionRestartable(jobExecution));
	}

	@Test
	public void testIsJobExecutionAbandonableWithFailedBatchStatus() {
		final JobExecution jobExecution = new JobExecution(1L);
		jobExecution.setStatus(BatchStatus.FAILED);
		assertTrue(JobUtils.isJobExecutionAbandonable(jobExecution));
	}

}
