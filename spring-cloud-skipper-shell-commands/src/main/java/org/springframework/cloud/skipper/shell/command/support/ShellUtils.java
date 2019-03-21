/*
 * Copyright 2017 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.ApplicationArguments;

/**
 * Various utility functions for a skipper shell.
 *
 * @author Janne Valkealahti
 *
 */
public abstract class ShellUtils {

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
}
