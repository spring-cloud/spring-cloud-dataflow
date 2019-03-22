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

import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;

/**
 * Proposes source app names when the user has typed nothing.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Andy Clement
 */
class EmptyStartYieldsSourceAppsTaskRecoveryStrategy
		extends StacktraceFingerprintingTaskRecoveryStrategy<CheckPointedParseException> {

	private final AppRegistryService registry;

	public EmptyStartYieldsSourceAppsTaskRecoveryStrategy(AppRegistryService registry) {
		super(CheckPointedParseException.class, "");
		this.registry = registry;
	}

	@Override
	public void addProposals(String dsl, CheckPointedParseException exception, int detailLevel,
			List<CompletionProposal> proposals) {
		CompletionProposal.Factory completionFactory = CompletionProposal.expanding(dsl);
		for (AppRegistration app : this.registry.findAll()) {
			if (app.getType() == ApplicationType.task) {
				proposals.add(completionFactory.withSeparateTokens(app.getName(), "Choose a task app"));
			}
		}
	}

}
