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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.dataflow.shell.command.support.ShellUtils;
import org.springframework.core.annotation.Order;
import org.springframework.shell.DefaultShellApplicationRunner;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * {@link ApplicationRunner} that extends the basic help command to include a skipper specific help-text at the top
 * of the normal help command output.
 *
 * <p>Has higher precedence than {@link InitializeConnectionApplicationRunner} so that it runs before that or any
 * shell runner.
 *
 * @author Chris Bono
 * @since 2.10
 *
 * @see InitializeConnectionApplicationRunner
 */
@Order(DefaultShellApplicationRunner.PRECEDENCE - 20)
public class HelpApplicationRunner implements ApplicationRunner {

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (ShellUtils.hasHelpOption(args)) {
			final Reader reader = new InputStreamReader(getInputStream(HelpApplicationRunner.class, "/usage.txt"));
			String usageInstructions;
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

	private InputStream getInputStream(final Class<?> loadingClass, final String filename) {
		final InputStream inputStream = loadingClass.getResourceAsStream(filename);
		Assert.notNull(inputStream, "Could not locate '" + filename + "' in classpath of " + loadingClass.getName());
		return inputStream;
	}
}
