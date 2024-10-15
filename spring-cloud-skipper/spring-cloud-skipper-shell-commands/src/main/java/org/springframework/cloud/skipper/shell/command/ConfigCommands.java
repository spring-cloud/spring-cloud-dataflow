/*
 * Copyright 2017-2020 the original author or authors.
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
package org.springframework.cloud.skipper.shell.command;

import org.springframework.cloud.skipper.client.SkipperClientProperties;
import org.springframework.cloud.skipper.client.SkipperServerException;
import org.springframework.cloud.skipper.domain.AboutResource;
import org.springframework.cloud.skipper.shell.command.support.ConsoleUserInput;
import org.springframework.cloud.skipper.shell.command.support.Target;
import org.springframework.cloud.skipper.shell.command.support.TargetHolder;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 * Configuration commands for the Shell. The default Skipper Server location is
 * <code>http://localhost:7577</code>
 *
 * @author Gunnar Hillert
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 * @author Mark Pollack
 * @author Eric Bottard
 * @author Mike Heath
 */
@ShellComponent
public class ConfigCommands extends AbstractSkipperCommand {

	private final ConsoleUserInput userInput;

	private final TargetHolder targetHolder;

	public ConfigCommands(TargetHolder targetHolder,
			ConsoleUserInput userInput) {
		this.targetHolder = targetHolder;
		this.userInput = userInput;
	}

	// @formatter:off
	@ShellMethod(key = "skipper config", value = "Configure the Spring Cloud Skipper REST server to use.")
	public String target(
			@ShellOption(help = "the location of the Spring Cloud Skipper REST endpoint", defaultValue = SkipperClientProperties.DEFAULT_TARGET)
					String uri,
			@ShellOption(help = "the username for authenticated access to the Admin REST endpoint", defaultValue = ShellOption.NULL)
					String username,
			@ShellOption(help = "the password for authenticated access to the Admin REST endpoint " +
					"(valid only with a username)", defaultValue = ShellOption.NULL)
					String password,
			@ShellOption(help = "a command to run that outputs the HTTP credentials used for authentication", defaultValue = ShellOption.NULL)
					String credentialsProviderCommand,
			@ShellOption(help = "accept any SSL certificate (even self-signed)")
					boolean skipSslValidation) throws Exception {
		// @formatter:on
		if (credentialsProviderCommand == null && password != null && username == null) {
			return "A password may be specified only together with a username";
		}

		if (credentialsProviderCommand == null &&
				password == null && username != null) {
			// read password from the command line
			password = userInput.prompt("Password", "", false);
		}

		this.targetHolder.changeTarget(new Target(uri, username, password, skipSslValidation),
				credentialsProviderCommand);

		return (this.targetHolder.getTarget().getTargetResultMessage());
	}

	@ShellMethod(key = "skipper info", value = "Show the Skipper server being used.")
	public AboutResource info() {
		Target target = targetHolder.getTarget();
		if (target.getTargetException() != null) {
			throw new SkipperServerException(this.targetHolder.getTarget().getTargetResultMessage());
		}
		return this.skipperClient.info();
	}
}
