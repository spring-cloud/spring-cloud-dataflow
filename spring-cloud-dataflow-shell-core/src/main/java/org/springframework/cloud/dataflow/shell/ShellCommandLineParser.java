/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.cloud.dataflow.shell;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.shell.CommandLine;
import org.springframework.shell.SimpleShellCommandLineOptions;


/**
 * Parses the {@link ShellProperties} and {@link ApplicationArguments} to create an
 * instance of the Spring Shell's CommandLine class.
 * <p>
 * The behavior of this class differs from the default Spring Shell
 * {@link org.springframework.shell.SimpleShellCommandLineOptions#parseCommandLine(String[])}
 * method in that additional passed in arguments are not interpreted to be commands to
 * execute and then quit. Only by passing in the options
 * <code>--spring.shell.commandFile</code> can you execute commands in the shell and then
 * quit.
 *
 * @author Mark Pollack
 * @author Furer Alexander
 */
public class ShellCommandLineParser {

	private static final Logger logger = LoggerFactory.getLogger(ShellCommandLineParser.class);

	/**
	 * Parse {@link ShellProperties} and {@link ApplicationArguments} to create an
	 * instance of the
	 *
	 * @param shellProperties the shell properties
	 * @param applicationArguments the raw unprocessed arguments that were passed to the
	 * application.
	 * @return a new {@link CommandLine} instance.
	 */
	public CommandLine parse(ShellProperties shellProperties, String[] applicationArguments) {
		List<String> commands = new ArrayList<String>();
		if (shellProperties.getCommandFile() != null) {
			for (String filePath : shellProperties.getCommandFile()) {
				File f = new File(filePath);
				try {
					commands.addAll(FileUtils.readLines(f));
				}
				catch (IOException e) {
					logger.error("Unable to read from " + f.toString(), e);
				}
			}
		}
		String[] commandsToExecute = (commands.size() > 0) ? commands.toArray(new String[commands.size()]) : null;

		int historySize = shellProperties.getHistorySize();
		if (historySize < 0) {
			logger.warn("historySize option must be > 0, using default value of "
					+ SimpleShellCommandLineOptions.DEFAULT_HISTORY_SIZE);
			historySize = SimpleShellCommandLineOptions.DEFAULT_HISTORY_SIZE;
		}

		return new CommandLine(applicationArguments, historySize, commandsToExecute);
	}
}
