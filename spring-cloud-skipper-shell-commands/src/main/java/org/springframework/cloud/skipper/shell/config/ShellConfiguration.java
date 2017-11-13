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
package org.springframework.cloud.skipper.shell.config;

import org.jline.reader.LineReader;
import org.jline.reader.Parser;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.skipper.client.SkipperClientConfiguration;
import org.springframework.cloud.skipper.shell.command.support.ConsoleUserInput;
import org.springframework.cloud.skipper.shell.command.support.InitializeConnectionApplicationRunner;
import org.springframework.cloud.skipper.shell.command.support.InteractiveModeApplicationRunner;
import org.springframework.cloud.skipper.shell.command.support.TargetHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.ResultHandler;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.PromptProvider;

/**
 * Configures the various commands that are part of the default Spring Shell experience.
 *
 * @author Josh Long
 * @author Mark Pollack
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 * @author Gunnar Hillert
 */
@Configuration
@Import(SkipperClientConfiguration.class)
public class ShellConfiguration {

	@Bean
	public ApplicationRunner helpAwareShellApplicationRunner() {
		return new HelpAwareShellApplicationRunner();
	}

	@Bean
	public ApplicationRunner initialConnectionApplicationRunner() {
		return new InitialConnectionApplicationRunner();
	}

	@Bean
	public ApplicationRunner initialConnectionApplicationRunner(TargetHolder targetHolder,
																@Qualifier("main") ResultHandler resultHandler,
																SkipperClientProperties skipperClientProperties) {
		return new InitializeConnectionApplicationRunner(targetHolder, resultHandler, skipperClientProperties);
	}

	@Bean
	public ApplicationRunner applicationRunner(LineReader lineReader, PromptProvider promptProvider, Parser parser,
			Shell shell) {
		return new InteractiveModeApplicationRunner(lineReader, promptProvider, parser, shell);
	}

	@Bean
	public ConsoleUserInput userInput(@Lazy LineReader lineReader) {
		return new ConsoleUserInput(lineReader);
	}

}
