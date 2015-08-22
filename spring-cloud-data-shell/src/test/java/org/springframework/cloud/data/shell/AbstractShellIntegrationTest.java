/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.data.shell;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.data.admin.AdminApplication;
import org.springframework.cloud.data.admin.config.AdminConfiguration;
import org.springframework.cloud.data.module.registry.ModuleRegistry;
import org.springframework.cloud.data.module.registry.StubModuleRegistry;
import org.springframework.cloud.data.shell.command.StreamCommandTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.Bootstrap;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;
import org.springframework.util.SocketUtils;

/**
 * Base class for shell integration tests. This class sets up and tears down
 * the infrastructure required for executing shell tests - in particular, the
 * {@link AdminApplication} server.
 * <p>
 * Extensions of this class may obtain an instance of {@link StreamCommandTemplate}
 * via {@link #stream} in order to perform stream operations.
 *
 * @author Ilayaperumal Gopinathan
 * @author Patrick Peralta
 */
public abstract class AbstractShellIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(AbstractShellIntegrationTest.class);

	/**
	 * Generator used to create random stream names.
	 */
	private final IdGenerator idGenerator = new AlternativeJdkIdGenerator();

	/**
	 * System property indicating whether the test infrastructure should
	 * be shut down after all tests are executed. If running in a test
	 * suite, this system property should be set to {@code true} to allow
	 * multiple tests to execute with the same admin server.
	 */
	public static final String SHUTDOWN_AFTER_RUN = "shutdown.after.run";

	/**
	 * Indicates whether the test infrastructure should be shut down
	 * after all tests are executed.
	 *
	 * @see #SHUTDOWN_AFTER_RUN
	 */
	private static boolean shutdownAfterRun = true;

	/**
	 * Application context for admin application.
	 */
	private static ApplicationContext applicationContext;

	/**
	 * Instance of shell to execute commands for testing.
	 */
	private static CloudDataShell cloudDataShell;

	/**
	 * TCP port for the admin server.
	 */
	private static final int adminPort = SocketUtils.findAvailableTcpPort();

	/**
	 * Used to capture currently executing test method.
	 */
	@Rule
	public TestName name = new TestName();

	@BeforeClass
	public static void startUp() throws InterruptedException, IOException {
		if (applicationContext == null) {
			if (System.getProperty(SHUTDOWN_AFTER_RUN) != null) {
				shutdownAfterRun = Boolean.getBoolean(SHUTDOWN_AFTER_RUN);
			}

			SpringApplication application = new SpringApplicationBuilder(AdminApplication.class,
					AdminConfiguration.class, TestConfig.class).build();
			applicationContext = application.run(
					String.format("--server.port=%s", adminPort), "--spring.main.show_banner=false");
		}
		JLineShellComponent shell = new Bootstrap(new String[] {"--port", String.valueOf(adminPort)})
				.getJLineShellComponent();
		if (!shell.isRunning()) {
			shell.start();
		}
		cloudDataShell = new CloudDataShell(shell);
	}

	@AfterClass
	public static void shutdown() {
		if (shutdownAfterRun) {
			logger.info("Stopping Cloud Data Shell");
			if (cloudDataShell != null) {
				cloudDataShell.stop();
			}
			if (applicationContext != null) {
				logger.info("Stopping Cloud Data Admin Server");
				SpringApplication.exit(applicationContext);
				applicationContext = null;
			}
		}
	}

	/**
	 * Return a {@link StreamCommandTemplate} for issuing shell based
	 * stream commands.
	 *
	 * @return template for issuing stream commands
	 */
	protected StreamCommandTemplate stream() {
		return new StreamCommandTemplate(cloudDataShell);
	}

	/**
	 * Return a unique random stream name for testing.
	 *
	 * @param name name to use as part of stream name
	 * @return unique random stream name
	 */
	protected String generateStreamName(String name) {
		return (name == null) ? generateUniqueName() : generateUniqueName(name);
	}

	/**
	 * Return a unique random stream name for testing.
	 *
	 * @return unique random stream name
	 */
	protected String generateStreamName() {
		return generateStreamName(null);
	}

	// Util methods
	private String generateUniqueName(String name) {
		return name + "-" + idGenerator.generateId();
	}

	private String generateUniqueName() {
		return generateUniqueName(name.getMethodName().replace('[', '-').replaceAll("]", ""));
	}


	private static class CloudDataShell extends JLineShellComponent {

		private final JLineShellComponent shell;

		public CloudDataShell(JLineShellComponent shell) {
			this.shell = shell;
		}

		public CommandResult executeCommand(String command) {
			CommandResult cr = this.shell.executeCommand(command);
			if (cr.getException() != null) {
				cr.getException().printStackTrace();
			}
			Assert.isTrue(cr.isSuccess(), "Failure.  CommandResult = " + cr.toString());
			return cr;
		}
	}

	/**
	 * Configuration for admin server that is specific to shell tests.
	 */
	@Configuration
	public static class TestConfig {

		@Bean
		public ModuleRegistry moduleRegistry() {
			return new StubModuleRegistry();
		}

	}
}
