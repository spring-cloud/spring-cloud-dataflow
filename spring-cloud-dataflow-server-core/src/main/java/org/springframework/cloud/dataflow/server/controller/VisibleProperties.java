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

package org.springframework.cloud.dataflow.server.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.server.support.RelaxedNames;
import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Knows how to expand app properties into their full form if visible properties (short
 * hand form) have been used.
 *
 * @author Eric Bottard
 */
public class VisibleProperties {

	/**
	 * Used to expand short form of included properties to their long form.
	 */
	private final ApplicationConfigurationMetadataResolver metadataResolver;

	public VisibleProperties(ApplicationConfigurationMetadataResolver metadataResolver) {
		this.metadataResolver = metadataResolver;
	}

	/**
	 * Return a copy of app properties where shorthand form have been expanded to their
	 * long form (amongst the visible supported properties of the app) if applicable.
	 *
	 * @param properties the application properties in shorthand form
	 * @param metadataResource the metadata that can be used to expand shorthand property
	 * names to long form names
	 * @return the application properties with expanded long form property names
	 */
	public Map<String, String> qualifyProperties(Map<String, String> properties, Resource metadataResource) {
		MultiValueMap<String, ConfigurationMetadataProperty> visible = new LinkedMultiValueMap<>();
		Set<String> allProps = new HashSet<>();

		for (ConfigurationMetadataProperty property : this.metadataResolver.listProperties(metadataResource, false)) {
			visible.add(property.getName(), property);// Use names here
		}
		for (ConfigurationMetadataProperty property : this.metadataResolver.listProperties(metadataResource, true)) {
			allProps.add(property.getId()); // But full ids here
		}

		Map<String, String> mutatedProps = new HashMap<>(properties.size());
		for (Map.Entry<String, String> entry : properties.entrySet()) {
			String provided = entry.getKey();
			if (!allProps.contains(provided)) {
				List<ConfigurationMetadataProperty> longForms = null;
				for (String relaxed : new RelaxedNames(provided)) {
					longForms = visible.get(relaxed);
					if (longForms != null) {
						break;
					}
				}
				if (longForms != null) {
					assertNoAmbiguity(longForms);
					mutatedProps.put(longForms.iterator().next().getId(), entry.getValue());
				}
				else {
					mutatedProps.put(provided, entry.getValue());
				}
			}
			else {
				mutatedProps.put(provided, entry.getValue());
			}
		}
		return mutatedProps;
	}

	private void assertNoAmbiguity(List<ConfigurationMetadataProperty> longForms) {
		if (longForms.size() > 1) {
			Set<String> ids = new HashSet<>(longForms.size());
			for (ConfigurationMetadataProperty pty : longForms) {
				ids.add(pty.getId());
			}
			throw new IllegalArgumentException(String.format("Ambiguous short form property '%s' could mean any of %s",
					longForms.iterator().next().getName(), ids));
		}
	}

}
