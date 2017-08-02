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
package org.springframework.cloud.skipper.shell.support;

import java.util.logging.Logger;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.shell.CommandLine;
import org.springframework.shell.ShellException;
import org.springframework.shell.core.ExitShellRequest;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.shell.support.logging.HandlerUtils;
import org.springframework.shell.support.util.FileUtils;
import org.springframework.util.StopWatch;

/**
 * This does basically the same thing as {@link org.springframework.shell.Bootstrap} in
 * Spring Shell, but using Spring Boot's {@link CommandLineRunner} as a callback hook for
 * initialization, instead of squatting on the application's one
 * {@code main(String[] args)} method.
 * <p>
 * This configuration also uses Spring Boot to parse command line arguments instead of
 * {@link org.springframework.shell.SimpleShellCommandLineOptions#parseCommandLine(String[])}.
 * This means that command line arguments use a different syntax than in Spring Shell. Key
 * value pairs for arguments need to be passed with an equal sign as the separator rather
 * than a space.
 *
 * @author Josh Long
 * @author Mark Pollack
 */
public class ShellCommandLineRunner implements CommandLineRunner, ApplicationContextAware {

	private final StopWatch stopWatch = new StopWatch("Spring Shell");

	private final Logger logger = Logger.getLogger(getClass().getName());

	private ApplicationContext applicationContext;

	@Autowired
	private JLineShellComponent lineShellComponent;

	@Autowired
	private CommandLine commandLine;

	@Autowired
	private ApplicationArguments applicationArguments;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void run(String... args) throws Exception {
		SpringApplication.exit(this.applicationContext, new ShellExitCodeGenerator(this.doRun()));
	}

	private ExitShellRequest doRun() {
		this.stopWatch.start();
		try {

			String[] commandsToExecuteAndThenQuit = this.commandLine.getShellCommandsToExecute();
			ExitShellRequest exitShellRequest;
			if (null != commandsToExecuteAndThenQuit) {

				boolean successful = false;
				exitShellRequest = ExitShellRequest.FATAL_EXIT;

				for (String cmd : commandsToExecuteAndThenQuit) {
					successful = this.lineShellComponent.executeCommand(cmd).isSuccess();
					if (!successful) {
						break;
					}
				}

				if (successful) {
					exitShellRequest = ExitShellRequest.NORMAL_EXIT;
				}
			}
			else if (this.applicationArguments.containsOption("help")) {
				System.out.println(FileUtils.readBanner(ShellCommandLineRunner.class, "/usage.txt"));
				exitShellRequest = ExitShellRequest.NORMAL_EXIT;
			}
			else {
				this.lineShellComponent.start();
				this.lineShellComponent.promptLoop();
				exitShellRequest = this.lineShellComponent.getExitShellRequest();
				if (exitShellRequest == null) {
					exitShellRequest = ExitShellRequest.NORMAL_EXIT;
				}
				this.lineShellComponent.waitForComplete();
			}

			if (this.lineShellComponent.isDevelopmentMode()) {
				System.out.println("Total execution time: " + this.stopWatch.getLastTaskTimeMillis() + " ms");
			}

			return exitShellRequest;
		}
		catch (Exception ex) {
			throw new ShellException(ex.getMessage(), ex);
		}
		finally {
			HandlerUtils.flushAllHandlers(this.logger);
			this.stopWatch.stop();
		}
	}

	private static class ShellExitCodeGenerator implements ExitCodeGenerator {

		private final ExitShellRequest exitShellRequest;

		ShellExitCodeGenerator(ExitShellRequest exitShellRequest) {
			this.exitShellRequest = exitShellRequest;
		}

		@Override
		public int getExitCode() {
			return this.exitShellRequest.getExitCode();
		}
	}
}
