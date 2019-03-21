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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ValueHint;

/**
 * Returns the closed set of {@literal true} and {@literal false} for properties
 * that are of type Boolean.
 *
 * @author Eric Bottard
 */
public class BooleanValueHintProvider implements ValueHintProvider {

	private static final List<ValueHint> BOOLEANS;
	static {
		ValueHint yes = new ValueHint();
		yes.setValue(true);
		ValueHint no = new ValueHint();
		no.setValue(false);
		BOOLEANS = Collections.unmodifiableList(Arrays.asList(yes, no));
	}

	@Override
	public List<ValueHint> generateValueHints(ConfigurationMetadataProperty property, ClassLoader classLoader) {
		return "java.lang.Boolean".equals(property.getType())
				? BOOLEANS
				: Collections.<ValueHint>emptyList();
	}

	@Override
	public boolean isExclusive(ConfigurationMetadataProperty property) {
		return true;
	}
}
