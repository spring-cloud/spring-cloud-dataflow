/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.common.test.docker.compose.execution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.joining;

public class Command {

	public static final int HOURS_TO_WAIT_FOR_STD_OUT_TO_CLOSE = 12;

	public static final int MINUTES_TO_WAIT_AFTER_STD_OUT_CLOSES = 1;

	private final Executable executable;

	private final Consumer<String> logConsumer;

	public Command(Executable executable, Consumer<String> logConsumer) {
		this.executable = executable;
		this.logConsumer = logConsumer;
	}

	public String execute(ErrorHandler errorHandler, boolean composeCommand, String... commands) throws IOException, InterruptedException {
		ProcessResult result = run(composeCommand, commands);

		if (result.exitCode() != 0) {
			errorHandler.handle(result.exitCode(), result.output(), executable.commandName(), commands);
		}

		return result.output();
	}

	public static ErrorHandler throwingOnError() {
		return (exitCode, output, commandName, commands) -> {
			String message =
					constructNonZeroExitErrorMessage(exitCode, commandName, commands) + "\nThe output was:\n" + output;
			throw new DockerExecutionException(message);
		};
	}

	private static String constructNonZeroExitErrorMessage(int exitCode, String commandName, String... commands) {
		return "'" + commandName + " " + Arrays.stream(commands).collect(joining(" ")) + "' returned exit code "
				+ exitCode;
	}

	private ProcessResult run(boolean composeCommand, String... commands) throws IOException, InterruptedException {
		Process process = executable.execute(composeCommand, commands);

		ExecutorService exec = newSingleThreadExecutor();
		Future<String> outputProcessing = exec
				.submit(() -> processOutputFrom(process));

		String output = waitForResultFrom(outputProcessing);

		process.waitFor(MINUTES_TO_WAIT_AFTER_STD_OUT_CLOSES, TimeUnit.MINUTES);
		exec.shutdown();

		return new ProcessResult(process.exitValue(), output);
	}

	private String processOutputFrom(Process process) {
		return asReader(process.getInputStream()).lines()
				.peek(logConsumer)
				.collect(joining(System.lineSeparator()));
	}

	private static String waitForResultFrom(Future<String> outputProcessing) {
		try {
			return outputProcessing.get(HOURS_TO_WAIT_FOR_STD_OUT_TO_CLOSE, TimeUnit.HOURS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new RuntimeException(e);
		}
	}

	private static BufferedReader asReader(InputStream inputStream) {
		return new BufferedReader(new InputStreamReader(inputStream, UTF_8));
	}
}
