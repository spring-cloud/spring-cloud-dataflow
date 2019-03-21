/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.cloud.skipper.shell.command.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.util.StringUtils;

/**
 * An {@link ApplicationRunner} implementation that determines if the shell should execute
 * in interactive mode or in a 'stand alone executable' mode. If any arguments are passed
 * in that do not have the prefix {@code spring.cloud.skipper.client}, 'stand alone
 * executeable' model will be activated and the arguments will be passed into the shell as
 * a command to execute and then exit.
 *
 * This {@link ApplicationRunner} has lower priority than
 * {@link InitializeConnectionApplicationRunner}.
 *
 * @see org.springframework.cloud.skipper.client.SkipperClientProperties
 *
 * @author Janne Valkealahti
 *
 */
@Order(InteractiveShellApplicationRunner.PRECEDENCE - 5)
public class InteractiveModeApplicationRunner implements ApplicationRunner {

	private final Shell shell;
	private final ConfigurableEnvironment environment;

	/**
	 * Construct a new InteractiveModeApplicationRunner, given the required shell components.
	 *
	 * @param shell the shell
	 * @param environment the environment
	 */
	public InteractiveModeApplicationRunner(Shell shell, ConfigurableEnvironment environment) {
		this.shell = shell;
		this.environment = environment;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// if we have args, assume one time run
		ArrayList<String> argsToShellCommand = new ArrayList<>();
		for (String arg : args.getSourceArgs()) {
			// consider client connection options as non command args
			if (!arg.contains("spring.cloud.skipper.client")) {
				argsToShellCommand.add(arg);
			}
		}
		if (argsToShellCommand.size() > 0) {
			if (ShellUtils.hasHelpOption(args)) {
				// going into 'help' mode. HelpAwareShellApplicationRunner already
				// printed usage, we just force running just help.
				argsToShellCommand.clear();
				argsToShellCommand.add("help");
			}
		}

		if (!argsToShellCommand.isEmpty()) {
			InteractiveShellApplicationRunner.disable(environment);
			shell.run(new StringInputProvider(argsToShellCommand));
		}
	}

	/**
	 * {@link InputProvider} which gives a single input to shell.
	 */
	private class StringInputProvider implements InputProvider {

		private final List<String> commands;

		private boolean done;

		StringInputProvider(List<String> words) {
			this.commands = words;
		}

		@Override
		public Input readInput() {
			if (!done) {
				done = true;
				return new Input() {
					@Override
					public List<String> words() {
						return commands;
					}

					@Override
					public String rawText() {
						return StringUtils.collectionToDelimitedString(commands, " ");
					}
				};
			}
			else {
				return null;
			}
		}
	}
}
