/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command.classic;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.shell.command.common.AbstractStreamCommands;
import org.springframework.cloud.dataflow.shell.command.common.Assertions;
import org.springframework.cloud.dataflow.shell.command.common.UserInput;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

/**
 * Stream commands.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author Christian Tzolov
 * @author Janne Valkealahti
 */
@Component
// todo: reenable optionContext attributes
public class ClassicStreamCommands extends AbstractStreamCommands implements CommandMarker {

	private static final String DEPLOY_STREAM = "stream deploy";

	@Autowired
	public void setDataFlowShell(DataFlowShell dataFlowShell) {
		this.dataFlowShell = dataFlowShell;
	}

	@Autowired
	public void setUserInput(UserInput userInput) {
		this.userInput = userInput;
	}

	@CliAvailabilityIndicator({ DEPLOY_STREAM })
	public boolean availableWithCreateRole() {
		return dataFlowShell.hasAccess(RoleType.CREATE, OpsType.STREAM);
	}

	@CliCommand(value = DEPLOY_STREAM, help = "Deploy a previously created stream")
	public String deployStream(
			@CliOption(key = { "",
					"name" }, help = "the name of the stream to deploy", mandatory = true, optionContext = "existing-stream disable-string-converter") String name,
			@CliOption(key = {
					PROPERTIES_OPTION }, help = "the properties for this deployment") String deploymentProperties,
			@CliOption(key = {
					PROPERTIES_FILE_OPTION }, help = "the properties for this deployment (as a File)") File propertiesFile)
			throws IOException {
		int which = Assertions.atMostOneOf(PROPERTIES_OPTION, deploymentProperties, PROPERTIES_FILE_OPTION,
				propertiesFile);
		Map<String, String> propertiesToUse = DeploymentPropertiesUtils.parseDeploymentProperties(deploymentProperties,
				propertiesFile, which);
		streamOperations().deploy(name, propertiesToUse);
		return String.format("Deployment request has been sent for stream '%s'\n", name);
	}
}
