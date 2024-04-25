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
import java.util.stream.Collectors;

import org.assertj.core.api.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Contains helper Hamcrest matchers for testing completion proposal related code.
 *
 * @author Eric Bottard
 */
class Proposals {
	private static final Logger log = LoggerFactory.getLogger(Proposals.class);

	static Condition<CompletionProposal> proposalThatIs(String text) {
		return new Condition<>(item -> text.equals(item.getText()), "proposalThatIs");
	}
	static Condition<CompletionProposal> proposalThatStartsWith(String text) {
		return new Condition<>(item -> item.getText().startsWith(text), "proposalThatStartsWith");
	}
	public static Condition<? super List<? extends CompletionProposal>> proposalThatHas(boolean all, String ...text) {
		Set<String> texts = new HashSet<>(Arrays.asList(text));
		if(all) {
			return new Condition<>(items -> {
				Set<String> itemStrings = items.stream().map(completionProposal -> completionProposal.getText()).collect(Collectors.toSet());
				return texts.stream().allMatch(txt -> itemStrings.contains(txt));
				},"proposalThatHasAll");
		} else {
			return new Condition<>(items ->  {
				Set<String> itemStrings = items.stream().map(completionProposal -> completionProposal.getText()).collect(Collectors.toSet());
				return texts.stream().anyMatch(txt -> itemStrings.contains(txt));
				}, "proposalThatHasAny");
		}
	}
	public static Condition<? super List<? extends CompletionProposal>> proposalThatHasAll(String ...text) {
		return proposalThatHas(true, text);
	}
	public static Condition<? super List<? extends CompletionProposal>> proposalThatHasAny(String ...text) {
		return proposalThatHas(false, text);
	}
}
