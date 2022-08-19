/*
 * Copyright 2013-2022 the original author or authors.
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

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.shell.command.AppRegistryCommands;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Knows how to parse String representations of {@link AppRegistryCommands.QualifiedApplicationName}s.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Chris Bono
 */
@Component
public class QualifiedApplicationNameConverter implements Converter<String, AppRegistryCommands.QualifiedApplicationName> {

	@Override
	public AppRegistryCommands.QualifiedApplicationName convert(String value) {
		int colonIndex = value.indexOf(':');
		if (colonIndex == -1) {
			throw new IllegalArgumentException("Incorrect syntax. Valid syntax is '<ApplicationType>:<ApplicationName>'.");
		}
		ApplicationType applicationType = ApplicationType.valueOf(value.substring(0, colonIndex));
		return new AppRegistryCommands.QualifiedApplicationName(value.substring(colonIndex + 1), applicationType);
	}
}
