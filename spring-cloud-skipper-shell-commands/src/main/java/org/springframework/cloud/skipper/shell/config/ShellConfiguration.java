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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.skipper.client.SkipperClientConfiguration;
import org.springframework.cloud.skipper.client.SkipperClientProperties;
import org.springframework.cloud.skipper.shell.command.support.ConsoleUserInput;
import org.springframework.cloud.skipper.shell.command.support.Target;
import org.springframework.cloud.skipper.shell.command.support.TargetHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.shell.ResultHandler;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.DefaultShellApplicationRunner;
import org.springframework.shell.jline.PromptProvider;

/**
 * Configures the various commands that are part of the default Spring Shell experience.
 *
 * @author Josh Long
 * @author Mark Pollack
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@Import(SkipperClientConfiguration.class)
public class ShellConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(ShellConfiguration.class);

	@Bean
	public ApplicationRunner defaultShellRunner(
			LineReader lineReader,
			PromptProvider promptProvider,
			Parser parser,
			Shell shell) {
		return new DefaultShellApplicationRunner(lineReader, promptProvider, parser, shell);
	}

	@Bean
	public ApplicationRunner initialConnectionApplicationRunner() {
		return new InitialConnectionApplicationRunner();
	}

	@Bean
	public ConsoleUserInput userInput(@Lazy LineReader lineReader) {
		return new ConsoleUserInput(lineReader);
	}

	@Order(DefaultShellApplicationRunner.PRECEDENCE - 10)
	private static class InitialConnectionApplicationRunner implements ApplicationRunner {
		@Autowired
		private TargetHolder targetHolder;

		@Autowired
		private SkipperClientProperties clientProperties;

		@Autowired
		@Qualifier("main")
		private ResultHandler resultHandler;

		@Override
		public void run(ApplicationArguments args) throws Exception {
			Target target = new Target(clientProperties.getUri(), clientProperties.getUsername(),
					clientProperties.getPassword(), clientProperties.isSkipSllValidation());

			// Attempt connection (including against default values) but do not crash the shell on
			// error
			try {
				targetHolder.changeTarget(target, null);
			}
			catch (Exception e) {
				resultHandler.handleResult(e);
			}

		}
	}

}
