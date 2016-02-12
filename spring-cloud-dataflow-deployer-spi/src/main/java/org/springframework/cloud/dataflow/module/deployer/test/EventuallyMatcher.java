/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.module.deployer.test;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;


/**
 * A matcher that will evaluate another matcher repeatedly until it matches, or fail after some number of attempts.
 *
 * @param <U> the type the wrapped matcher operates on
 *
 * @author Eric Bottard
 */
public class EventuallyMatcher<U> extends DiagnosingMatcher<U> {

	private final Matcher<U> delegate;

	private int noAttempts;

	private int pause;

	public EventuallyMatcher(Matcher<U> delegate) {
		this(delegate, 20, 100);
	}

	public EventuallyMatcher(Matcher<U> delegate, int noAttempts, int pause) {
		this.delegate = delegate;
		this.noAttempts = noAttempts;
		this.pause = pause;
	}

	@Override
	public void describeTo(Description description) {
		description.appendDescriptionOf(delegate).appendText(String.format(", trying at most %d times", noAttempts));
	}

	@Override
	protected boolean matches(Object item, Description mismatchDescription) {
		mismatchDescription.appendText(String.format("failed after %d*%d=%dms:%n", noAttempts, pause, noAttempts
				* pause));
		for (int i = 0; i < noAttempts; i++) {
			boolean result = delegate.matches(item);
			if (result) {
				return true;
			}
			delegate.describeMismatch(item, mismatchDescription);
			mismatchDescription.appendText(", ");
			try {
				Thread.sleep(pause);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return false;
	}

	public static <T> EventuallyMatcher<T> eventually(Matcher<T> delegate) {
		return new EventuallyMatcher<>(delegate);
	}

	public static <T> EventuallyMatcher<T> eventually(Matcher<T> delegate, int noAttempts, int pause) {
		return new EventuallyMatcher<>(delegate, noAttempts, pause);
	}
}
