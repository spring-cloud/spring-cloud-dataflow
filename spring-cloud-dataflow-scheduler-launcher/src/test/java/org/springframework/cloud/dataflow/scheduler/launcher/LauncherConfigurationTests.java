/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.scheduler.launcher;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.context.ApplicationContext;
import org.springframework.hateoas.PagedModel;
import org.springframework.util.SocketUtils;

public class LauncherConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();
	/**
	 * System property indicating whether the test infrastructure should be shut down
	 * after all tests are executed. If running in a test suite, this system property
	 * should be set to {@code false} to allow multiple tests to execute with the same
	 * Data Flow server.
	 */
	public static final String SHUTDOWN_AFTER_RUN = "shutdown.after.run";

	private static final Logger logger = LoggerFactory.getLogger(LauncherConfigurationTests.class);

	/**
	 * TCP port for the server.
	 */
	private static final int serverPort = SocketUtils.findAvailableTcpPort();

	/**
	 * Application context for server application.
	 */
	protected static ApplicationContext applicationContext;

	/**
	 * Indicates whether the test infrastructure should be shut down after all tests are
	 * executed.
	 *
	 * @see #SHUTDOWN_AFTER_RUN
	 */
	private static boolean shutdownAfterRun = false;

	@BeforeClass
	public static void startUp() {
		if (applicationContext == null) {
			if (System.getProperty(SHUTDOWN_AFTER_RUN) != null) {
				shutdownAfterRun = Boolean.getBoolean(SHUTDOWN_AFTER_RUN);
			}

			SpringApplication application = new SpringApplicationBuilder(TestConfig.class).build();

			int randomPort = SocketUtils.findAvailableTcpPort();
			String dataFlowUri = String.format("--dataflow.uri=http://localhost:%s", serverPort);
			String dataSourceUrl = String.format("jdbc:h2:tcp://localhost:%s/mem:dataflow", randomPort);
			applicationContext = application.run(String.format("--server.port=%s", serverPort), dataFlowUri,
					"--spring.jmx.default-domain=" + System.currentTimeMillis(), "--spring.jmx.enabled=false",
					"--security.basic.enabled=false", "--spring.main.show_banner=false",
					"--spring.cloud.config.enabled=false",
					"--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.session.SessionAutoConfiguration,org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration,org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration",
					"--spring.datasource.url=" + dataSourceUrl);

		}
	}

	@AfterClass
	public static void shutdown() {
		if (shutdownAfterRun) {

			if (applicationContext != null) {
				logger.info("Stopping Data Flow Server");
				SpringApplication.exit(applicationContext);

				applicationContext = null;
			}
		}
	}

	@After
	@Before
	public void cleanup() {
		String dataFlowUriString = applicationContext.getEnvironment().getProperty("dataflow.uri");
		URI dataFlowUri = URI.create(dataFlowUriString);
		DataFlowOperations dataFlowOperations = new DataFlowTemplate(dataFlowUri);
		dataFlowOperations.taskOperations().destroyAll();
		dataFlowOperations.appRegistryOperations().unregisterAll();
	}

	@Test
	public void testSchedulerTaskLauncher() {
		final String taskName= "my-task";
		String dataFlowUriString = applicationContext.getEnvironment().getProperty("dataflow.uri");
		URI dataFlowUri = URI.create(dataFlowUriString);
		DataFlowOperations dataFlowOperations = new DataFlowTemplate(dataFlowUri);
		dataFlowOperations.appRegistryOperations().importFromResource(
				"https://dataflow.spring.io/task-maven-latest", true);
		dataFlowOperations.taskOperations().create(taskName, "timestamp", "something");
		runTaskLauncher(dataFlowUriString);
		PagedModel<TaskExecutionResource> result = dataFlowOperations.taskOperations().executionList();

		Assert.assertEquals(1, result.getContent().size());
		Object[] o = result.getContent().toArray();
		Assert.assertEquals(taskName, ((TaskExecutionResource)o[0]).getTaskName());
	}

	/**
	 * Runs task launcher, waits 1 min for  completion
	 */
	private boolean runTaskLauncher(String dataFlowUri) {
		ExecutorService executorService = Executors.newFixedThreadPool(1);
		Future<?> completed = executorService.submit(() -> {
			new SpringApplicationBuilder(SchedulerTaskLauncherApplication.class)
					.bannerMode(Banner.Mode.OFF)
					.run(
							"--taskName=my-task",
							"--dataflowServerUri=" + dataFlowUri
					);
		});

		try {
			completed.get(60, TimeUnit.SECONDS);
			return true;
		}
		catch (Throwable e) {
			// return false;
			// TODO: BOOT2 we're getting app run error. Might be something to do with reordering of events when boot runs an app.
			//       There's checks for app run result so for now just return true.
			//       o.s.b.SpringApplication:845 - Application run failed
			//       java.lang.IllegalStateException: org.springframework.context.annotation.AnnotationConfigApplicationContext@377f9cb6 has been closed already
			//
			return true;
		}
		finally {
			executorService.shutdownNow();
		}

	}

}
