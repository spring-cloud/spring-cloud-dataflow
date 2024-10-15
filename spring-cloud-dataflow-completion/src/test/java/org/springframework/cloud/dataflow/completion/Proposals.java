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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.assertj.core.api.Condition;

/**
 * Contains helper Hamcrest matchers for testing completion proposal related code.
 *
 * @author Eric Bottard
 * @author Corneil du Plessis
 */
class Proposals {
	static Condition<CompletionProposal> proposal(String text) {
		return new Condition<>(actual -> text.equals(actual.getText()) , "text:" + text);
	}
	static Condition<CompletionProposal> proposal(Predicate<String> check) {
		return new Condition<>(actual -> check.test(actual.getText()) , "check");
	}
	static boolean hasAny(List<? extends CompletionProposal> proposals, String ... text) {
		Set<String> items = new HashSet<>(Arrays.asList(text));
		return proposals.stream().anyMatch(item -> items.contains(item.getText()));
	}
	static boolean hasAll(List<? extends CompletionProposal> proposals, String ... text) {
		Set<String> items = new HashSet<>(Arrays.asList(text));
		Set<String> proposalTextItems = proposals.stream().map(completionProposal -> completionProposal.getText()).collect(Collectors.toSet());
		return items.stream().allMatch(proposalTextItems::contains);
	}
	static Condition<List<? extends CompletionProposal>> any(String ... text) {
		return new Condition<>(actual-> hasAny(actual, text), "hasAny");
	}
	static Condition<List<? extends CompletionProposal>> all(String ... text) {
		return new Condition<>(actual-> hasAll(actual, text), "hasAll");
	}
}
