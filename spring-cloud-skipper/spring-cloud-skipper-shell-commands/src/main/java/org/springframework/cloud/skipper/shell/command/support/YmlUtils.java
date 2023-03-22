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

package org.springframework.cloud.skipper.shell.command.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.support.DeploymentPropertiesUtils;
import org.springframework.cloud.skipper.support.yaml.YamlConverter;
import org.springframework.cloud.skipper.support.yaml.YamlConverter.Mode;
import org.springframework.util.StringUtils;

/**
 * Utility for converting a String of comma delimited property values to YAML. To be moved
 * into the server side for AppDeployer based manifests
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Pollack
 * @author Chris Bono
 */
public abstract class YmlUtils {

	public static String getYamlConfigValues(File yamlFile, String properties) {
		String configValuesYML = null;
		if (yamlFile != null) {
			Yaml yaml = new Yaml(new SafeConstructor());
			// Validate it is yaml formatted.
			try {
				configValuesYML = yaml.dump(yaml.load(new FileInputStream(yamlFile)));
			}
			catch (FileNotFoundException e) {
				throw new SkipperException("Could not find file " + yamlFile.toString());
			}
		}
		else if (StringUtils.hasText(properties)) {
			configValuesYML = convertToYaml(properties);
		}
		return configValuesYML;
	}

	private static String convertToYaml(String properties) {
		return YamlConverter.builder()
				.mode(Mode.FLATTEN)
				.flat("spec.applicationProperties")
				.flat("spec.deploymentProperties")
				.flat("\\w+\\.spec\\.applicationProperties")
				.flat("\\w+\\.spec\\.deploymentProperties")
				.map(DeploymentPropertiesUtils.parse(properties)).build()
				.convert().getYaml();
	}
}
