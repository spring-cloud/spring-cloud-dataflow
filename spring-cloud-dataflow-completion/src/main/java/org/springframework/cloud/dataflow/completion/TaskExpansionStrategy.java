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

import java.util.List;

import org.springframework.cloud.dataflow.core.TaskDefinition;

/**
 * Used to enhance a well formed task definition by adding yet more text to it
 * (<i>e.g.</i> adding more options to a module).
 *
 * @author Eric Bottard
 * @author Andy Clement
 */
public interface TaskExpansionStrategy {

	/**
	 * For a given task DSL text and {@link TaskDefinition},
	 * <ul>
	 * <li>Generate {@link CompletionProposal}s that apply (if any) and add them to the
	 * provided {@code collector} list</li>
	 * <li>Return {@code true} if no other strategies should be applied for the task DSL
	 * text (this strategy make take the liberty to erase already collected proposals)
	 * </li>
	 * </ul>
	 *
	 * @param text DSL text for the task
	 * @param taskDefinition task definition
	 * @param detailLevel integer representing the amount of detail to include in the
	 * generated {@code CompletionProposal}s (higher values mean more details. typical
	 * range is [1..5])
	 * @param collector list of {@code CompletionProposal}s to add/remove from if this
	 * strategy applies
	 * @return {@code true} if no other strategies should be applied for the task DSL text
	 */
	boolean addProposals(String text, TaskDefinition taskDefinition, int detailLevel,
			List<CompletionProposal> collector);

}
