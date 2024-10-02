/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.skipper.support.yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.cloud.skipper.support.yaml.YamlConverter.Mode;
import org.springframework.cloud.skipper.support.yaml.YamlPathSegment.AtIndex;

/**
 * {@code YamlBuilder} acts as a recursive builder handler for properties.
 *
 * @author Kris De Volder
 * @author Janne Valkealahti
 *
 */
class YamlBuilder {

	private final Mode mode;
	private final ArrayList<String> keyspaceList;
	private final YamlConversionStatus status;
	private final YamlPath path;
	private final List<String> scalars = new ArrayList<>();
	private final TreeMap<Integer, YamlBuilder> listItems = new TreeMap<>();
	private final TreeMap<String, YamlBuilder> mapEntries = new TreeMap<>();

	/**
	 * Instantiates a new yaml builder.
	 *
	 * @param mode the mode
	 * @param keyspaceList the keyspaces
	 * @param status the status
	 * @param path the path
	 */
	YamlBuilder(Mode mode, ArrayList<String> keyspaceList, YamlConversionStatus status, YamlPath path) {
		this.mode = mode;
		this.keyspaceList = keyspaceList;
		this.status = status;
		this.path = path;
	}

	/**
	 * Adds a property to this builder identified by its path and value.
	 *
	 * @param path the path
	 * @param value the value
	 */
	public void addProperty(YamlPath path, String value) {
		if (path.isEmpty()) {
			scalars.add(value);
		}
		else {
			YamlPathSegment segment = path.getSegment(0);
			YamlBuilder subBuilder;
			if (segment instanceof AtIndex) {
				subBuilder = getSubBuilder(listItems, segment, segment.toIndex());
			}
			else {
				subBuilder = getSubBuilder(mapEntries, segment, segment.toPropString());
			}
			subBuilder.addProperty(path.dropFirst(1), value);
		}
	}

	/**
	 * Builds the object from this builder.
	 *
	 * @return the object
	 */
	public Object build() {
		String propString = this.path.toPropString();
		boolean force = keyspaceList.stream().anyMatch((s) -> {
			// check that prop starts with regex pattern and
			// need to skip full match
			Matcher m = Pattern.compile(s).matcher(propString);
			return m.lookingAt() && !m.matches();
		});

		Flatten flatten = null;
		if (!scalars.isEmpty()) {
			if (listItems.isEmpty() && mapEntries.isEmpty()) {
				if (scalars.size() > 1) {
					status.addWarning("Multiple values " + scalars + " assigned to '" + path.toPropString()
							+ "'. Values will be merged into a yaml sequence node.");
					return scalars;
				}
				else {
					return scalars.get(0);
				}
			}
			else {
				if (mode == Mode.FLATTEN) {
					flatten = new Flatten();
					flatten.list = scalars;
				}
				else {
					if (!mapEntries.isEmpty()) {
						status.addError("Direct assignment '" + path.toPropString() + "=" + scalars.get(0)
								+ "' can not be combined " + "with sub-property assignment '" + path.toPropString()
								+ "." + mapEntries.keySet().iterator().next() + "...'. "
								+ "Direct assignment will be dropped!");
					}
					else {
						status.addError("Direct assignment '" + path.toPropString() + "=" + scalars.get(0)
								+ "' can not be combined " + "with sequence assignment '" + path.toPropString() + "["
								+ listItems.keySet().iterator().next() + "]...' "
								+ "Direct assignments will be dropped!");
					}
					scalars.clear();
				}
			}
		}
		if (!listItems.isEmpty() && !mapEntries.isEmpty()) {
			status.addWarning("'" + path.toPropString()
					+ "' has some entries that look like list items and others that look like map entries. "
					+ "All these entries will be treated as map entries");
			for (Entry<Integer, YamlBuilder> listItem : listItems.entrySet()) {
				mapEntries.put(listItem.getKey().toString(), listItem.getValue());
			}
			listItems.clear();
		}
		if (!listItems.isEmpty()) {
			return listItems.values().stream().map(childBuilder -> childBuilder.build())
					.collect(Collectors.toList());
		}
		else {
			TreeMap<String, Object> map = new TreeMap<>();
			if (force) {
				flatten = new Flatten();
			}
			for (Entry<String, YamlBuilder> entry : mapEntries.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue().build();

				if (mode == Mode.FLATTEN && value instanceof Flatten f) {
					if (f.list != null && f.list.size() == 1) {
						map.put(key, f.list.get(0));
					}
					else {
						if (f.list != null) {
							map.put(key, f.list);
						}
					}
					for (Entry<String, Object> e : f.map.entrySet()) {
						if (e.getValue() != null) {
							map.put(key + "." + e.getKey(), e.getValue());
						}
					}
				}
				else {
					map.put(key, value);
				}
			}
			if (flatten != null) {
				flatten.map = map;
				return flatten;
			}
			else {
				return map;
			}
		}
	}

	private <T> YamlBuilder getSubBuilder(TreeMap<T, YamlBuilder> subBuilders, YamlPathSegment segment, T key) {
		YamlBuilder existing = subBuilders.get(key);
		if (existing == null) {
			existing = new YamlBuilder(mode, keyspaceList, status, path.append(segment));
			subBuilders.put(key, existing);
		}
		return existing;
	}

	private static class Flatten {
		List<String> list;
		Map<String, Object> map;
	}
}
