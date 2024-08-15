/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.common.test.docker.compose.execution;

import org.joda.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.dataflow.common.test.docker.compose.utils.MockitoMultiAnswer;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RetryerTests {
	@Mock private Retryer.RetryableDockerOperation<String> operation;
	private final Retryer retryer = new Retryer(1, Duration.millis(0));

	@Test
	public void not_retry_if_the_operation_was_successful_and_return_result() throws Exception {
		when(operation.call()).thenReturn("hi");

		assertThat(retryer.runWithRetries(operation)).isEqualTo("hi");
		verify(operation).call();
	}

	@Test
	public void should_not_pause_after_last_failure() throws Exception {
		Retryer failFast = new Retryer(0, Duration.standardSeconds(1));
		when(operation.call()).thenThrow(new DockerExecutionException());
		StopWatch stopwatch = new StopWatch();
		stopwatch.start();
		try {
			failFast.runWithRetries(operation);
		} catch (DockerExecutionException e) {
			// expected
		}
		stopwatch.stop();
		assertThat(stopwatch.getTotalTimeMillis()).isLessThan(1000L);
	}

	@Test
	public void retryer_should_wait_after_failure_before_trying_again() throws Exception {
		Retryer timeRetryer = new Retryer(1, Duration.millis(100));

		StopWatch stopwatch = new StopWatch();
		stopwatch.start();
		when(operation.call()).thenThrow(new DockerExecutionException()).thenAnswer(i -> {
			stopwatch.stop();
			assertThat(stopwatch.getTotalTimeMillis()).isGreaterThan(100L);
			return "success";
		});

		String result = timeRetryer.runWithRetries(operation);
		assertThat(result).isEqualTo("success");
	}

	@Test
	public void retry_the_operation_if_it_failed_once_and_return_the_result_of_the_next_successful_call() throws Exception {
		when(operation.call()).thenAnswer(MockitoMultiAnswer.<String>of(
				firstInvocation -> {
					throw new DockerExecutionException();
				},
				secondInvocation -> "hola"
		));

		assertThat(retryer.runWithRetries(operation)).isEqualTo("hola");
		verify(operation, times(2)).call();
	}

	@Test
	public void throw_the_last_exception_when_the_operation_fails_more_times_than_the_number_of_specified_retry_attempts() throws Exception {
		DockerExecutionException finalException = new DockerExecutionException();

		when(operation.call()).thenAnswer(MockitoMultiAnswer.<String>of(
				firstInvocation -> {
					throw new DockerExecutionException();
				},
				secondInvocation -> {
					throw finalException;
				}
		));

		try {
			retryer.runWithRetries(operation);
			fail("Should have caught exception");
		} catch (DockerExecutionException actualException) {
			assertThat(actualException).isEqualTo(finalException);
		}

		verify(operation, times(2)).call();
	}
}
