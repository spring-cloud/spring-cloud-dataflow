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

/**
 * {@code YamlConversionResult} is a result returned by {@link YamlConverter}
 * and contains actual {@code YAML} and conversion status.
 *
 * @author Kris De Volder
 * @author Janne Valkealahti
 *
 */
public class YamlConversionResult {

	static YamlConversionResult EMPTY = new YamlConversionResult(YamlConversionStatus.EMPTY, "");
	private YamlConversionStatus status;
	private String yaml;

	/**
	 * Instantiates a new yaml conversion result.
	 *
	 * @param status the status
	 * @param output the output
	 */
	YamlConversionResult(YamlConversionStatus status, String output) {
		this.status = status;
		this.yaml = output;
	}

	/**
	 * Gets the yaml.
	 *
	 * @return the yaml
	 */
	public String getYaml() {
		return yaml;
	}

	/**
	 * Gets the status.
	 *
	 * @return the status
	 */
	public YamlConversionStatus getStatus() {
		return status;
	}
}
