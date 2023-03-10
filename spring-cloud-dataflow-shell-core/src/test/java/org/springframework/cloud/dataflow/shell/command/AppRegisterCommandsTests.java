/*
 * Copyright 2016-2022 the original author or authors.
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

import org.junit.Before;

import org.junit.Test;
import org.mockito.MockitoAnnotations;

import org.springframework.cloud.dataflow.rest.client.AppRegistryOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.shell.AbstractShellIntegrationTest;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RuntimeCommands}.
 *
 * @author Chris Bono
 * @author Corneil du Plessis
 */
public class AppRegisterCommandsTests extends AbstractShellIntegrationTest {

	private DataFlowOperations dataFlowOperations;

	private AppRegistryOperations registryOperations;

	private ResourceLoader resourceLoader;

	private AppRegistryCommands registryCommands;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(dataFlowOperations.appRegistryOperations()).thenReturn(registryOperations);
		DataFlowShell dataFlowShell = new DataFlowShell();
		dataFlowShell.setDataFlowOperations(dataFlowOperations);
		registryCommands = new AppRegistryCommands(dataFlowShell);
		registryCommands.setResourceLoader(resourceLoader);

	}

	@Test
	public void testRegisterApp() {
		// TODO implement
	}

	@Test
	public void testRegisterAppBoot3() {
		// TODO implement
	}

	@Test
	public void testRegisterAppUpdateToBoot3() {// TODO implement
		// TODO implement
	}

}
