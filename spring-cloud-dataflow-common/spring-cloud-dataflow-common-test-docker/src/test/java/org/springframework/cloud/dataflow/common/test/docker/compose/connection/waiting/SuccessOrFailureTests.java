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
package org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.SuccessOrFailureMatchers.failure;
import static org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.SuccessOrFailureMatchers.failureWithMessage;
import static org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.SuccessOrFailureMatchers.successful;

public class SuccessOrFailureTests {
    @Test
    public void not_have_failed_if_actually_a_success() {
		assertThat(SuccessOrFailure.success(), successful());
    }

    @Test
    public void have_failed_if_actually_a_failure() {
		assertThat(SuccessOrFailure.failure("oops"), failure());
    }

    @Test
    public void return_the_failure_message_if_set() {
		assertThat(SuccessOrFailure.failure("oops"), failureWithMessage("oops"));
    }

    @Test
    public void fail_from_an_exception() {
        Exception exception = new RuntimeException("oh no");
        assertThat(SuccessOrFailure.fromException(exception),
				failureWithMessage(containsString("oh no")));
    }

    @Test
    public void succeed_on_a_lambda_that_returns_true() {
        SuccessOrFailure successFromLambda = SuccessOrFailure.onResultOf(() -> true);
		assertThat(successFromLambda, successful());
    }

    @Test
    public void fail_on_a_lambda_that_throws_an_exception() {
        SuccessOrFailure failureFromLambda = SuccessOrFailure.onResultOf(() -> {
            throw new IllegalArgumentException("oh no");
        });

        assertThat(failureFromLambda, failureWithMessage(containsString("oh no")));
    }

    @Test
    public void fail_on_a_lambda_that_returns_false() {
        SuccessOrFailure failureFromLambda = SuccessOrFailure.onResultOf(() -> false);

		assertThat(failureFromLambda, failureWithMessage("Attempt to complete healthcheck failed"));
    }
}
