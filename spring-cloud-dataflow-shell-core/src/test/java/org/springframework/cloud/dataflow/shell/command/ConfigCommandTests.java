/*
 * Copyright 2016-2018 the original author or authors.
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.springframework.cloud.dataflow.rest.Version;
import org.springframework.cloud.dataflow.rest.client.AboutOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.RootResource;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.cloud.dataflow.rest.resource.security.SecurityInfoResource;
import org.springframework.cloud.dataflow.shell.DataFlowMode;
import org.springframework.cloud.dataflow.shell.Target;
import org.springframework.cloud.dataflow.shell.TargetHolder;
import org.springframework.cloud.dataflow.shell.command.common.ConfigCommands;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.hateoas.Link;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.shell.CommandLine;
import org.springframework.shell.table.Table;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConfigCommands}.
 *
 * @author Gunnar Hillert
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
public class ConfigCommandTests {

	private ConfigCommands configCommands = new ConfigCommands();

	private DataFlowShell dataFlowShell = new DataFlowShell();

	@Mock
	private RestTemplate restTemplate;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		final CommandLine commandLine = Mockito.mock(CommandLine.class);

		when(commandLine.getArgs()).thenReturn(null);

		final List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		messageConverters.add(new MappingJackson2HttpMessageConverter());

		when(restTemplate.getMessageConverters()).thenReturn(messageConverters);

		TargetHolder targetHolder = new TargetHolder();
		targetHolder.setTarget(new Target("http://localhost:9393"));
		configCommands.setTargetHolder(targetHolder);
		configCommands.setRestTemplate(restTemplate);
		configCommands.setDataFlowShell(dataFlowShell);
		configCommands.setServerUri("http://localhost:9393");
	}

	public static boolean isWindows() {
		String osName = System.getProperty("os.name");

		return osName != null && osName.toLowerCase().startsWith("windows");
	}

	@Test
	public void testInfo() throws IOException {
		if (!isWindows()) {
			DataFlowOperations dataFlowOperations = mock(DataFlowOperations.class);
			AboutOperations aboutOperations = mock(AboutOperations.class);
			when(dataFlowOperations.aboutOperation()).thenReturn(aboutOperations);
			AboutResource aboutResource = new AboutResource();
			when(aboutOperations.get()).thenReturn(aboutResource);
			dataFlowShell.setDataFlowOperations(dataFlowOperations);

			aboutResource.getFeatureInfo().setTasksEnabled(false);
			aboutResource.getVersionInfo().getCore().setName("Foo Core");
			aboutResource.getVersionInfo().getCore().setVersion("1.2.3.BUILD-SNAPSHOT");
			aboutResource.getSecurityInfo().setAuthenticationEnabled(true);
			aboutResource.getRuntimeEnvironment().getAppDeployer().setJavaVersion("1.8");
			aboutResource.getRuntimeEnvironment().getAppDeployer().getPlatformSpecificInfo().put("Some", "Stuff");
			aboutResource.getRuntimeEnvironment().getTaskLauncher().setDeployerSpiVersion("6.4");
			final Table infoResult = (Table) configCommands.info().get(0);
			String expectedOutput = FileCopyUtils.copyToString(new InputStreamReader(
					getClass().getResourceAsStream(ConfigCommandTests.class.getSimpleName() + "-testInfo.txt"), "UTF-8"));
			assertThat(infoResult.render(80), is(expectedOutput));
		}
	}

	@Test
	public void testApiRevisionMismatch() {
		RootResource value = new RootResource(-12);
		value.add(new Link("http://localhost:9393/dashboard", "dashboard"));
		when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(RootResource.class))).thenReturn(value);

		configCommands.onApplicationEvent(null);

		final String targetResult = configCommands.target("http://localhost:9393", null, null, null, false, null, null, null);
		assertThat(targetResult, containsString("Incompatible version of Data Flow server detected"));
	}

	@Test
	public void testModeWithSkipperShellAndClassicServer() {
		testDataFlowMode(DataFlowMode.skipper, DataFlowMode.classic,
				"You must re-start the Shell with --dataflow.mode=classic");
	}

	@Test
	public void testModeWithSkipperShellAndSkipperServer() {
		testDataFlowMode(DataFlowMode.skipper, DataFlowMode.skipper,
				"Shell mode: skipper, Server mode: skipper");
	}

	@Test
	public void testModeWithClassicShellAndSkipperServer() {
		testDataFlowMode(DataFlowMode.classic, DataFlowMode.skipper,
				"You must re-start the Shell with --dataflow.mode=skipper");
	}

	@Test
	public void testModeWithClassicShellAndClassicServer() {
		testDataFlowMode(DataFlowMode.classic, DataFlowMode.classic,
				"Shell mode: classic, Server mode: classic");
	}

	public void testDataFlowMode(DataFlowMode shellDataFlowMode, DataFlowMode serverDataFlowMode, String expectedTargetMessage) {

		configCommands.setShellDataflowMode(shellDataFlowMode);

		AboutResource aboutResource = new AboutResource();
		if (serverDataFlowMode == DataFlowMode.skipper) {
			aboutResource.getFeatureInfo().setSkipperEnabled(true);
		}
		when(restTemplate.getForObject(Mockito.any(String.class), Mockito.eq(AboutResource.class))).thenReturn(aboutResource);

		RootResource value = new RootResource(Version.REVISION);
		value.add(new Link("http://localhost:9393/dashboard", "dashboard"));
		value.add(new Link("http://localhost:9393/about", "about"));
		value.add(new Link("http://localhost:9393/apps", "apps"));
		value.add(new Link("http://localhost:9393/completions/task", "completions/task"));
		value.add(new Link("http://localhost:9393/completions/stream", "completions/stream"));

		when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(RootResource.class))).thenReturn(value);

		SecurityInfoResource securityInfoResource = new SecurityInfoResource();
		securityInfoResource.setAuthenticationEnabled(false);
		when(restTemplate.getForObject(Mockito.any(String.class), Mockito.eq(SecurityInfoResource.class))).thenReturn(securityInfoResource);

		final String targetResult = configCommands.target(
				Target.DEFAULT_TARGET,
				Target.DEFAULT_USERNAME,
				Target.DEFAULT_SPECIFIED_PASSWORD,
				Target.DEFAULT_CREDENTIALS_PROVIDER_COMMAND,
				true,
				Target.DEFAULT_PROXY_URI,
				Target.DEFAULT_PROXY_USERNAME,
				Target.DEFAULT_PROXY_SPECIFIED_PASSWORD);

		System.out.println(targetResult);

		assertThat(targetResult, is(expectedTargetMessage));
	}
}
