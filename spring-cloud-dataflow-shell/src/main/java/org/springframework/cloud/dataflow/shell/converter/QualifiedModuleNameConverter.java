/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.shell.converter;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.ArtifactType;
import org.springframework.cloud.dataflow.rest.resource.ModuleRegistrationResource;
import org.springframework.cloud.dataflow.shell.command.ModuleCommands;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.shell.ShellException;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.shell.core.SimpleParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


/**
* Knows how to build and query {@link org.springframework.cloud.dataflow.shell.command.ModuleCommands.QualifiedModuleName}s.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 */
@Component
public class QualifiedModuleNameConverter implements Converter<ModuleCommands.QualifiedModuleName> {

	private static final Logger logger = LoggerFactory.getLogger(SimpleParser.class);

	@Autowired
	private DataFlowShell dataFlowShell;

	@Override
	public boolean supports(Class<?> type, String optionContext) {
		return ModuleCommands.QualifiedModuleName.class.isAssignableFrom(type);
	}

	@Override
	public ModuleCommands.QualifiedModuleName convertFromText(String value, Class<?> targetType, String optionContext) {
		int colonIndex = value.indexOf(':');
		if (colonIndex == -1) {
			logger.warn("Incorrect syntax. Valid syntax is '<ModuleType>:<ModuleName>'.");
			logger.warn("For example, 'module info source:file' to get 'file' <source> module info.");
			throw new ShellException();
		}
		ArtifactType moduleType = validateAndReturnModuleType(value.substring(0, colonIndex));
		return new ModuleCommands.QualifiedModuleName(value.substring(colonIndex + 1), moduleType);
	}

	/**
	 * Verify the module type used in the option value and return the valid artifact type.
	 * @param value the value to validate
	 * @return ArtifactType the valid artifact type
	 */
	private ArtifactType validateAndReturnModuleType(String value) {
		try {
			return ArtifactType.valueOf(value);
		}
		catch (IllegalArgumentException e) {
			logger.warn("Not a valid module type. Valid module types are: "
					+ Arrays.toString(ArtifactType.values()));
			throw new ShellException();
		}
	}

	@Override
	public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData,
			String optionContext, MethodTarget target) {
		for (ModuleRegistrationResource m : dataFlowShell.getDataFlowOperations().moduleOperations().list(null)) {
			String value = m.getType() + ":" + m.getName();
			completions.add(new Completion(value, m.getName(), pretty(m.getType()), 0));
		}
		return true;

	}

	private String pretty(String type) {
		return StringUtils.capitalize(type) + "s";
	}
}
