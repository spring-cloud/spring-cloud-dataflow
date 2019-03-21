/*
 * Copyright 2015 the original author or authors.
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

/**
 * Represents a proposal to complete a given piece of definition DSL.
 *
 * @author Eric Bottard
 */
public class CompletionProposal {

	/**
	 * The full text of the proposal. Also includes the prefix that was used to
	 * trigger the completion, as some strategies may decide to actually
	 * <em>overwrite</em> that prefix (<i>e.g.</i> to provide correction).
	 */
	private final String text;

	/**
	 * Some description of the completion, may be {@literal null}.
	 */
	private final String explanation;

	public CompletionProposal(String text, String explanation) {
		this.text = text;
		this.explanation = explanation;
	}

	public static Factory expanding(String start) {
		return new Factory(start);
	}

	public String getText() {
		return text;
	}

	public String getExplanation() {
		return explanation;
	}

	@Override
	public String toString() {
		return String.format("%s[%s]", getClass().getSimpleName(), text);
	}

	public static class Factory {

		private final String start;

		private Factory(String start) {
			this.start = start;
		}

		public CompletionProposal withSuffix(String suffix, String explanation) {
			return new CompletionProposal(start + suffix, explanation);
		}

		public CompletionProposal withSuffix(String suffix) {
			return withSuffix(suffix, null);
		}

		/**
		 * Add a suffix as a new token, that is, make sure there is a space before it.
		 *
		 * A space is not appended if this is the very first token.
		 */
		public CompletionProposal withSeparateTokens(String suffix, String explanation) {
			return new CompletionProposal((start.endsWith(" ")
					|| start.isEmpty()) ? start + suffix : start + " " + suffix, explanation);
		}

		/**
		 * Add a suffix as a new token, that is, make sure there is a space before it.
		 *
		 * A space is not appended if this is the very first token.
		 */
		public CompletionProposal withSeparateTokens(String suffix) {
			return withSeparateTokens(suffix, null);
		}
	}

}
