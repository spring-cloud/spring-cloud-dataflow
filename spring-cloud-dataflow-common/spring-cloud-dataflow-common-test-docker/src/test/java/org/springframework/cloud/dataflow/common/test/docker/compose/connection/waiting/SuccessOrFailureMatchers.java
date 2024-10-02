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

import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.equalTo;

public class SuccessOrFailureMatchers {

    public static class Successful extends TypeSafeDiagnosingMatcher<SuccessOrFailure> {
        @Override
        protected boolean matchesSafely(SuccessOrFailure item, Description mismatchDescription) {
            if (item.failed()) {
                mismatchDescription.appendValue(item);
            }

            return item.succeeded();
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("is successful");
        }
    }

    public static Matcher<SuccessOrFailure> successful() {
        return new Successful();
    }

    public static class Failure extends FeatureMatcher<SuccessOrFailure, String> {
        public Failure(Matcher<? super String> subMatcher) {
            super(subMatcher, "failure message of", "failure message");
        }

        @Override
        protected String featureValueOf(SuccessOrFailure actual) {
            return actual.failureMessage();
        }

        @Override
        protected boolean matchesSafely(SuccessOrFailure actual, Description mismatch) {
            if (actual.succeeded()) {
                mismatch.appendValue(actual);
                return false;
            }

            return super.matchesSafely(actual, mismatch);
        }
    }

    public static Matcher<SuccessOrFailure> failure() {
        return new Failure(anything());
    }

    public static Matcher<SuccessOrFailure> failureWithMessage(Matcher<String> messageMatcher) {
        return new Failure(messageMatcher);
    }

    public static Matcher<SuccessOrFailure> failureWithMessage(String message) {
        return new Failure(equalTo(message));
    }
}
