/*
 * Copyright 2015-2016 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;

import org.springframework.cloud.dataflow.core.ArtifactType;
import org.springframework.cloud.dataflow.core.BindingPropertyKeys;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;

/**
 * Various utility methods used throughout the completion package.
 *
 * @author Eric Bottard
 */
public class CompletionUtils {

	/**
	 * The names of properties that may be added implicitly to all module definitions,
	 * even though the user did not type them himself.
	 */
	static final Set<String> IMPLICIT_PARAMETER_NAMES = new HashSet<>();
	static {
		IMPLICIT_PARAMETER_NAMES.add(BindingPropertyKeys.INPUT_DESTINATION);
		IMPLICIT_PARAMETER_NAMES.add(BindingPropertyKeys.INPUT_GROUP);
		IMPLICIT_PARAMETER_NAMES.add(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS);
		IMPLICIT_PARAMETER_NAMES.add(BindingPropertyKeys.OUTPUT_DESTINATION);
	}

	/**
	 * Return the type(s) a given module definition <em>could</em> have, in the context of code completion.
	 */
	static ArtifactType[] determinePotentialTypes(ModuleDefinition moduleDefinition) {
		Set<String> properties = moduleDefinition.getParameters().keySet();
		if (properties.contains(BindingPropertyKeys.INPUT_DESTINATION)) {
			// Can't be source. For the purpose of completion, being the last module
			// (hence having BindingPropertyKeys.OUTPUT_DESTINATION not set) does NOT guarantee we're dealing
			// with a sink (could be an unfinished "source | processor | processor" stream)
			if (properties.contains(BindingPropertyKeys.OUTPUT_DESTINATION)) {
				return new ArtifactType[] {ArtifactType.processor};
			}
			else {
				return new ArtifactType[] {ArtifactType.processor, ArtifactType.sink};
			}
		} // MUST be source
		else {
			return new ArtifactType[] {ArtifactType.source};
		}
	}

	/**
	 * Given a candidate module name, maybe prefix it with an auto-generated label
	 * if its use would clash with an already existing definition.
	 *
	 * <p>As an example, consider the (unfinished) stream definition
	 * {@literal http | filter | filter}. Here {@literal moduleName} refers to the
	 * second "filter" module name. An invocation of this method would return
	 * {@literal "filter2: filter"} in that case.</p>
	 * <p>Contrast this with the case of {@literal http | transform | filter},
	 * where "filter" is not yet used. This method would simply return an unaltered
	 * "filter" in that case.</p>
	 */
	static String maybeQualifyWithLabel(String moduleName, StreamDefinition streamDefinition) {
		String candidate = moduleName;

		Set<String> alreadyUsed = new HashSet<>();
		for (ModuleDefinition moduleDefinition : streamDefinition.getModuleDefinitions()) {
			alreadyUsed.add(moduleDefinition.getLabel());
		}

		String result = candidate;
		int counter = 2;
		while (alreadyUsed.contains(candidate)) {
			candidate = moduleName + counter++;
			result = String.format("%s: %s", candidate, moduleName);
		}
		return result;
	}
}
