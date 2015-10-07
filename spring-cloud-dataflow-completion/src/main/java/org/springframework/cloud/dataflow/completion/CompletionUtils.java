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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.cloud.dataflow.core.BindingProperties;
import org.springframework.cloud.dataflow.core.ModuleCoordinates;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.ModuleType;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.stream.module.resolver.Coordinates;
import org.springframework.util.Assert;

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
		IMPLICIT_PARAMETER_NAMES.add(BindingProperties.INPUT_BINDING_KEY);
		IMPLICIT_PARAMETER_NAMES.add(BindingProperties.OUTPUT_BINDING_KEY);
	}

	/**
	 * Return the type(s) of a given module definition, in the context of code completion.
	 */
	static ModuleType[] inferType(ModuleDefinition moduleDefinition) {
		Set<String> properties = moduleDefinition.getParameters().keySet();
		if (properties.contains(BindingProperties.INPUT_BINDING_KEY)) {
			// Can't be source. For the purpose of completion, being the last module
			// (hence having BindingProperties.OUTPUT_BINDING_KEY not set) does NOT guarantee we're dealing
			// with a sink (could be an unfinished "source | processor | processor" stream)
			if (properties.contains(BindingProperties.OUTPUT_BINDING_KEY)) {
				return new ModuleType[] {ModuleType.processor};
			} else {
				return new ModuleType[] {ModuleType.processor, ModuleType.sink};
			}
		} // MUST be source
		else {
			return new ModuleType[] {ModuleType.source};
		}
	}

	static Coordinates adapt(ModuleCoordinates coordinates) {
		return new Coordinates(coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getExtension(), "exec", coordinates.getVersion());
	}

	/**
	 * Given a candidate module name, maybe prefix it with an auto-generated label if its use would clash with
	 * an already existing definition.
	 */
	static String maybeQualifyWithLabel(String moduleName, StreamDefinition streamDefinition) {
		String candidate = moduleName;
		Pattern pattern = Pattern.compile("^(?<prefix>.+?)(?<number>\\d+)?$");
		Matcher matcher = pattern.matcher(moduleName);
		Assert.isTrue(matcher.matches(), "Module name did match the expected format");
		String prefix = matcher.group("prefix");
		int counter = matcher.group("number") == null ? 2 : Integer.parseInt(matcher.group("number"));


		Set<String> alreadyUsed = new HashSet<>();
		for (ModuleDefinition moduleDefinition : streamDefinition.getModuleDefinitions()) {
			alreadyUsed.add(moduleDefinition.getLabel());
		}
		String result = candidate;
		while (alreadyUsed.contains(candidate)) {
			candidate = prefix + counter++;
			result = String.format("%s: %s", candidate, moduleName);
		}
		return result;
	}
}
