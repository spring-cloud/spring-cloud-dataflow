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

package org.springframework.cloud.dataflow.completion;

import org.hamcrest.FeatureMatcher;

/**
 * Contains helper Hamcrest matchers for testing completion proposal related code.
 *
 * @author Eric Bottard
 */
class Proposals {
	static org.hamcrest.Matcher<CompletionProposal> proposalThat(org.hamcrest.Matcher<String> matcher) {
		return new FeatureMatcher<CompletionProposal, String>(matcher, "a proposal whose text", "text") {
			@Override
			protected String featureValueOf(CompletionProposal actual) {
				return actual.getText();
			}
		};
	}
}
