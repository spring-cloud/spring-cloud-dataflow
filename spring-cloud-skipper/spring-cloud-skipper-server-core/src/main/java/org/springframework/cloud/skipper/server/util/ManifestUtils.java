/*
 * Copyright 2017-2022 the original author or authors.
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
package org.springframework.cloud.skipper.server.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.samskivert.mustache.Mustache;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.util.StringUtils;

/**
 * Utility functions for manifest file processing.
 * @author Mark Pollack
 * @author Christian Tzolov
 * @author Chris Bono
 */
public class ManifestUtils {

	/**
	 * In Java '\\\\' sands for escaped backslash. E.g. it corresponds to '\\' in regular expression.
	 *
	 * (?<!\\) - (negative lookbehind) - the previous character is not a backslash.
	 * (\\)    - (non-capturing group) - current character is a backslash
	 * (?![\\0abtnvfreN_LP\s"]) - (negative lookforward) - next character is neither of \, 0, a, b, t, n, v, f, r, e, N, _, L, P, ,\s, "
	 */
	private static final Pattern SINGLE_BACKSLASH = Pattern.compile("(?<!\\\\)(\\\\)(?![\\\\0abtnvfreN_LP\\s\"])");

	/**
	 * Resolve a kind from a raw manifest yaml.
	 *
	 * @param manifest the raw yaml
	 * @return the kind or {@code null} if not found
	 */
	public static String resolveKind(String manifest) {
		if (!StringUtils.hasText(manifest)) {
			return null;
		}
		LoaderOptions options = new LoaderOptions();
		Yaml yaml = new Yaml(new SafeConstructor(options));
		Iterable<Object> object = yaml.loadAll(manifest);
		for (Object o : object) {
			if (o != null && o instanceof Map<?,?> map) {
				Object kind = map.get("kind");
				if (kind instanceof String string) {
					return string;
				}
			}
		}
		return null;
	}

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
		Map<String, Object> newModel = new HashMap<>();
		backslashEscapeMap((Map<String, Object>) model, newModel);
		String rawManifest = applyManifestTemplate(packageToDeploy, newModel);

		Yaml yaml = createYaml();
		// Lazy evaluation of the generated manifest
		List<Object> yamlList = StreamSupport
				.stream(yaml.loadAll(rawManifest).spliterator(), false)
				.collect(Collectors.toList());

		return yaml.dumpAll(yamlList.iterator());
	}

	private static String applyManifestTemplate(Package packageToDeploy, Map<String, ?> model) {

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
			sb.append(applyManifestTemplate(pkg, modelForDependency));
		}

		return sb.toString();
	}

	private static void backslashEscapeMap(Map<String, Object> input, Map<String, Object> output) {
		for (Map.Entry<String, Object> e : input.entrySet()) {
			if (e.getValue() instanceof Map) {
				Map<String, Object> map2 = new HashMap<>();
				backslashEscapeMap((Map<String, Object>) e.getValue(), map2);
				output.put(e.getKey(), map2);
			}
			else {
				output.put(e.getKey(), backslashEscape("" + e.getValue()));
			}
		}
	}

	private static String backslashEscape(String text) {
		return SINGLE_BACKSLASH.matcher(text).replaceAll("\\\\\\\\");
	}

	private static Yaml createYaml() {
		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
		dumperOptions.setPrettyFlow(true);
		dumperOptions.setSplitLines(false);
		return new Yaml(new ValueTypeRepresenter(dumperOptions), dumperOptions);
	}

	private static class ValueTypeRepresenter extends Representer {

		ValueTypeRepresenter(DumperOptions options) {
			super(options);
		}

		@Override
		protected Node representScalar(Tag tag, String value) {
			if (tag.equals(Tag.INT) || tag.equals(Tag.FLOAT) || tag.equals(Tag.BOOL)
					|| tag.equals(Tag.TIMESTAMP)) {
				return super.representScalar(Tag.STR, value);
			}
			else {
				return super.representScalar(tag, value);
			}
		}
	}

}
