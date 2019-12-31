/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.integration.test.util;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Utilities that helps to blocks execution until a predicate is satisfied or throws a IllegalStateException if
 * the preconfigured timeout duration is exceeded. The until method, repeatedly tests the predicate
 * in pauseDuration intervals. For instance:
 *  <pre>
 *     {@code
 *     Wait.on(stream).withTimeout(Duration.ofMinutes(5)).until(s -> s.getStatus().equals("deployed"));
 *     }
 * </pre>
 *
 * For void targets use the following syntax:
 *  <pre>
 *     {@code
 *     Wait.on(Void.class).until(() -> System.currentTimeMillis() > someValue);
 *     }
 * </pre>
 *
 * @param <T> An argument passed to the predicate on every test.
 *
 * @author Christian Tzolov
 */
public class Wait<T> {

	/**
	 * An argument passed to the predicate on every test.
	 */
	private T target;

	/**
	 * Total time to wait until the predicate is satisfied. When the timeoutDuration is exceeded
	 * an {@link IllegalStateException} is thrown.
	 */
	private Duration timeoutDuration = Duration.ofMinutes(5);

	/**
	 * Time for waiting between two consecutive predicate tests.
	 */
	private Duration pauseDuration = Duration.ofSeconds(10);

	/**
	 * Human readable description send in the error message.
	 */
	private String description = "";


	private Wait(T target) {
		this.target = target;
	}

	public static <T> Wait<T> on(T t) {
		return new Wait<>(t);
	}

	public Wait<T> withTimeout(Duration timeout) {
		this.timeoutDuration = timeout;
		return this;
	}

	public Wait<T> withPause(Duration timeout) {
		this.pauseDuration = timeout;
		return this;
	}

	public Wait<T> withDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Blocks until the wait-predicate is satisfied or throws a IllegalStateException if the timeoutDuration expires.
	 * Repeatedly tests the predicate in pauseDuration intervals.
	 *
	 * TODO: for Java9 consider using CompletableFuture instead.
	 *
	 * @param waitPredicate Condition which when satisfied (e.g. #test() returns true) unblocks the call.
	 */
	public void until(Predicate<T> waitPredicate) {

		final long timeout = System.currentTimeMillis() + this.timeoutDuration.toMillis();

		while (System.currentTimeMillis() < timeout) {
			if (waitPredicate.test(this.target)) {
				return;
			}
			try {
				TimeUnit.SECONDS.sleep(this.pauseDuration.getSeconds());
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
		throw new IllegalStateException(String.format("Timeout after: %s [sec], description: %s",
				this.timeoutDuration.getSeconds(), "While waiting on (" + this.target + ") - " + this.description));
	}
}
