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

import java.util.List;

/**
 * Used to provide completions on ill-formed stream definitions, after an initial (failed)
 * parse.
 *
 * @param <E> the kind of exception that is intercepted during parsing
 * @author Eric Bottard
 */
public interface RecoveryStrategy<E extends Exception> {

	/**
	 * Whether this completion should be triggered.
	 *
	 * @param dslStart the partial DSL text
	 * @param exception the exception thrown when parsing the DSL text
	 * @return if proposals to complete the DSL should be provided
	 */
	boolean shouldTrigger(String dslStart, Exception exception);

	/**
	 * Perform code completion by adding proposals to the {@code proposals} list.
	 *
	 * @param dsl the partial DSL text
	 * @param exception the exception thrown when parsing the DSL text
	 * @param detailLevel an integer describing the level of detail to include in
	 * proposal, starting at 1. Higher values request more detail, with values typically
	 * in the range [1..5]
	 * @param proposals the list of completion proposals to show the user
	 */
	void addProposals(String dsl, E exception, int detailLevel, List<CompletionProposal> proposals);
}
