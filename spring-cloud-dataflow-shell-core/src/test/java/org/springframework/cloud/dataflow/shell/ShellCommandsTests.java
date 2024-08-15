/*
 * Copyright 2018-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.shell;

import java.io.FileNotFoundException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.util.ResourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify launching a shell application instance against a running Dataflow server instance.
 *
 * <p>These tests extend the {@link AbstractShellIntegrationTest} so that each test has a running DataFlow
 * Server and then also launches the shell as a separate application that points to the already running Dataflow server.
 *
 * @author Furer Alexander
 * @author Chris Bono
 * @author Corneil du Plessis
 */
@Disabled("taskRepository not found")
class ShellCommandsTests extends AbstractShellIntegrationTest {

	@AfterEach
	@BeforeEach
	void unregisterAll() {
		AppRegistryService registry = applicationContext.getBean(AppRegistryService.class);
		for (AppRegistration appReg : registry.findAll()) {
			registry.delete(appReg.getName(), appReg.getType(), appReg.getVersion());
		}
	}

	@Test
	void singleFileCommand() {
		String commandFiles = toAbsolutePaths("commands/registerTask_timestamp.txt");
		// TODO add boot 3 checks
		assertThat(runShell(commandFiles)).isTrue();
		assertAppExists("timestamp", ApplicationType.task);
	}

	@Test
	void multiFileCommandOrderPreserved() {
		String commandFiles = toAbsolutePaths(
				"commands/stream_all_delete.txt,commands/registerTask_timestamp.txt,commands/unregisterTask_timestamp.txt,commands/registerSink_log.txt,commands/unregisterSink_log.txt");
		assertThat(runShell(commandFiles)).isTrue();
		assertThat(AbstractShellIntegrationTest.applicationContext.getBean(AppRegistryService.class).findAll()).isEmpty();
	}

	@Test
	void multiFileCommand() {
		String commandFiles = toAbsolutePaths("commands/registerTask_timestamp.txt,commands/registerSink_log.txt");
		assertThat(runShell(commandFiles)).isTrue();
		assertAppExists("timestamp", ApplicationType.task);
		assertAppExists("log", ApplicationType.sink);
	}

	private void assertAppExists(String name, ApplicationType type) {
		AppRegistryService registry = AbstractShellIntegrationTest.applicationContext.getBean(AppRegistryService.class);
		assertThat(registry.appExist(name, type)).isTrue();
	}

	/**
	 * Convert comma separated resources locations to comma separated absolute paths string
	 * @param fileResources
	 * @return
	 */
	private String toAbsolutePaths(String fileResources) {
		return Stream.of(fileResources.split(","))
				.map(f -> {
					try {
						return ResourceUtils.getFile("classpath:" + f).getAbsolutePath();
					}
					catch (FileNotFoundException e) {
						throw new RuntimeException(e);
					}
				})
				.collect(Collectors.joining(","));
	}

	/**
	 * Runs shell with given command files, waits 1 min for completion.
	 *
	 * <p>NOTE: this starts up another application context for running the shell against the Dataflow Server that was
	 * started by the parent {@link AbstractShellIntegrationTest}.
	 *
	 * @param commandFiles
	 */
	private boolean runShell(String commandFiles) {
		String dataFlowUri = applicationContext.getEnvironment().getProperty("dataflow.uri");
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		Future<?> completed = executorService.submit(() -> {
			new SpringApplicationBuilder(ShellApp.class)
					.web(WebApplicationType.NONE)
					.bannerMode(Banner.Mode.OFF)
					.run(
							"--spring.shell.command-file=" + commandFiles,
							"--spring.cloud.config.enabled=false",
							"--spring.autoconfigure.exclude=" + Stream.of(SessionAutoConfiguration.class,
									DataSourceAutoConfiguration.class,
									HibernateJpaAutoConfiguration.class)
									.map(Class::getName)
									.collect(Collectors.joining(",")),
							"--dataflow.uri=" + dataFlowUri
					);
		});

		try {
			completed.get(60, TimeUnit.SECONDS);
			return true;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			executorService.shutdownNow();
		}

	}

	/**
	 * The test application that is used for running the shell.
	 *
	 * <p>NOTE: To prevent the other {@link TestConfig test application} used by the parent {@link AbstractShellIntegrationTest}
	 * from being auto-configured when this application is run, a component scan is used that excludes that configuration
	 * class. Otherwise, we would have 2 full-blown Dataflow servers up and running.
	 */
	@ComponentScan(
			basePackages = "org.springframework.cloud.dataflow.shell",
			excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = TestConfig.class)
	)
	@EnableAutoConfiguration(exclude = DataFlowClientAutoConfiguration.class)
	public static class ShellApp {
	}

}
