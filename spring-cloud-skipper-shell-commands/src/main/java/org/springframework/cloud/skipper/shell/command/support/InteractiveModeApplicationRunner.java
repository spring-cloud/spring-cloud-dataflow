/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.shell.command.support;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jline.reader.LineReader;
import org.jline.reader.Parser;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.DefaultShellApplicationRunner;
import org.springframework.shell.jline.DefaultShellApplicationRunner.JLineInputProvider;
import org.springframework.shell.jline.FileInputProvider;
import org.springframework.shell.jline.PromptProvider;
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
@Order(DefaultShellApplicationRunner.PRECEDENCE - 5)
public class InteractiveModeApplicationRunner implements ApplicationRunner {

	private final LineReader lineReader;

	private final PromptProvider promptProvider;

	private final Parser parser;

	private final Shell shell;

	/**
	 * Construct a new InteractiveModeApplicationRunner, given the required shell components.
	 *
	 * @param lineReader the line reader
	 * @param promptProvider the prompt provider
	 * @param parser the parser
	 * @param shell the shell
	 */
	public InteractiveModeApplicationRunner(LineReader lineReader, PromptProvider promptProvider, Parser parser,
			Shell shell) {
		this.lineReader = lineReader;
		this.promptProvider = promptProvider;
		this.parser = parser;
		this.shell = shell;
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
			CommandInputProvider inputProvider = new CommandInputProvider(
					StringUtils.collectionToDelimitedString(argsToShellCommand, " "));
			shell.run(inputProvider);
			return;
		}

		// otherwise, fallback to what DefaultShellApplicationRunner would do
		List<File> scriptsToRun = args.getNonOptionArgs().stream()
				.filter(s -> s.startsWith("@"))
				.map(s -> new File(s.substring(1)))
				.collect(Collectors.toList());
		if (scriptsToRun.isEmpty()) {
			InputProvider inputProvider = new JLineInputProvider(lineReader, promptProvider);
			shell.run(inputProvider);
		}
		else {
			for (File file : scriptsToRun) {
				try (Reader reader = new FileReader(file);
						FileInputProvider inputProvider = new FileInputProvider(reader, parser)) {
					shell.run(inputProvider);
				}
			}
		}
	}

	/**
	 * {@link InputProvider} which gives a single input to shell.
	 */
	private static class CommandInputProvider implements InputProvider {

		private String command;

		private boolean done;

		CommandInputProvider(String command) {
			this.command = command;
		}

		@Override
		public Input readInput() {
			// we can only give single input as otherwise
			// shell will start to loop command. thus use a simple
			// flag to return null next time.
			if (!done) {
				done = true;
				return () -> command;
			}
			return null;
		}
	}
}
