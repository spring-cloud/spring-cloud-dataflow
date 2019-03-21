/*
 * Copyright 2013-2016 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.shell.command.common.AbstractAppRegistryCommands;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.shell.ShellException;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Knows how to build and query
 * {@link AbstractAppRegistryCommands.QualifiedApplicationName}s.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Mark Fisher
 */
@Component
public class QualifiedApplicationNameConverter implements Converter<AbstractAppRegistryCommands.QualifiedApplicationName> {

	@Autowired
	private DataFlowShell dataFlowShell;

	@Override
	public boolean supports(Class<?> type, String optionContext) {
		return AbstractAppRegistryCommands.QualifiedApplicationName.class.isAssignableFrom(type);
	}

	@Override
	public AbstractAppRegistryCommands.QualifiedApplicationName convertFromText(String value, Class<?> targetType,
			String optionContext) {
		int colonIndex = value.indexOf(':');
		if (colonIndex == -1) {
			throw new ShellException("Incorrect syntax. Valid syntax is '<ApplicationType>:<ApplicationName>'.");
		}
		ApplicationType applicationType = ApplicationType.valueOf(value.substring(0, colonIndex));
		return new AbstractAppRegistryCommands.QualifiedApplicationName(value.substring(colonIndex + 1), applicationType);
	}

	@Override
	public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData,
			String optionContext, MethodTarget target) {
		for (AppRegistrationResource app : dataFlowShell.getDataFlowOperations().appRegistryOperations().list()) {
			String value = app.getType() + ":" + app.getName();
			completions.add(new Completion(value, app.getName(), pretty(app.getType()), 0));
		}
		return true;

	}

	private String pretty(String type) {
		return StringUtils.capitalize(type) + "s";
	}
}
