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
package org.springframework.cloud.skipper.shell.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.skipper.shell.command.support.ShellUtils;
import org.springframework.core.annotation.Order;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * {@link ApplicationRunner} that print a help-text to the console in case
 * the help command-line option was provided and will also exit the application in that case.
 *
 * @author Gunnar Hillert
 *
 * @see org.springframework.cloud.skipper.shell.command.support.InitializeConnectionApplicationRunner
 */
@Order(InteractiveShellApplicationRunner.PRECEDENCE - 20)
public class HelpAwareShellApplicationRunner implements ApplicationRunner {

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (ShellUtils.hasHelpOption(args)) {
			String usageInstructions;

			final Reader reader = new InputStreamReader(getInputStream(HelpAwareShellApplicationRunner.class, "/usage.txt"));

			try {
				usageInstructions = FileCopyUtils.copyToString(new BufferedReader(reader));
				usageInstructions.replaceAll("(\\r|\\n)+", System.getProperty("line.separator"));
			}
			catch (Exception ex) {
				throw new IllegalStateException("Cannot read stream", ex);
			}
			System.out.println(usageInstructions);
		}
	}

	public static InputStream getInputStream(final Class<?> loadingClass, final String filename) {
		final InputStream inputStream = loadingClass.getResourceAsStream(filename);
		Assert.notNull(inputStream, "Could not locate '" + filename + "' in classpath of " + loadingClass.getName());
		return inputStream;
	}
}
