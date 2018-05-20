/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.server.util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.samskivert.mustache.Mustache;

import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.Template;

/**
 * Utility functions for manifest file processing.
 * @author Mark Pollack
 */
public class ManifestUtils {

	/**
	 * Iterate overall the template files, replacing placeholders with model values. One
	 * string is returned that contain all the YAML of multiple files using YAML file
	 * delimiter.
	 * @param packageToDeploy The top level package that contains all templates where
	 * placeholders are to be replaced
	 * @param model The placeholder values.
	 * @return A YAML string containing all the templates with replaced values.
	 */
	@SuppressWarnings("unchecked")
	public static String createManifest(Package packageToDeploy, Map<String, ?> model) {

		// Aggregate all valid manifests into one big doc.
		StringBuilder sb = new StringBuilder();
		// Top level templates.
		List<Template> templates = packageToDeploy.getTemplates();
		if (templates != null) {
			for (Template template : templates) {
				String templateAsString = new String(template.getData());
				com.samskivert.mustache.Template mustacheTemplate = Mustache.compiler().compile(templateAsString);
				sb.append("\n---\n# Source: " + template.getName() + "\n");
				sb.append(mustacheTemplate.execute(model));
			}
		}
		for (Package pkg : packageToDeploy.getDependencies()) {
			String packageName = pkg.getMetadata().getName();
			Map<String, Object> modelForDependency;
			if (model.containsKey(packageName)) {
				modelForDependency = (Map<String, Object>) model.get(pkg.getMetadata().getName());
			}
			else {
				modelForDependency = new TreeMap<>();
			}
			sb.append(createManifest(pkg, modelForDependency));
		}

		return sb.toString();
	}
}
