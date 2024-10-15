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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility methods for merging of configuration values.
 *
 * @author Mark Pollack
 * @author Chris Bono
 */
@SuppressWarnings("unchecked")
public class ConfigValueUtils {

	/**
	 * Merge configuration values from the Package and values passed at runtime. Values passed
	 * at runtime override those specified in the package. The ConfigValue string is assumed
	 * to be in YAML format and parsed into a Map.
	 * <p>
	 * The values for dependencies of packages are also merged, which higher level packages
	 * overriding lower level packages. Only one level of dependent packages are currently
	 * supported.
	 * <p>
	 * @param pkg The package to be installed or upgraded.
	 * @param overrideValues Configuration values pass in at runtime, when installing or
	 * updating.
	 * @return The merged configuration values.
	 */
	public static Map<String, Object> mergeConfigValues(Package pkg, ConfigValues overrideValues) {
		// parse ConfigValues to a map.
		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
		Map<String, Object> mergedValues;
		// merge top level override values on top level package values
		if (StringUtils.hasText(overrideValues.getRaw())) {
			Object data = yaml.load(overrideValues.getRaw());
			if (data instanceof Map) {
				Map<String, Object> overrideMap = (Map<String, Object>) yaml.load(overrideValues.getRaw());
				mergedValues = mergeOverrideMap(pkg, overrideMap);
			}
			else {
				throw new SkipperException("Was expecting override values to produce a Map, instead got class = " +
						data.getClass() + "overrideValues.getRaw() = " + overrideValues.getRaw());
			}
		}
		else {
			mergedValues = mergeOverrideMap(pkg, new TreeMap<>());
		}
		// return mergedValues;
		return mergePackagesIncludingDependencies(pkg, mergedValues);
	}

	/**
	 * Merge top level configuration values from the Package and the configuration values
	 * passed in at runtime.
	 * <p>
	 * @param pkg The package being configured
	 * @param overrideMap The runtime override values
	 * @return A merged map of package configuration values and runtime override values.
	 */
	public static Map<String, Object> mergeOverrideMap(Package pkg, Map<String, Object> overrideMap) {
		// if the package does not have any values, return just the override values.
		if (pkg.getConfigValues() == null || (pkg.getConfigValues() != null &&
				!StringUtils.hasText(pkg.getConfigValues().getRaw()))) {
			return overrideMap;
		}
		// load the package values
		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
		Object object = yaml.load(pkg.getConfigValues().getRaw());
		if (object == null) {
			// Config Values could have been file with comments only, no data.
			return overrideMap;
		}
		Map<String, Object> packageValueMap;
		if (object instanceof Map) {
			packageValueMap = (Map<String, Object>) object;
		}
		else {
			throw new SkipperException("Config Values that are not a map are not yet supported.");
		}

		// exclude dependency values from being merged into the current packages' values
		if (packageValueMap != null) {
			for (Package dependency : pkg.getDependencies()) {
				if (packageValueMap.containsKey(dependency.getMetadata().getName())) {
					packageValueMap.remove(dependency.getMetadata().getName());
				}
			}
		}
		else {
			packageValueMap = new TreeMap<>();
		}

		merge(packageValueMap, overrideMap);

		return packageValueMap;
	}

	private static Map<String, Object> mergePackagesIncludingDependencies(Package pkg,
			Map<String, Object> overrideMap) {
		List<Package> dependencies = pkg.getDependencies();
		if (dependencies.size() == 0) {
			return overrideMap;
		}
		Map<String, Object> mergedValues = new TreeMap<>();

		// parse ConfigValues to a map.
		Map<String, Object> currentPackageValueMap = convertConfigValuesToMap(pkg);

		// Merge top level value properties
		for (Map.Entry<String, Object> stringObjectEntry : overrideMap.entrySet()) {
			if (!(stringObjectEntry.getValue() instanceof Map)) {
				if (currentPackageValueMap.containsKey(stringObjectEntry.getKey())) {
					mergedValues.put(stringObjectEntry.getKey(), stringObjectEntry.getValue());
				}
			}
		}
		for (Package dependency : dependencies) {
			String dependencyName = dependency.getMetadata().getName();
			Map<String, Object> currentPackageValueMapForDependency = (Map<String, Object>) currentPackageValueMap
					.getOrDefault(dependencyName, new TreeMap<>());

			// If the override Map contains configuration for dependencies, unravel the override Map,
			// otherwise ignore
			Map<String, Object> overrideMapToUse;
			if (overrideMap.containsKey(dependencyName)) {
				overrideMapToUse = (Map<String, Object>) overrideMap.get(dependencyName);
			}
			else {
				overrideMapToUse = new TreeMap<>();
			}
			merge(currentPackageValueMapForDependency, overrideMapToUse);

			mergedValues.put(dependency.getMetadata().getName(),
					mergeOverrideMap(dependency, currentPackageValueMapForDependency));
		}
		return mergedValues;

	}

	private static Map<String, Object> convertConfigValuesToMap(Package pkg) {
		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
		Map<String, Object> currentPackageValueMap = new TreeMap<>();
		if (pkg.getConfigValues() != null && StringUtils.hasText(pkg.getConfigValues().getRaw())) {
			currentPackageValueMap = (Map<String, Object>) yaml.load(pkg.getConfigValues().getRaw());
		}
		if (currentPackageValueMap == null) {
			currentPackageValueMap = new TreeMap<>();
		}
		return currentPackageValueMap;
	}

	/**
	 * Nested merge of maps where the values from map2 override those from map1.
	 * @param map1 The base map to merge values onto
	 * @param map2 The map of values to override those of the first map argument.
	 */
	public static void merge(Map<String, Object> map1, Map<String, Object> map2) {
		Assert.notNull(map1, "Base map to merge value onto must not be null.");
		Assert.notNull(map2, "Map with values to override must not be null.");
		for (String key : map2.keySet()) {
			Object value2 = map2.get(key);
			if (map1.containsKey(key)) {
				Object value1 = map1.get(key);
				if (value1 instanceof Map && value2 instanceof Map) {
					merge((Map<String, Object>) value1, (Map<String, Object>) value2);
				}
				else if (value1 instanceof List<?> list && value2 instanceof List<?> list1) {
					map1.put(key, mergeList(list, list1));
				}
				else {
					map1.put(key, value2);
				}
			}
			else {
				map1.put(key, value2);
			}
		}
	}

	/**
	 * Merge two lists, the contents of the second list override those in the first.
	 * @param list1 The base list to merge values onto.
	 * @param list2 The list of value that override those of the first list argument.
	 * @return the merged list
	 */
	public static List mergeList(List list1, List list2) {
		list2.removeAll(list1);
		list1.addAll(list2);
		return list1;
	}

}
