/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.dataflow.shell.command.support;

import io.codearte.props2yaml.Props2YAML;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Utility for converting a String of comma delimited property values to YAML.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Pollack
 */
public abstract class YmlUtils {

	public static String convertFromCsvToYaml(String propertiesAsString) {
		String stringToConvert = propertiesAsString.replaceAll(",", "\n");
		String yamlString = Props2YAML.fromContent(stringToConvert).convert();
		// validate the yaml can be parsed
		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
		yaml.load(yamlString);
		return yamlString;
	}
}
