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
package org.springframework.cloud.skipper.shell.command.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import io.codearte.props2yaml.Props2YAML;
import org.yaml.snakeyaml.Yaml;

import org.springframework.cloud.skipper.SkipperException;
import org.springframework.util.StringUtils;

/**
 * Utility for converting a String of comma delimited property values to YAML. To be moved
 * into the server side for AppDeployer based manifests
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Pollack
 */
public abstract class YmlUtils {

	private static final String APPLICATION_PROPERTIES_PREFIX = "spec.applicationProperties.";

	private static final String DEPLOYMENT_PROPERTIES_PREFIX = "spec.deploymentProperties.";

	private static final String SPEC_APPLICATION_PROPERTIES_REPLACEMENT = "REPLACE_APPLICATION_PROPERTIES";

	private static final String SPEC_DEPLOYMENT_PROPERTIES_REPLACEMENT = "REPLACE_DEPLOYMENT_PROPERTIES";

	private static final String DOT_CHAR = "\\.";

	private static final String DOT_CHAR_REPLACEMENT = "-";

	public static String convertFromCsvToYaml(String propertiesAsString) {
		String stringToConvert = propertiesAsString.replaceAll(",", "\n");
		String yamlString = Props2YAML.fromContent(stringToConvert).convert();
		// validate the yaml can be parsed
		Yaml yaml = new Yaml();
		yaml.load(yamlString);
		return yamlString;
	}

	public static String getYamlConfigValues(File yamlFile, String propertiesAsCsvString) throws IOException {
		String configValuesYML = null;
		if (yamlFile != null) {
			Yaml yaml = new Yaml();
			// Validate it is yaml formatted.
			try {
				configValuesYML = yaml.dump(yaml.load(new FileInputStream(yamlFile)));
			}
			catch (FileNotFoundException e) {
				throw new SkipperException("Could not find file " + yamlFile.toString());
			}
		}
		else if (StringUtils.hasText(propertiesAsCsvString)) {
			String propertyValues = propertiesAsCsvString.replaceAll(APPLICATION_PROPERTIES_PREFIX,
					SPEC_APPLICATION_PROPERTIES_REPLACEMENT);
			propertyValues = propertyValues.replaceAll(DEPLOYMENT_PROPERTIES_PREFIX,
					SPEC_DEPLOYMENT_PROPERTIES_REPLACEMENT);
			// Replace the original property keys' dots to avoid type errors when using Props2YML
			propertyValues = propertyValues.replaceAll(DOT_CHAR, DOT_CHAR_REPLACEMENT);
			propertyValues = propertyValues.replaceAll(SPEC_APPLICATION_PROPERTIES_REPLACEMENT,
					APPLICATION_PROPERTIES_PREFIX);
			propertyValues = propertyValues.replaceAll(SPEC_DEPLOYMENT_PROPERTIES_REPLACEMENT,
					DEPLOYMENT_PROPERTIES_PREFIX);
			String ymlString = Props2YAML.fromContent(propertyValues.replaceAll(",", "\n")).convert();
			// Revert original property keys' dots
			ymlString = ymlString.replaceAll(DOT_CHAR_REPLACEMENT, DOT_CHAR);
			Yaml yaml = new Yaml();
			configValuesYML = yaml.dump(yaml.load(ymlString));
		}
		return configValuesYML;
	}
}
