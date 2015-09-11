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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.dsl.ParseException;

/**
 * Provides completion when the user has typed in the first dash to a module configuration property.
 *
 * @author Eric Bottard
 */
class ExpandOneDashToTwoDashesRecoveryStrategy extends StacktraceFingerprintingRecoveryStrategy<ParseException> {

	@Autowired
	private StreamCompletionProvider completionProvider;

	public ExpandOneDashToTwoDashesRecoveryStrategy() {
		super(ParseException.class, "file -");
	}

	@Override
	public void addProposals(String dsl, ParseException exception, int detailLevel, List<CompletionProposal> proposals) {
		// Pretend there was an additional dash and invoke recursively
		List<CompletionProposal> completions = completionProvider.complete(dsl + "-", detailLevel);
		proposals.addAll(completions);
	}

}
