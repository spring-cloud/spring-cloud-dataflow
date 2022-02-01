/*
 * Copyright 2017-2022 the original author or authors.
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

import org.jline.reader.LineReader;

import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.skipper.client.SkipperClientConfiguration;
import org.springframework.cloud.skipper.client.SkipperClientProperties;
import org.springframework.cloud.skipper.shell.command.support.ConsoleUserInput;
import org.springframework.cloud.skipper.shell.command.support.InitializeConnectionApplicationRunner;
import org.springframework.cloud.skipper.shell.command.support.ShellUtils;
import org.springframework.cloud.skipper.shell.command.support.TargetHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.boot.NonInteractiveShellRunnerCustomizer;
import org.springframework.shell.result.ThrowableResultHandler;

/**
 * Configures the various commands that are part of the default Spring Shell experience.
 *
 * @author Josh Long
 * @author Mark Pollack
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 * @author Gunnar Hillert
 * @author Chris Bono
 */
@Configuration
@Import(SkipperClientConfiguration.class)
public class ShellConfiguration {

	@Bean
	public ApplicationRunner helpAwareShellApplicationRunner() {
		return new HelpAwareShellApplicationRunner();
	}

	@Bean
	public ApplicationRunner initializeConnectionApplicationRunner(TargetHolder targetHolder, ThrowableResultHandler resultHandler,
																SkipperClientProperties skipperClientProperties) {
		return new InitializeConnectionApplicationRunner(targetHolder, resultHandler, skipperClientProperties);
	}

	@Bean
	public NonInteractiveShellRunnerCustomizer skipperClientArgsFilteringCustomizer() {
		return (shellRunner) -> shellRunner.setArgsToShellCommand(ShellUtils::filteredArgsToShellCommands);
	}

	@Bean
	public ConsoleUserInput userInput(@Lazy LineReader lineReader) {
		return new ConsoleUserInput(lineReader);
	}

}
