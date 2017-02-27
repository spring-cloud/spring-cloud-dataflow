/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.springframework.cloud.dataflow.rest.Version;
import org.springframework.cloud.dataflow.shell.TargetHolder;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.shell.CommandLine;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for {@link ConfigCommands}.
 *
 * @author Gunnar Hillert
 *
 */
public class ConfigCommandTests {

	private ConfigCommands configCommands = new ConfigCommands();

	private DataFlowShell dataFlowShell = new DataFlowShell();

	@Mock
	private RestTemplate restTemplate;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		final CommandLine commandLine = Mockito.mock(CommandLine.class);

		when(commandLine.getArgs()).thenReturn(null);

		final List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		messageConverters.add(new  MappingJackson2HttpMessageConverter());

		when(restTemplate.getMessageConverters()).thenReturn(messageConverters);

		configCommands.setTargetHolder(new TargetHolder());
		configCommands.setRestTemplate(restTemplate);
		configCommands.setDataFlowShell(dataFlowShell);
		configCommands.setServerUri("http://localhost:9393");
	}

	@Test
	public void testInfo() {
		when(restTemplate.getForObject(Mockito.endsWith("/management/info"), Mockito.eq(Map.class)))
			.thenReturn(Collections.singletonMap(Version.REVISION_KEY, String.valueOf(Version.REVISION)));
		final Exception e = new RestClientException("FooBar");
		when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(ResourceSupport.class))).thenThrow(e);

		configCommands.onApplicationEvent(null);

		final String infoResult = configCommands.info();
		assertThat(infoResult, containsString("Target│http://localhost:9393"));
		assertThat(infoResult, containsString("RestClientException: FooBar"));
	}

	@Test
	public void testApiRevisionMismatch() {
		when(restTemplate.getForObject(Mockito.endsWith("/management/info"), Mockito.eq(Map.class)))
			.thenReturn(Collections.singletonMap(Version.REVISION_KEY, "-12"));

		configCommands.onApplicationEvent(null);

		final String targetResult = configCommands.target("http://localhost:9393", null, null, false);
		assertThat(targetResult, containsString("Incompatible version of Data Flow server detected"));
	}

}
