/*
 * Copyright 2015-2016 the original author or authors.
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

import static org.springframework.cloud.dataflow.core.ApplicationType.processor;
import static org.springframework.cloud.dataflow.core.ApplicationType.sink;

import java.util.List;

import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;

/**
 * Continues a well-formed stream definition by adding a pipe symbol and another app,
 * provided that the stream definition hasn't reached its end yet.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
public class PipeIntoOtherAppsExpansionStrategy implements ExpansionStrategy {

	private final AppRegistry appRegistry;

	public PipeIntoOtherAppsExpansionStrategy(AppRegistry appRegistry) {
		this.appRegistry = appRegistry;
	}

	@Override
	public boolean addProposals(String text, StreamDefinition parseResult, int detailLevel,
			List<CompletionProposal> collector) {
		if (text.isEmpty() || !text.endsWith(" ")) {
			return false;
		}
		StreamAppDefinition lastApp = parseResult.getDeploymentOrderIterator().next();
		// Consider "bar | foo". If there is indeed a sink named foo in the registry,
		// "foo" may also be a processor, in which case we can continue
		boolean couldBeASink = appRegistry.find(lastApp.getName(), sink) != null;
		if (couldBeASink) {
			boolean couldBeAProcessor = appRegistry.find(lastApp.getName(), processor) != null;
			if (!couldBeAProcessor) {
				return false;
			}
		}

		CompletionProposal.Factory proposals = CompletionProposal.expanding(text);
		for (AppRegistration appRegistration : appRegistry.findAll()) {
			if (appRegistration.getType() == processor || appRegistration.getType() == sink) {
				String expansion = CompletionUtils.maybeQualifyWithLabel(appRegistration.getName(), parseResult);
				collector.add(proposals.withSeparateTokens("| " + expansion,
						"Continue stream definition with a " + appRegistration.getType()));
			}
		}
		return false;
	}
}
