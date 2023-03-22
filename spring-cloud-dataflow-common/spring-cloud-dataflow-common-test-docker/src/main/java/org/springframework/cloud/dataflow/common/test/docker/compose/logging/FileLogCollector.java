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
package org.springframework.cloud.dataflow.common.test.docker.compose.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerCompose;
import org.springframework.util.Assert;

public class FileLogCollector implements LogCollector {

	private static final Logger log = LoggerFactory.getLogger(FileLogCollector.class);

	private static final long STOP_TIMEOUT_IN_MILLIS = 50;

	private final File logDirectory;

	private ExecutorService executor = null;

	public FileLogCollector(File logDirectory) {
		Assert.state(!logDirectory.isFile(), "Log directory cannot be a file");
		if (!logDirectory.exists()) {
			Validate.isTrue(logDirectory.mkdirs(), "Error making log directory: " + logDirectory.getAbsolutePath());
		}
		this.logDirectory = logDirectory;
	}

	public static LogCollector fromPath(String path) {
		return new FileLogCollector(new File(path));
	}

	@Override
	public synchronized void startCollecting(DockerCompose dockerCompose) throws IOException, InterruptedException {
		if (executor != null) {
			throw new RuntimeException("Cannot start collecting the same logs twice");
		}

		List<String> serviceNames = dockerCompose.services();
		if (serviceNames.size() == 0) {
			return;
		}
		executor = Executors.newFixedThreadPool(serviceNames.size());
		serviceNames.stream().forEachOrdered(service -> this.collectLogs(service, dockerCompose));
	}

	private void collectLogs(String container, DockerCompose dockerCompose)  {
		executor.submit(() -> {
			File outputFile = new File(logDirectory, container + ".log");
			try {
				Files.createFile(outputFile.toPath());
			} catch (final FileAlreadyExistsException e) {
				// ignore
			} catch (final IOException e) {
				throw new RuntimeException("Error creating log file", e);
			}
			log.info("Writing logs for container '{}' to '{}'", container, outputFile.getAbsolutePath());
			try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
				dockerCompose.writeLogs(container, outputStream);
			} catch (IOException e) {
				throw new RuntimeException("Error reading log", e);
			}
		});
	}

	@Override
	public synchronized void stopCollecting() throws InterruptedException {
		if (executor == null) {
			return;
		}
		if (!executor.awaitTermination(STOP_TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS)) {
			log.warn("docker containers were still running when log collection stopped");
			executor.shutdownNow();
		}
		executor = null;
	}

}
