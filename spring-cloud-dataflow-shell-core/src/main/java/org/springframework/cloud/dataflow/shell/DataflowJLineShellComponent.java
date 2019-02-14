/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.shell;

import org.springframework.cloud.dataflow.shell.command.ConfigCommands;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.util.StringUtils;

/**
 * Extended version of {@link JLineShellComponent} that "targets" the Spring
 * Cloud Data Flow server in {@link #getStartupNotifications()} before the shell
 * is fully initialized (Any errors are printed to the console)
 *
 * @author Gunnar Hillert
 *
 */
public class DataflowJLineShellComponent extends JLineShellComponent {

	private final ConfigCommands configCommands;
	private final TargetHolder targetHolder;

	public DataflowJLineShellComponent(ConfigCommands configCommands, TargetHolder targetHolder) {
		this.configCommands = configCommands;
		this.targetHolder = targetHolder;
	}

	@Override
	public String getStartupNotifications() {
		this.configCommands.triggerTarget();
		super.setPromptPath(null);

		final StringBuilder sb = new StringBuilder();
		if (targetHolder.getTarget().getTargetException() != null &&
			StringUtils.hasText(targetHolder.getTarget().getTargetResultMessage())) {
			sb.append("WARNING - Problem connecting to the Spring Cloud Data Flow Server:"
				+ System.lineSeparator())
			.append("\"" + targetHolder.getTarget().getTargetResultMessage() + "\""
				+ System.lineSeparator())
			.append("Please double check your startup parameters and either restart the "
				+ "Data Flow Shell (with any missing configuration including security etc.) "
				+ "or target the Data Flow Server using the "
				+ "'dataflow config server' command."
				+ System.lineSeparator()
				+ System.lineSeparator());
		}
		else if (StringUtils.hasText(targetHolder.getTarget().getTargetResultMessage())) {
			sb.append(targetHolder.getTarget().getTargetResultMessage());
		}
		return sb.toString();
	}

}
