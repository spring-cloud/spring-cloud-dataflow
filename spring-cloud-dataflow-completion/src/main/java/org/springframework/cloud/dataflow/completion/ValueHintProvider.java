/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.completion;

import java.util.List;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ValueHint;

/**
 * Interface to provide value hints that can be discovered for properties.
 *
 * @author Eric Bottard
 */
public interface ValueHintProvider {

	/**
	 * For a given property, return a list of {@link ValueHint} that may apply.
	 *
	 * @param property property for which to generate value hints
	 * @param classLoader class loader for the artifact/module that this property applies
	 * to; this may be used to load other classes/resources for generating value hints
	 * @return list of value hints for the provided property
	 */
	List<ValueHint> generateValueHints(ConfigurationMetadataProperty property, ClassLoader classLoader);

	/**
	 * Return {@code true} if the values returned by this provider are the only values
	 * that apply as completion proposals. If this returns {@code true}, then no other
	 * kind of completion applies until one of the returned values has been typed in full.
	 *
	 * @param property property for which to determine if the values returned by this
	 * provider are exclusive
	 * @return {@code true} if the values returned by this provider are exclusive, thus
	 * requiring one of these values to be provided before any other
	 * {@code ValueHintProvider} may be applied
	 */
	boolean isExclusive(ConfigurationMetadataProperty property);
}
