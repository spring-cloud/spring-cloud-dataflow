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

import org.springframework.cloud.data.rest.AdminApplication;
import org.springframework.cloud.data.shell.command.StreamCommandTemplate;
import org.springframework.shell.Bootstrap;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;
import org.springframework.util.SocketUtils;

/**
 * Base class for all the Shell integration test classes.
 *
 * @author Ilayaperumal Gopinathan
 */
public abstract class AbstractShellIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(AbstractShellIntegrationTest.class);

	private final IdGenerator idGenerator = new AlternativeJdkIdGenerator();

	/**
	 * This property can be set to false when running test suite.
	 */
	private static final boolean SHUTDOWN_AFTER_RUN = true;

	private static AdminApplication application;

	private static CloudDataShell cloudDataShell;

	/**
	 * Use available TCP port for the admin port
	 */
	private static final int adminPort = SocketUtils.findAvailableTcpPort();

	/**
	 * Used to capture currently executing test method.
	 */
	@Rule
	public TestName name = new TestName();

	@BeforeClass
	public static synchronized void startUp() throws InterruptedException, IOException {
		if (application == null) {
			application = new AdminApplication();
			application.main(new String[] {
					String.format("--server.port=%s", adminPort),
					"--spring.main.show_banner=false"});
		}
		JLineShellComponent shell = new Bootstrap(new String[] {"--port", String.valueOf(adminPort)}).getJLineShellComponent();
		if (!shell.isRunning()) {
			shell.start();
		}
		cloudDataShell = new CloudDataShell(shell);
	}

	@AfterClass
	public static void shutdown() {
		if (SHUTDOWN_AFTER_RUN) {
			logger.info("Stopping Cloud Data Shell");
			cloudDataShell.stop();
			if (application != null) {
				logger.info("Stopping Cloud Data Admin Server");
				application = null;
			}
		}
	}

	// Instantiate Command templates here
	protected StreamCommandTemplate stream() {
		return new StreamCommandTemplate(cloudDataShell);
	}

	// Util methods
	private String generateUniqueName(String name) {
		return name + "-" + idGenerator.generateId();
	}

	private String generateUniqueName() {
		return generateUniqueName(name.getMethodName().replace('[', '-').replaceAll("]", ""));
	}

	protected String generateStreamName(String name) {
		return (name == null) ? generateUniqueName() : generateUniqueName(name);
	}

	protected String generateStreamName() {
		return generateStreamName(null);
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
}
