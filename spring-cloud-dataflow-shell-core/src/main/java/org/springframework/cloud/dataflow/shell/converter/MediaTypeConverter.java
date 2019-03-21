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

package org.springframework.cloud.dataflow.shell.converter;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;

/**
 * Knows how to parse String representations of {@link MediaType}.
 *
 * @author Eric Bottard
 */
@Component
public class MediaTypeConverter implements Converter<MediaType> {

	@Override
	public boolean supports(Class<?> type, String optionContext) {
		return MediaType.class.isAssignableFrom(type);
	}

	@Override
	public MediaType convertFromText(String value, Class<?> targetType, String optionContext) {
		return MediaType.parseMediaType(value);
	}

	@Override
	public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData,
			String optionContext, MethodTarget target) {
		return false;
	}

}