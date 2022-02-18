/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.cloud.dataflow.shell.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.dataflow.shell.TargetHolder;
import org.springframework.cloud.dataflow.shell.command.ConfigCommands;
import org.springframework.cloud.dataflow.shell.command.support.ShellUtils;
import org.springframework.core.annotation.Order;
import org.springframework.shell.DefaultShellApplicationRunner;
import org.springframework.shell.result.ThrowableResultHandler;
import org.springframework.util.StringUtils;

/**
 * An {@link ApplicationRunner} implementation that initialises the connection to the Data Flow Server.
 *
 * <p>Has higher precedence than {@link DefaultShellApplicationRunner} so that it runs before any shell runner.
 *
 * @author Chris Bono
 * @since 2.10
 */
@Order(DefaultShellApplicationRunner.PRECEDENCE - 10)
public class InitializeConnectionApplicationRunner implements ApplicationRunner {

	private final Logger logger = LoggerFactory.getLogger(InitializeConnectionApplicationRunner.class);

	private TargetHolder targetHolder;

	private ConfigCommands configCommands;

	private ThrowableResultHandler resultHandler;

	/**
	 * Construct a new InitializeConnectionApplicationRunner instance.
	 * @param targetHolder holds the target server connection info
	 * @param configCommands commands to configure the server
	 * @param resultHandler handles exceptions form the connection attempt
	 */
	public InitializeConnectionApplicationRunner(TargetHolder targetHolder,
			ConfigCommands configCommands,
			ThrowableResultHandler resultHandler) {
		this.targetHolder = targetHolder;
		this.configCommands = configCommands;
		this.resultHandler = resultHandler;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// user just wants to print help - don't attempt connection - pass onto HelpAwareShellApplicationRunner
		if (ShellUtils.hasHelpOption(args)) {
			return;
		}

		// Attempt connection (including against default values) but do not crash the shell on error
		try {
			this.configCommands.triggerTarget();
			logResultMessage(this.targetHolder.getTarget().getTargetResultMessage());
		}
		catch (Exception e) {
			if (this.targetHolder.getTarget().getTargetException() != null &&
					StringUtils.hasText(this.targetHolder.getTarget().getTargetResultMessage())) {
				logResultMessage(String.format(
						"WARNING - Problem connecting to the Spring Cloud Data Flow Server:%n\"%s\"%n"
						+ "Please double check your startup parameters and either restart the "
						+ "Data Flow Shell (with any missing configuration including security etc.) "
						+ "or target the Data Flow Server using the 'dataflow config server' command.%n%n",
						this.targetHolder.getTarget().getTargetResultMessage()));
			}
			this.resultHandler.handleResult(e);
		}
	}

	private void logResultMessage(String message) {
		if (StringUtils.hasText(message)) {
			logger.info(message);
		}
	}
}
