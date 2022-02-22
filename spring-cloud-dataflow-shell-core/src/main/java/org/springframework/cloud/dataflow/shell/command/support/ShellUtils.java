/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command.support;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;

import org.springframework.boot.ApplicationArguments;
import org.springframework.cloud.dataflow.shell.ShellProperties;

/**
 * @author Eric Bottard
 * @author Chris Bono
 */
public class ShellUtils {

	private final static List<String> helpArgs = Arrays.asList("-h", "--h", "-help", "--help", "help", "h");

	/**
	 * Checks if given application arguments contains any usual help option.
	 *
	 * @param args the application arguments
	 * @return true if arguments contain common help option
	 */
	public static boolean hasHelpOption(ApplicationArguments args) {
		return !Collections.disjoint(helpArgs, args.getNonOptionArgs()) || !Collections.disjoint(helpArgs, args.getOptionNames());
	}

	/**
	 * Gets a filtered list of args to pass to the command shell to be executed in non-interactive mode.
	 *
	 * <p>If any {@link ShellUtils#hasHelpOption help arg} is specified returns a single element list containing 'help'.
	 * Otherwise, returns a list containing the commands in all command files specified in the
	 * {@link ShellProperties#getCommandFile()} 'spring.shell.commandFile'} property.
	 *
	 * @param shellProperties shell configuration properties
	 * @param args the application arguments
	 * @return list of raw command lines to pass to the shell for non-interactive execution where each entry is a
	 * 		string that specifies the command and options (eg. 'task create --name myFirstTask')
	 */
	public static List<String> filteredArgsToShellCommands(ShellProperties shellProperties, ApplicationArguments args) {
		if (ShellUtils.hasHelpOption(args)) {
			return Collections.singletonList("help");
		}
		if (shellProperties.getCommandFile() == null) {
			return Collections.emptyList();
		}
		return shellProperties.getCommandFile().stream()
				.map(File::new)
				.flatMap(ShellUtils::commandsInFile)
				.collect(Collectors.toList());
	}

	private static Stream<String> commandsInFile(File file) {
		try {
			return FileUtils.readLines(file, Charset.defaultCharset()).stream();
		}
		catch (IOException e) {
			throw new UncheckedIOException("Could not read commands from: " + file, e);
		}
	}

	public static String prettyPrintIfJson(String maybeJson) {
		JsonFactory factory = new JsonFactory();
		ObjectMapper mapper = new ObjectMapper(factory);
		TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
		};
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readValue(maybeJson, typeRef));
		}
		catch (IOException e) {
			// Not JSON? Return unchanged
			return maybeJson;
		}
	}
}
