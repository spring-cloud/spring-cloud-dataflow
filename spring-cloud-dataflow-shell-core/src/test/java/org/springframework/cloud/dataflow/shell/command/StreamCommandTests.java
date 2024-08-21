/*
 * Copyright 2015-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.shell.AbstractShellIntegrationTest;
import org.springframework.cloud.dataflow.shell.command.support.TablesInfo;
import org.springframework.cloud.deployer.spi.app.ActuatorOperations;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.shell.table.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @author Glenn Renfro
 * @author Chris Bono
 */
public class StreamCommandTests extends AbstractShellIntegrationTest {

	private static final String APPS_URI = "META-INF/test-stream-apps.properties";

	private static final Logger logger = LoggerFactory.getLogger(StreamCommandTests.class);

	@BeforeEach
	public void registerApps() {
		AppRegistryService registry = applicationContext.getBean(AppRegistryService.class);
		registry.importAll(true, new ClassPathResource(APPS_URI));
	}

	@AfterEach
	public void destroyStreams() {
		stream().destroyCreatedStreams();
	}

	@Test
	public void testStreamLifecycleForTickTock() throws InterruptedException {
		String streamName = generateUniqueStreamOrTaskName();
		when(skipperClient.status(ArgumentMatchers.anyString())).thenReturn(setupBaseTest());
		AppDeployer appDeployer = applicationContext.getBean(AppDeployer.class);
		Deployer deployer = new Deployer("testDeployer", "testType", appDeployer, mock(ActuatorOperations.class));
		when(skipperClient.listDeployers()).thenReturn(Arrays.asList(deployer));
		stream().create(streamName, "time | log");
	}

	@Test
	public void testStreamUpdateForTickTock() throws InterruptedException {
		String streamName = generateUniqueStreamOrTaskName();

		when(skipperClient.status(ArgumentMatchers.anyString())).thenReturn(setupBaseTest());
		AppDeployer appDeployer = applicationContext.getBean(AppDeployer.class);
		Deployer deployer = new Deployer("testDeployer", "testType", appDeployer, mock(ActuatorOperations.class));
		when(skipperClient.listDeployers()).thenReturn(Arrays.asList(deployer));
		stream().create(streamName, "time | log");
		stream().update(streamName, "version.log=5.0.0","Update request has been sent for the stream");
	}

	@Test
	public void testStreamUpdatePropFileForTickTock() throws InterruptedException {
		String streamName = generateUniqueStreamOrTaskName();

		when(skipperClient.status(ArgumentMatchers.anyString())).thenReturn(setupBaseTest());
		AppDeployer appDeployer = applicationContext.getBean(AppDeployer.class);
		Deployer deployer = new Deployer("testDeployer", "testType", appDeployer, mock(ActuatorOperations.class));
		when(skipperClient.listDeployers()).thenReturn(Arrays.asList(deployer));
		stream().create(streamName, "time | log");
		File resourcesDirectory = new File("src/test/resources");
		stream().updateFile(streamName, resourcesDirectory.getAbsolutePath() + "/myproperties.properties","Update request has been sent for the stream");
	}

	private Info setupBaseTest() throws InterruptedException {
		logger.info("Starting Stream Test for TickTock Update");
		Thread.sleep(2000);
		Info info = new Info();
		Status status = new Status();
		status.setStatusCode(StatusCode.UNKNOWN);
		status.setPlatformStatus(null);
		info.setStatus(status);
		return info;
	}

	@Test
	public void testValidate() throws InterruptedException {
		Thread.sleep(2000);
		String streamName = generateUniqueStreamOrTaskName();
		Info info = new Info();
		Status status = new Status();
		status.setStatusCode(StatusCode.UNKNOWN);
		status.setPlatformStatus(null);
		info.setStatus(status);

		when(skipperClient.status(ArgumentMatchers.anyString())).thenReturn(info);
		AppDeployer appDeployer = applicationContext.getBean(AppDeployer.class);
		Deployer deployer = new Deployer("testDeployer", "testType", mock(AppDeployer.class), mock(ActuatorOperations.class));
		when(skipperClient.listDeployers()).thenReturn(Arrays.asList(deployer));

		//stream().create(streamName, "time | log");
		stream().createDontDeploy(streamName, "time | log");

		Object result = stream().validate(streamName);
		assertThat(result).isInstanceOf(TablesInfo.class);
		TablesInfo results = (TablesInfo) result;
		Table table = results.getTables().get(0);
		assertEquals("Number of columns returned was not expected", 2, table.getModel().getColumnCount());
		assertEquals("First Row First Value should be: Stream Name", "Stream Name", table.getModel().getValue(0, 0));
		assertEquals("First Row Second Value should be: Stream Definition", "Stream Definition", table.getModel().getValue(0, 1));
		assertEquals("Second Row First Value should be: " + streamName, streamName, table.getModel().getValue(1, 0));
		assertEquals("Second Row Second Value should be: time | log", "time | log", table.getModel().getValue(1, 1));

		String message = String.format("\n%s is a valid stream.", streamName);
		assertEquals(String.format("Notification should be: %s",message ), message, results.getFooters().get(0));

		table = results.getTables().get(1);
		assertEquals("Number of columns returned was not expected", 2, table.getModel().getColumnCount());
		assertEquals("First Row First Value should be: App Name", "App Name", table.getModel().getValue(0, 0));
		assertEquals("First Row Second Value should be: Validation Status", "Validation Status", table.getModel().getValue(0, 1));
		assertEquals("Second Row First Value should be: source:time", "source:time" , table.getModel().getValue(1, 0));
		assertEquals("Second Row Second Value should be: valid", "valid", table.getModel().getValue(1, 1));
		assertEquals("Third Row First Value should be: sink:log", "sink:log" , table.getModel().getValue(2, 0));
		assertEquals("Third Row Second Value should be: valid", "valid", table.getModel().getValue(2, 1));
	}

}
