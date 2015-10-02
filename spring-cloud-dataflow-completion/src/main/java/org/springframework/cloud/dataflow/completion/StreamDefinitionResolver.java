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
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.ModuleType;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.module.registry.ModuleRegistration;
import org.springframework.cloud.dataflow.module.registry.ModuleRegistry;

/**
 * Encapsulates parsing of a DSL definition as well as trying to resolve modules once parsing is done.
 *
 * This is useful as StacktraceFingerprintingCompletionRecoveryStrategy expects failures coming
 * from a single call point.
 *
 * @author Eric Bottard
 */
public class StreamDefinitionResolver {

	@Autowired
	private ModuleRegistry registry;

	public StreamDefinition parseAndResolve(String dsl) {
		StreamDefinition streamDefinition = new StreamDefinition("__dummy", dsl);
		List<ModuleDefinition> moduleDefinitions = streamDefinition.getModuleDefinitions();
		for (int i = 0; i < moduleDefinitions.size(); i++) {
			ModuleDefinition currentModule = moduleDefinitions.get(i);
			ModuleType type = (i == 0) ? ModuleType.source
					: (i < moduleDefinitions.size() ? ModuleType.processor : ModuleType.sink);
			ModuleRegistration registration = this.registry.find(currentModule.getName(), type);
			if (registration == null) {
				throw new IllegalArgumentException(String.format(
						"Module %s of type %s not found in registry", currentModule.getName(), type));
			}
		}
		return streamDefinition;
	}
}
