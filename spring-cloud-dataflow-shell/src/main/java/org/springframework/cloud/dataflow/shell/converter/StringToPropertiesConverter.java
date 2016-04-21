/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.converter;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Properties;

import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;

/**
 * Convert method functionality copied from
 * {@code org.springframework.core.convert.support.StringToPropertiesConverter}.
 *
 * @author Keith Donald
 * @author Mark Fisher
 */
@Component
public class StringToPropertiesConverter implements Converter<Properties> {

	@Override
	public boolean supports(Class<?> type, String optionContext) {
		return Properties.class.isAssignableFrom(type);
	}

	@Override
	public Properties convertFromText(String value, Class<?> targetType, String optionContext) {
		try {
			Properties props = new Properties();
			// Must use the ISO-8859-1 encoding because Properties.load(stream) expects it.
			props.load(new ByteArrayInputStream(value.getBytes("ISO-8859-1")));
			return props;
		}
		catch (Exception ex) {
			// Should never happen.
			throw new IllegalArgumentException("Failed to parse [" + value + "] into Properties", ex);
		}
	}

	@Override
	public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData,
			String optionContext, MethodTarget target) {
		return false;
	}
}
