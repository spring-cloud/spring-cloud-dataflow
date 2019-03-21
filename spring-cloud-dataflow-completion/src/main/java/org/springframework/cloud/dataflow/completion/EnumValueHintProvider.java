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

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ValueHint;
import org.springframework.util.ClassUtils;

/**
 * A {@link ValueHintProvider} that returns possible values when the property is an
 * {@link Enum}.
 *
 * @author Eric Bottard
 */
public class EnumValueHintProvider implements ValueHintProvider {

	@Override
	public List<ValueHint> generateValueHints(ConfigurationMetadataProperty property, ClassLoader classLoader) {
		List<ValueHint> result = new ArrayList<>();
		if (ClassUtils.isPresent(property.getType(), classLoader)) {
			Class<?> clazz = ClassUtils.resolveClassName(property.getType(), classLoader);
			if (clazz.isEnum()) {
				for (Object o : clazz.getEnumConstants()) {
					ValueHint hint = new ValueHint();
					hint.setValue(o);
					result.add(hint);
				}
			}
		}
		return result;
	}

	@Override
	public boolean isExclusive(ConfigurationMetadataProperty property) {
		return true;
	}
}
