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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Default implementation of a {@link YamlConverter}.
 *
 * @author Janne Valkealahti
 *
 */
public class DefaultYamlConverter implements YamlConverter {

	private static final Pattern COMMENT = Pattern.compile("(?m)^\\s*(\\#|\\!)");
	private final YamlConversionStatus status = new YamlConversionStatus();
	private final Mode mode;
	private final ArrayList<Map<String, String>> mapList;
	private final ArrayList<Properties> propertiesList;
	private final ArrayList<File> fileList;
	private final ArrayList<String> keyspaceList;

	/**
	 * Instantiates a new default yaml converter.
	 *
	 * @param mapList the map list
	 * @param propertiesList the properties list
	 * @param fileList the file list
	 * @param keyspaceList the keyspace list
	 */
	public DefaultYamlConverter(Mode mode, ArrayList<Map<String, String>> mapList, ArrayList<Properties> propertiesList,
			ArrayList<File> fileList, ArrayList<String> keyspaceList) {
		this.mode = mode != null ? mode : Mode.DEFAULT;
		this.propertiesList = propertiesList;
		this.mapList = mapList;
		this.fileList = fileList;
		this.keyspaceList = keyspaceList;
	}

	@Override
	public YamlConversionResult convert() {
		Map<String, Collection<String>> propertiesMap = new HashMap<>();
		mapList.forEach(m -> addMap(propertiesMap, m));
		propertiesList.forEach(p -> addProperties(propertiesMap, p));
		fileList.forEach(f -> addFile(propertiesMap, f));
		return convert(propertiesMap);
	}

	private Map<String, Collection<String>> addFile(Map<String, Collection<String>> propertiesMap, File file) {
		Properties p = new Properties();
		try {
			String content = new String(Files.readAllBytes(Paths.get(file.toURI())));
			if (hasComments(content)) {
				status.addWarning("The properties file has comments, which will be lost in the refactoring!");
			}
			p.load(new StringReader(content));
		}
		catch (IOException e) {
			status.addError("Problem loading file " + file + ": " + e.getMessage());
		}
		return addProperties(propertiesMap, p);
	}

	private Map<String, Collection<String>> addProperties(Map<String, Collection<String>> propertiesMap, Properties properties) {
		for (Entry<Object, Object> e : properties.entrySet()) {
			Set<String> s = new LinkedHashSet<>();
			s.add((String) e.getValue());
			propertiesMap.put((String) e.getKey(), s);
		}
		return propertiesMap;
	}

	private Map<String, Collection<String>> addMap(Map<String, Collection<String>> propertiesMap, Map<String, String> properties) {
		for (Entry<String, String> e : properties.entrySet()) {
			Set<String> s = new LinkedHashSet<>();
			s.add((String) e.getValue());
			propertiesMap.put((String) e.getKey(), s);
		}
		return propertiesMap;
	}

	private YamlConversionResult convert(Map<String, Collection<String>> properties) {
		if (properties.isEmpty()) {
			return YamlConversionResult.EMPTY;
		}

		YamlBuilder root = new YamlBuilder(mode, keyspaceList, status, YamlPath.EMPTY);
		for (Entry<String, Collection<String>> e : properties.entrySet()) {
			for (String v : e.getValue()) {
				root.addProperty(YamlPath.fromProperty(e.getKey()), v);
			}
		}

		Object object = root.build();

		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);

		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(options), options);
		String output = yaml.dump(object);
		return new YamlConversionResult(status, output);
	}

	private boolean hasComments(String line) {
		return COMMENT.matcher(line).find();
	}

	/**
	 * Default implementation of a {@link org.springframework.cloud.skipper.support.yaml.YamlConverter.Builder} building a {@link DefaultYamlConverter}.
	 */
	public static class DefaultBuilder implements Builder {

		private Mode mode;
		private ArrayList<Properties> propertiesList = new ArrayList<>();
		private ArrayList<Map<String, String>> mapList = new ArrayList<>();
		private ArrayList<File> fileList = new ArrayList<>();
		private ArrayList<String> keyspaceList = new ArrayList<>();

		@Override
		public Builder mode(Mode mode) {
			this.mode = mode;
			return this;
		}

		@Override
		public Builder flat(String keyspace) {
			this.keyspaceList.add(keyspace);
			return this;
		}

		@Override
		public Builder file(File file) {
			this.fileList.add(file);
			return this;
		}

		@Override
		public Builder properties(Properties properties) {
			this.propertiesList.add(properties);
			return this;
		}

		@Override
		public Builder map(Map<String, String> properties) {
			this.mapList.add(properties);
			return this;
		}

		@Override
		public YamlConverter build() {
			return new DefaultYamlConverter(mode, mapList, propertiesList, fileList, keyspaceList);
		}
	}
}
