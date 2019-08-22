/*
 * Copyright 2016-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.rest.client.dsl;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import org.springframework.cloud.dataflow.rest.SkipperStream;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.StreamOperations;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.skipper.domain.PackageIdentifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Vinicius Carvalho
 * @author Christian Tzolov
 */
@SuppressWarnings("unchecked")
public class StreamDslTests {

	@Mock
	private DataFlowOperations client;

	@Mock
	private StreamOperations streamOperations;

	private StreamApplication timeApplication = new StreamApplication("time");

	private StreamApplication filterApplication = new StreamApplication("filter");

	private StreamApplication logApplication = new StreamApplication("log");

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
		when(client.streamOperations()).thenReturn(this.streamOperations);
	}

	@Test
	public void simpleDefinition() {
		StreamApplication time = new StreamApplication("time");
		StreamApplication log = new StreamApplication("log");
		Stream stream = Stream.builder(client).name("foo").source(time).sink(log).create()
				.deploy();
		assertThat("time | log").isEqualTo(stream.getDefinition());
	}

	@Test
	public void definitionWithLabel() {
		StreamApplication time = new StreamApplication("time").label("tick");
		StreamApplication log = new StreamApplication("log");

		Stream stream = Stream.builder(client).name("foo").source(time).sink(log).create()
				.deploy();
		assertThat("tick: time | log").isEqualTo(stream.getDefinition());
	}

	@Test
	public void definitionWithProcessor() {
		StreamApplication time = new StreamApplication("time").label("tick");
		StreamApplication filter = new StreamApplication("filter");
		StreamApplication log = new StreamApplication("log");
		Stream stream = Stream.builder(client).name("foo").source(time).processor(filter)
				.sink(log).create().deploy();
		assertThat("tick: time | filter | log").isEqualTo(stream.getDefinition());
	}

	@Test
	public void definitionWithProperties() {
		StreamApplication time = new StreamApplication("time").label("tick")
				.addProperty("fixed-delay", 5000);
		StreamApplication log = new StreamApplication("log");
		Stream stream = Stream.builder(client).name("foo").source(time).sink(log).create()
				.deploy();
		assertThat("tick: time --fixed-delay=5000 | log")
				.isEqualTo(stream.getDefinition());
	}

	@Test
	public void definitionWithDeploymentProperties() {
		StreamApplication time = new StreamApplication("time").label("tick")
				.addProperty("fixed-delay", "5000").addDeploymentProperty("count", 2);

		Map<String, Object> deploymentProperties = time.getDeploymentProperties();
		assertThat(deploymentProperties.get("deployer.tick.count")).isEqualTo(2);
	}

	@Test
	public void definitionWithDeploymentPropertiesBuilder() {
		StreamDefinitionResource resource = new StreamDefinitionResource("ticktock",
				"tick: time | log", "demo stream");
		resource.setStatus("deploying");
		when(streamOperations.createStream(anyString(),
				anyString(), anyString(), anyBoolean())).thenReturn(resource);
		SkipperDeploymentPropertiesBuilder propertiesBuilder = new SkipperDeploymentPropertiesBuilder();
		Map<String, String> props = propertiesBuilder.count("tick", 2)
				.memory("tick", 2048)
				.packageVersion("1.0.0.RELEASE")
				.repoName("foo")
				.platformName("pcf").build();
		Stream.builder(client).name("ticktock").definition("tick: time | log").create()
				.deploy(props);
		ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		verify(streamOperations, times(1)).deploy(eq("ticktock"),
				mapArgumentCaptor.capture());
		assertThat(mapArgumentCaptor.getValue()).containsKeys("deployer.tick.count",
				"deployer.tick.memory", SkipperStream.SKIPPER_PLATFORM_NAME,
				SkipperStream.SKIPPER_PACKAGE_VERSION, SkipperStream.SKIPPER_REPO_NAME);
	}

	@Test
	public void deployWithCreate() {
		StreamDefinitionResource resource = new StreamDefinitionResource("ticktock",
				"time | log", "demo stream");
		resource.setStatus("deploying");
		when(streamOperations.createStream(anyString(),
				anyString(), anyString(), anyBoolean())).thenReturn(resource);
		StreamApplication time = new StreamApplication("time");
		StreamApplication log = new StreamApplication("log");
		Stream.builder(client).name("ticktock").source(time).sink(log).create().deploy();
		verify(streamOperations, times(1)).createStream(
				eq("ticktock"), eq("time | log"), eq("demo stream"), eq(false));
		verify(streamOperations, times(1)).deploy(eq("ticktock"),
				anyMap());
	}

	@Test
	public void deployWithDefinition() {
		StreamDefinitionResource resource = new StreamDefinitionResource("ticktock",
				"time | log", "demo stream");
		resource.setStatus("deploying");
		when(streamOperations.createStream(anyString(),
				anyString(), anyString(), anyBoolean())).thenReturn(resource);

		Stream.builder(client).name("ticktock").definition("time | log")
				.create().deploy(Collections.singletonMap("deployer.log.count", "2"));
		verify(streamOperations, times(1)).createStream(
				eq("ticktock"), eq("time | log"), eq("demo stream"), eq(false));
		verify(streamOperations, times(1)).deploy(eq("ticktock"),
				eq(Collections.singletonMap("deployer.log.count", "2")));

	}

	@Test
	public void getStatus() {
		StreamDefinitionResource resource = new StreamDefinitionResource("ticktock",
				"time | log", "demo stream");
		resource.setStatus("unknown");
		when(streamOperations.getStreamDefinition(eq("ticktock")))
				.thenReturn(resource);
		when(streamOperations.createStream(anyString(),
				anyString(), anyString(), anyBoolean())).thenReturn(resource);
		doAnswer((Answer<Void>) invocationOnMock -> {
			resource.setStatus("deploying");
			return null;
		}).when(streamOperations).deploy(eq("ticktock"), anyMap());

		StreamDefinition streamDefinition = Stream.builder(client).name("ticktock")
				.definition("time | log").create();
		verify(streamOperations, times(1)).createStream(
				eq("ticktock"), eq("time | log"), eq("demo stream"), eq(false));
		Stream stream = streamDefinition.deploy();
		assertThat("deploying").isEqualTo(stream.getStatus());
	}

	@Test
	public void createStream() {
		StreamDefinitionResource resource = new StreamDefinitionResource("ticktock",
				"time | log", "demo stream");
		resource.setStatus("deploying");
		when(streamOperations.createStream(anyString(),
				anyString(), anyString(), anyBoolean())).thenReturn(resource);
		StreamApplication time = new StreamApplication("time");
		StreamApplication log = new StreamApplication("log");
		Stream.builder(client).name("ticktock").source(time).sink(log).create();
		verify(streamOperations, times(1)).createStream(
				eq("ticktock"), eq("time | log"), eq("demo stream"), eq(false));
	}

	@Test
	public void testDuplicateNameWithLabel() {
		StreamApplication filter2 = new StreamApplication("filter").label("filter2");
		Stream.builder(client).name("test").source(timeApplication)
				.processor(filterApplication).processor(filter2).sink(logApplication)
				.create();
		verify(streamOperations, times(1)).createStream(
				eq("test"), eq("time | filter | filter2: filter | log"), eq(StringUtils.EMPTY),
				eq(false));
	}

	@Test(expected = IllegalStateException.class)
	public void testDuplicateNameNoLabel() {
		Stream.builder(client).name("test").source(timeApplication)
				.processor(filterApplication).processor(filterApplication)
				.sink(logApplication).create();
	}

	@Test
	public void update() {
		StreamDefinitionResource ticktockDefinition = new StreamDefinitionResource("ticktock", "time | log",
				"demo stream");
		ticktockDefinition.setStatus("deploying");
		when(streamOperations.createStream(anyString(), anyString(), anyString(), anyBoolean()))
				.thenReturn(ticktockDefinition);

		Stream stream = Stream.builder(client).name("ticktock").definition(ticktockDefinition.getDslText()).create()
				.deploy();

		when(streamOperations.info(eq("ticktock"))).thenReturn(new StreamDeploymentResource("ticktock", "time | log"));

		stream.update("app.log.log.expression='TIMESTAMP: '.concat(payload)");

		verify(streamOperations, times(1)).updateStream(eq("ticktock"),
				eq("ticktock"), isA(PackageIdentifier.class), isA(Map.class), eq(false), isNull());

		verify(streamOperations, times(1)).info(eq("ticktock"));
	}

	@Test
	public void rollback() {
		StreamDefinitionResource ticktockDefinition = new StreamDefinitionResource("ticktock", "time | log",
				"demo stream");
		ticktockDefinition.setStatus("deploying");
		when(streamOperations.createStream(anyString(), anyString(), anyString(), anyBoolean()))
				.thenReturn(ticktockDefinition);

		Stream stream = Stream.builder(client).name("ticktock").definition(ticktockDefinition.getDslText()).create()
				.deploy();

		when(streamOperations.info(eq("ticktock"))).thenReturn(new StreamDeploymentResource("ticktock", "time | log"));

		stream.rollback(666);

		verify(streamOperations, times(1)).rollbackStream(eq("ticktock"), eq(666));
		verify(streamOperations, times(1)).info(eq("ticktock"));
	}

	@Test
	public void manifest() {
		StreamDefinitionResource ticktockDefinition = new StreamDefinitionResource("ticktock", "time | log",
				"demo stream");
		ticktockDefinition.setStatus("deploying");
		when(streamOperations.createStream(anyString(), anyString(), anyString(), anyBoolean()))
				.thenReturn(ticktockDefinition);

		Stream stream = Stream.builder(client).name("ticktock").definition(ticktockDefinition.getDslText()).create()
				.deploy();

		stream.manifest(666);

		verify(streamOperations, times(1)).getManifest(eq("ticktock"), eq(666));
	}

	@Test
	public void history() {
		StreamDefinitionResource ticktockDefinition = new StreamDefinitionResource("ticktock", "time | log",
				"demo stream");
		ticktockDefinition.setStatus("deploying");
		when(streamOperations.createStream(anyString(), anyString(), anyString(), anyBoolean()))
				.thenReturn(ticktockDefinition);

		Stream stream = Stream.builder(client).name("ticktock").definition(ticktockDefinition.getDslText()).create()
				.deploy();

		stream.history();

		verify(streamOperations, times(1)).history(eq("ticktock"));
	}

	@Test
	public void undeploy() {
		StreamDefinitionResource resource = new StreamDefinitionResource("ticktock", "time | log", "demo stream");
		resource.setStatus("deploying");
		when(streamOperations.createStream(anyString(),
				anyString(), anyString(), anyBoolean())).thenReturn(resource);
		StreamApplication time = new StreamApplication("time");
		StreamApplication log = new StreamApplication("log");

		Stream stream = Stream.builder(client).name("ticktock").source(time).sink(log)
				.create().deploy();
		verify(streamOperations, times(1)).createStream(
				eq("ticktock"), eq("time | log"), eq("demo stream"), eq(false));
		verify(streamOperations, times(1)).deploy(eq("ticktock"),
				anyMap());
		stream.undeploy();
		verify(streamOperations, times(1))
				.undeploy(eq("ticktock"));
	}

	@Test
	public void destroy() {
		StreamDefinitionResource resource = new StreamDefinitionResource("ticktock",
				"time | log", "demo stream");
		resource.setStatus("deploying");
		when(streamOperations.createStream(anyString(),
				anyString(), anyString(), anyBoolean())).thenReturn(resource);
		StreamApplication time = new StreamApplication("time");
		StreamApplication log = new StreamApplication("log");
		Stream stream = Stream.builder(client).name("ticktock").source(time).sink(log)
				.create().deploy();
		verify(streamOperations, times(1)).createStream(
				eq("ticktock"), eq("time | log"), eq("demo stream"), eq(false));
		verify(streamOperations, times(1)).deploy(eq("ticktock"),
				anyMap());
		stream.destroy();
		verify(streamOperations, times(1))
				.destroy(eq("ticktock"));
	}
}
