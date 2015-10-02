/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.completion;

/**
 * Represents a proposal to complete a given piece of definition DSL.
 *
 * @author Eric Bottard
 */
public class CompletionProposal {

	private final String text;

	private final String explanation;

	public CompletionProposal(String text, String explanation) {
		this.text = text;
		this.explanation = explanation;
	}

	public String getText() {
		return text;
	}

	public String getExplanation() {
		return explanation;
	}

	public static class Factory {

		private final String start;

		private Factory(String start) {
			this.start = start;
		}

		public static Factory expanding(String start) {
			return new Factory(start);
		}

		public CompletionProposal withSuffix(String suffix, String explanation) {
			return new CompletionProposal(start + suffix, explanation);
		}

		public CompletionProposal withSuffix(String suffix) {
			return withSuffix(suffix, null);
		}

		public CompletionProposal withSeparateTokens(String suffix, String explanation) {
			return new CompletionProposal(start.endsWith(" ") ? start + suffix : start + " " + suffix, explanation);
		}

		public CompletionProposal withSeparateTokens(String suffix) {
			return withSeparateTokens(suffix, null);
		}
	}

}
