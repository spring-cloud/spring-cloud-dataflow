/*
 * Copyright 2015-2022 the original author or authors.
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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.dataflow.shell.command.JobCommandTemplate;
import org.springframework.cloud.dataflow.shell.command.StreamCommandTemplate;
import org.springframework.cloud.dataflow.shell.command.TaskCommandTemplate;
import org.springframework.cloud.dataflow.shell.command.TaskScheduleCommandTemplate;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.context.ApplicationContext;
import org.springframework.shell.Shell;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.IdGenerator;

/**
 * Base class for shell integration tests. This class sets up and tears down the
 * infrastructure required for executing shell tests - in particular, the Data Flow
 * server.
 * Extensions of this class may obtain instances of command templates. For example, call
 * {@link #stream} to obtain a {@link StreamCommandTemplate} in order to perform stream
 * operations.
 *
 * @author Ilayaperumal Gopinathan
 * @author Patrick Peralta
 * @author Glenn Renfro
 * @author Chris Bono
 * @author Corneil du Plessis
 */
public abstract class AbstractShellIntegrationTest {

	// TODO: BOOT2 disabled metric stuff

	/**
	 * System property indicating whether the test infrastructure should be shut down
	 * after all tests are executed. If running in a test suite, this system property
	 * should be set to {@code false} to allow multiple tests to execute with the same
	 * Data Flow server.
	 */
	public static final String SHUTDOWN_AFTER_RUN = "shutdown.after.run";

	private static final Logger logger = LoggerFactory.getLogger(AbstractShellIntegrationTest.class);

	/**
	 * TCP port for the server.
	 */
	private static final int serverPort = TestSocketUtils.findAvailableTcpPort();

	/**
	 * Application context for server application.
	 */
	protected static ApplicationContext applicationContext;

	/**
	 * Skipper client mock
	 */
	protected static SkipperClient skipperClient;

	/**
	 * Indicates whether the test infrastructure should be shut down after all tests are
	 * executed.
	 *
	 * @see #SHUTDOWN_AFTER_RUN
	 */
	private static boolean shutdownAfterRun = false;

	/**
	 * Instance of shell to execute commands for testing.
	 */
	private static ShellCommandRunner commandRunner;

	/**
	 * Generator used to create random stream names.
	 */
	private final IdGenerator idGenerator = new AlternativeJdkIdGenerator();

	/**
	 * Used to capture currently executing test method.
	 */
	public TestInfo testInfo;

	@BeforeEach
	void prepareTest(TestInfo testInfo) {
		this.testInfo = testInfo;
	}

	@BeforeAll
	public static void startUp() {
		if (applicationContext == null) {
			shutdownAfterRun = Boolean.parseBoolean(System.getProperty(SHUTDOWN_AFTER_RUN, "false"));
			int randomPort = TestSocketUtils.findAvailableTcpPort();
			String dataFlowUri = String.format("--dataflow.uri=http://localhost:%s", serverPort);
			String dataSourceUrl = String.format("jdbc:h2:tcp://localhost:%s/mem:dataflow;DATABASE_TO_UPPER=FALSE", randomPort);
			SpringApplication application = new SpringApplicationBuilder(TestConfig.class).build();
			applicationContext = application.run(String.format("--server.port=%s", serverPort), dataFlowUri,
					"--spring.jmx.default-domain=" + System.currentTimeMillis(), "--spring.jmx.enabled=false",
					"--security.basic.enabled=false", "--spring.main.show_banner=false",
					"--spring.cloud.config.enabled=false",
					"--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
						"org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration," +
						"org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration," +
						"org.springframework.boot.autoconfigure.session.SessionAutoConfiguration," +
						"org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration," +
						"org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration," +
						"org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration," +
						"org.springframework.cloud.deployer.spi.local.LocalDeployerAutoConfiguration," +
						"org.springframework.cloud.task.configuration.SimpleTaskAutoConfiguration",
					"--spring.datasource.url=" + dataSourceUrl,
					"--spring.cloud.dataflow.features.schedules-enabled=true");
			Shell shell = applicationContext.getBean(Shell.class);
			skipperClient = applicationContext.getBean(SkipperClient.class);
			commandRunner = new ShellCommandRunner(shell);
		}
	}

	@AfterAll
	public static void shutdown() {
		if (shutdownAfterRun && applicationContext != null) {
			logger.info("Stopping Data Flow Server");
			SpringApplication.exit(applicationContext);
			applicationContext = null;
		}
	}

	/**
	 * Return a {@link StreamCommandTemplate} for issuing shell based stream commands.
	 *
	 * @return template for issuing stream commands
	 */
	protected StreamCommandTemplate stream() {
		return new StreamCommandTemplate(commandRunner);
	}

	/**
	 * Return a {@link TaskCommandTemplate} for issuing shell based task commands.
	 *
	 * @return template for issuing task commands
	 */
	protected TaskCommandTemplate task() {
		return new TaskCommandTemplate(commandRunner);
	}

	/**
	 * Return a {@link TaskCommandTemplate} that throws any errors back to caller.
	 *
	 * @return template for issuing task commands
	 */
	protected TaskCommandTemplate taskWithErrors() {
		return new TaskCommandTemplate(commandRunner.withValidateCommandSuccess());
	}

	/**
	 * Return a {@link TaskScheduleCommandTemplate} for issuing shell based task schedule commands.
	 *
	 * @return template for issuing task schedule commands
	 */
	protected TaskScheduleCommandTemplate schedule() {
		return new TaskScheduleCommandTemplate(commandRunner, applicationContext);
	}

	/**
	 * Return a {@link TaskScheduleCommandTemplate} that throws any errors back to caller.
	 *
	 * @return template for issuing task schedule commands
	 */
	protected TaskScheduleCommandTemplate scheduleWithErrors() {
		return new TaskScheduleCommandTemplate(commandRunner.withValidateCommandSuccess(), applicationContext);
	}

	/**
	 * Return a {@link JobCommandTemplate} for issuing shell based job commands.
	 *
	 * @return template for issuing job commands
	 */
	protected JobCommandTemplate job() {
		return new JobCommandTemplate(commandRunner);
	}

	/**
	 * Gets the configured shell command runner.
	 * @return the configured shell command runner
	 */
	protected ShellCommandRunner commandRunner() {
		return commandRunner;
	}

	/**
	 * Return a unique random name for stream/task testing.
	 *
	 * @param name name to use as part of stream/task name
	 * @return unique random stream/task name
	 */
	protected String generateUniqueStreamOrTaskName(String name) {
		// Return stream name of 20 characters of length
		if (name.length() > 16) {
			name = name.substring(0, 16);
		}
		return name + "-" + idGenerator.generateId().toString().substring(0, 4);
	}

	/**
	 * Return a unique random name for stream/task testing.
	 *
	 * @return unique random stream/task name
	 */
	protected String generateUniqueStreamOrTaskName() {
		return generateUniqueStreamOrTaskName(testInfo.getTestMethod().get().getName()
				.replace('[', '-').replace("]", ""));
	}

}
