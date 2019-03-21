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
import java.util.Map;
import java.util.Properties;

/**
 * Interface able to convert a various sources into {@code YAML}.
 *
 * @author Janne Valkealahti
 *
 */
public interface YamlConverter {

	/**
	 * Gets a default {@link Builder} building a configured {@link YamlConverter}.
	 *
	 * @return the builder
	 */
	static Builder builder() {
		return new DefaultYamlConverter.DefaultBuilder();
	}

	/**
	 * Convert configured sources as a {@link YamlConversionResult}.
	 *
	 * @return the yaml conversion result
	 */
	YamlConversionResult convert();

	/**
	 * Enumeration of a converter mode.
	 */
	enum Mode {

		/** Default strict mode using normal yaml format without any compatibility tricks */
		DEFAULT,

		/** Flatten properties if parent has a scalar and sub-keys */
		FLATTEN;
	}

	/**
	 * Interface for building a {@code YamlConverter}.
	 */
	interface Builder {

		/**
		 * Sets the used {@link Mode} for a converter. Defaults
		 * to {@link Mode#DEFAULT}.
		 *
		 * @param mode the mode
		 * @return the builder
		 */
		Builder mode(Mode mode);

		/**
		 * Adds a keyspace regex pattern which forces flattening.
		 * This pattern needs to fully match target keyspace.
		 *
		 * @param keyspace the keyspace
		 * @return the builder for chaining
		 */
		Builder flat(String keyspace);

		/**
		 * Adds a {@link File} containing properties.
		 *
		 * @param file the file
		 * @return the builder for chaining
		 */
		Builder file(File file);

		/**
		 * Adds a plain {@link Properties}.
		 *
		 * @param properties the properties
		 * @return the builder for chaining
		 */
		Builder properties(Properties properties);

		/**
		 * Adds a {@link Map} of properties.
		 *
		 * @param properties the properties
		 * @return the builder for chaining
		 */
		Builder map(Map<String, String> properties);

		/**
		 * Builds the configured {@code YamlConverter}.
		 *
		 * @return the built yaml converter
		 */
		YamlConverter build();
	}
}
