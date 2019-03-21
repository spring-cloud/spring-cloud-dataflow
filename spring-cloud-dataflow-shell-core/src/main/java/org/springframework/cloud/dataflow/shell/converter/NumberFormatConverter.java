/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.converter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;

/**
 * Knows how to convert from a String to a new {@link NumberFormat} instance.
 * 
 * @author Eric Bottard
 */
@Component
public class NumberFormatConverter implements Converter<NumberFormat> {

	public static final String DEFAULT = "<use platform locale>";

	@Override
	public boolean supports(Class<?> type, String optionContext) {
		return NumberFormat.class.isAssignableFrom(type);
	}

	@Override
	public NumberFormat convertFromText(String value, Class<?> targetType, String optionContext) {
		if (DEFAULT.equals(value)) {
			return NumberFormat.getInstance();
		}
		else {
			return new DecimalFormat(value);
		}
	}

	@Override
	public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData,
			String optionContext, MethodTarget target) {
		return false;
	}

}
