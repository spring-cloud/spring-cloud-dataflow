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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.BindingPropertyKeys;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;

/**
 * Various utility methods used throughout the completion package.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
public class CompletionUtils {

	/**
	 * The names of properties that may be added implicitly to all stream app definitions,
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
	 * Return the type(s) a given stream app definition <em>could</em> have, in the context of code completion.
	 */
	static ApplicationType[] determinePotentialTypes(StreamAppDefinition appDefinition) {
		Set<String> properties = appDefinition.getProperties().keySet();
		if (properties.contains(BindingPropertyKeys.INPUT_DESTINATION)) {
			// Can't be source. For the purpose of completion, being the last app
			// (hence having BindingPropertyKeys.OUTPUT_DESTINATION not set) does NOT guarantee we're dealing
			// with a sink (could be an unfinished "source | processor | processor" stream)
			if (properties.contains(BindingPropertyKeys.OUTPUT_DESTINATION)) {
				return new ApplicationType[] {ApplicationType.processor};
			}
			else {
				return new ApplicationType[] {ApplicationType.processor, ApplicationType.sink};
			}
		} // MUST be source
		else {
			return new ApplicationType[] {ApplicationType.source};
		}
	}

	/**
	 * Given a candidate app name, maybe prefix it with an auto-generated label
	 * if its use would clash with an already existing definition.
	 *
	 * <p>As an example, consider the (unfinished) stream definition
	 * {@literal http | filter | filter}. Here {@literal appName} refers to the
	 * second "filter" app name. An invocation of this method would return
	 * {@literal "filter2: filter"} in that case.</p>
	 * <p>Contrast this with the case of {@literal http | transform | filter},
	 * where "filter" is not yet used. This method would simply return an unaltered
	 * "filter" in that case.</p>
	 */
	static String maybeQualifyWithLabel(String appName, StreamDefinition streamDefinition) {
		String candidate = appName;

		Set<String> alreadyUsed = new HashSet<>();
		for (StreamAppDefinition appDefinition : streamDefinition.getAppDefinitions()) {
			alreadyUsed.add(appDefinition.getName());
		}

		String result = candidate;
		int counter = 2;
		while (alreadyUsed.contains(candidate)) {
			candidate = appName + counter++;
			result = String.format("%s: %s", candidate, appName);
		}
		return result;
	}

	/**
	 * Return whether the given property name should be considered matching the candidate configuration property, also
	 * taking into account the list of whitelist properties (which are tested on their short name).
	 */
	static boolean isMatchingProperty(String propertyName, ConfigurationMetadataProperty property, List<ConfigurationMetadataProperty> whiteListedProps) {
		if (property.getId().equals(propertyName)) {
			return true; // For any prop
		} // Handle special case of short form for whitelist
		else {
			for (ConfigurationMetadataProperty white : whiteListedProps) {
				if (property.getId().equals(white.getId())) { // prop#equals() not implemented
					return property.getName().equals(propertyName);
				}
			}
			return false;
		}
	}
}
