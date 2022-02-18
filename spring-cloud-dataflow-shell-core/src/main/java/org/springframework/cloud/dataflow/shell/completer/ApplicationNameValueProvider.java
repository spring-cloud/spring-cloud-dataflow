/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.completer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.core.MethodParameter;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProviderSupport;
import org.springframework.stereotype.Component;

/**
 * A {@link org.springframework.shell.standard.ValueProvider} that provides completion for Data Flow registered
 * application names.
 *
 * @author Chris Bono
 */
@Component
public class ApplicationNameValueProvider extends ValueProviderSupport {

	private final DataFlowShell dataFlowShell;

	public ApplicationNameValueProvider(DataFlowShell dataFlowShell) {
		this.dataFlowShell = dataFlowShell;
	}

	@Override
	public List<CompletionProposal> complete(MethodParameter parameter, CompletionContext completionContext, String[] hints) {
		return StreamSupport.stream(
				dataFlowShell.getDataFlowOperations().appRegistryOperations().list().spliterator(), false)
				.map(AppRegistrationResource::getName)
				.map(CompletionProposal::new)
				.collect(Collectors.toList());
	}
}
