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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.cloud.dataflow.rest.Version;
import org.springframework.cloud.dataflow.rest.client.AboutOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.RootResource;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.cloud.dataflow.rest.resource.about.RuntimeEnvironmentDetails;
import org.springframework.cloud.dataflow.rest.resource.security.SecurityInfoResource;
import org.springframework.cloud.dataflow.rest.support.jackson.Jackson2DataflowModule;
import org.springframework.cloud.dataflow.shell.Target;
import org.springframework.cloud.dataflow.shell.TargetHolder;
import org.springframework.cloud.dataflow.shell.command.support.TablesInfo;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.cloud.dataflow.shell.config.DataFlowShellProperties;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.shell.table.Table;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConfigCommands}.
 *
 * @author Gunnar Hillert
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Chris Bono
 * @author Corneil du Plessis
 */
public class ConfigCommandTests {

	private ConfigCommands configCommands;

	private DataFlowShell dataFlowShell;

	@Mock
	private RestTemplate restTemplate;

	private ObjectMapper mapper;


	@BeforeEach
	public void setUp() {
		if (this.mapper == null) {
			this.mapper = new ObjectMapper();
			this.mapper.registerModule(new Jdk8Module());
			this.mapper.registerModule(new Jackson2HalModule());
			this.mapper.registerModule(new JavaTimeModule());
			this.mapper.registerModule(new Jackson2DataflowModule());
		}
		MockitoAnnotations.openMocks(this);

		dataFlowShell = new DataFlowShell();

		ConsoleUserInput userInput = mock(ConsoleUserInput.class);
		when(userInput.prompt(anyString(), anyString(), anyBoolean())).thenReturn(null);

		final List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		messageConverters.add(new MappingJackson2HttpMessageConverter());
		when(restTemplate.getMessageConverters()).thenReturn(messageConverters);

		TargetHolder targetHolder = new TargetHolder();
		targetHolder.setTarget(new Target("http://localhost:9393"));

		configCommands = new ConfigCommands(dataFlowShell, shellProperties(), userInput, targetHolder, restTemplate, null, mapper);
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
			aboutResource.getRuntimeEnvironment().getAppDeployer()
					.getPlatformSpecificInfo().put("Some", "Stuff");
			List<RuntimeEnvironmentDetails> taskLaunchers = new ArrayList<>();
			taskLaunchers.add(new RuntimeEnvironmentDetails());
			aboutResource.getRuntimeEnvironment().setTaskLaunchers(taskLaunchers);
			aboutResource.getRuntimeEnvironment().getTaskLaunchers().get(0)
					.setDeployerSpiVersion("6.4");
			TablesInfo tablesInfo = configCommands.info();
			final Table infoResult = tablesInfo.getTables().get(0);
			String expectedOutput = FileCopyUtils.copyToString(new InputStreamReader(
					getClass().getResourceAsStream(ConfigCommandTests.class.getSimpleName() + "-testInfo.txt"), StandardCharsets.UTF_8));
			assertThat(infoResult.render(80)
					.replace("\r\n", "\n")).isEqualTo(expectedOutput.replace("\r\n", "\n"));
		}
	}

	@Test
	public void testApiRevisionMismatch() throws Exception {
		RootResource value = new RootResource(-12);
		value.add(Link.of("http://localhost:9393/dashboard", "dashboard"));
		when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(RootResource.class))).thenReturn(value);

		assertThatThrownBy(() -> configCommands.target("http://localhost:9393", null, null, null, null, false, null, null, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Incompatible version of Data Flow server detected");
	}

	@Test
	public void testModeWithSkipperShellAndSkipperServer() throws Exception {
		String expectedTargetMessage = "Successfully targeted http://localhost:9393/";
		AboutResource aboutResource = new AboutResource();

		when(restTemplate.getForObject(Mockito.any(String.class), Mockito.eq(AboutResource.class))).thenReturn(aboutResource);

		RootResource value = new RootResource(Version.REVISION);
		value.add(Link.of("http://localhost:9393/dashboard", "dashboard"));
		value.add(Link.of("http://localhost:9393/about", "about"));
		value.add(Link.of("http://localhost:9393/apps", "apps"));
		value.add(Link.of("http://localhost:9393/completions/task", "completions/task"));
		value.add(Link.of("http://localhost:9393/completions/stream", "completions/stream"));

		when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(RootResource.class))).thenReturn(value);

		SecurityInfoResource securityInfoResource = new SecurityInfoResource();
		securityInfoResource.setAuthenticationEnabled(false);
		when(restTemplate.getForObject(Mockito.any(String.class), Mockito.eq(SecurityInfoResource.class))).thenReturn(securityInfoResource);

		final String targetResult = configCommands.target(
				Target.DEFAULT_TARGET,
				Target.DEFAULT_USERNAME,
				Target.DEFAULT_PASSWORD,
				Target.DEFAULT_CLIENT_REGISTRATION_ID,
				Target.DEFAULT_CREDENTIALS_PROVIDER_COMMAND,
				true,
				Target.DEFAULT_PROXY_URI,
				Target.DEFAULT_PROXY_USERNAME,
				Target.DEFAULT_PROXY_PASSWORD);

		assertThat(targetResult).isEqualTo(expectedTargetMessage);
	}


	private boolean isWindows() {
		return System.getProperty("os.name", "null").toLowerCase().startsWith("windows");
	}

	private DataFlowShellProperties shellProperties() {
		Binder binder = new Binder(new MapConfigurationPropertySource());
		try {
			return binder.bindOrCreate("dataflow", DataFlowShellProperties.class);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
