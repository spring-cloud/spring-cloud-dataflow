package org.springframework.cloud.dataflow.rest.client.dsl;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.StreamOperations;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vinicius Carvalho
 */
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
		Mockito.when(client.streamOperations()).thenReturn(this.streamOperations);
	}

	@Test
	public void simpleDefinition() throws Exception {
		StreamApplication time = new StreamApplication("time");
		StreamApplication log = new StreamApplication("log");
		Stream stream = Stream.builder(client).name("foo").source(time).sink(log)
				.create().deploy();
		assertThat("time | log").isEqualTo(stream.getDefinition());
	}

	@Test
	public void definitionWithLabel() throws Exception {
		StreamApplication time = new StreamApplication("time").label("tick");
		StreamApplication log = new StreamApplication("log");

		Stream stream = Stream.builder(client).name("foo").source(time).sink(log)
				.create().deploy();
		assertThat("tick: time | log").isEqualTo(stream.getDefinition());
	}

	@Test
	public void definitionWithProcessor() throws Exception {
		StreamApplication time = new StreamApplication("time").label("tick");
		StreamApplication filter = new StreamApplication("filter");
		StreamApplication log = new StreamApplication("log");
		Stream stream = Stream.builder(client).name("foo").source(time).processor(filter)
				.sink(log).create().deploy();
		assertThat("tick: time | filter | log").isEqualTo(stream.getDefinition());
	}

	@Test
	public void definitionWithProperties() throws Exception {
		StreamApplication time = new StreamApplication("time").label("tick")
				.addProperty("fixed-delay", 5000);
		StreamApplication log = new StreamApplication("log");
		Stream stream = Stream.builder(client).name("foo").source(time).sink(log)
				.create().deploy();
		assertThat("tick: time --fixed-delay=5000 | log")
				.isEqualTo(stream.getDefinition());
	}

	@Test
	public void definitionWithDeploymentProperties() throws Exception {
		StreamApplication time = new StreamApplication("time").label("tick")
				.addProperty("fixed-delay", "5000").addDeploymentProperty("count", 2);

		Map<String, Object> deploymentProperties = time.getDeploymentProperties();
		assertThat(deploymentProperties.get("deployer.tick.count")).isEqualTo(2);
	}

	@Test
	public void deployWithCreate() throws Exception {
		StreamDefinitionResource resource = new StreamDefinitionResource("ticktock",
				"time | log");
		resource.setStatus("deploying");
		Mockito.when(streamOperations.createStream(Mockito.anyString(),
				Mockito.anyString(), Mockito.anyBoolean())).thenReturn(resource);
		StreamApplication time = new StreamApplication("time");
		StreamApplication log = new StreamApplication("log");
		Stream stream = Stream.builder(client).name("ticktock").source(time).sink(log)
				.create().deploy();
		Mockito.verify(streamOperations, Mockito.times(1)).createStream(
				Mockito.eq("ticktock"), Mockito.eq("time | log"), Mockito.eq(false));
		Mockito.verify(streamOperations, Mockito.times(1)).deploy(Mockito.eq("ticktock"),
				Mockito.anyMap());
	}

	@Test
	public void deployWithDefinition() throws Exception {
		StreamDefinitionResource resource = new StreamDefinitionResource("ticktock",
				"time | log");
		resource.setStatus("deploying");
		Mockito.when(streamOperations.createStream(Mockito.anyString(),
				Mockito.anyString(), Mockito.anyBoolean())).thenReturn(resource);

		Stream stream = Stream.builder(client).name("ticktock").definition("time | log")
				.create().deploy(Collections.singletonMap("deployer.log.count", "2"));
		Mockito.verify(streamOperations, Mockito.times(1)).createStream(
				Mockito.eq("ticktock"), Mockito.eq("time | log"), Mockito.eq(false));
		Mockito.verify(streamOperations, Mockito.times(1)).deploy(Mockito.eq("ticktock"),
				Mockito.eq(Collections.singletonMap("deployer.log.count", "2")));

	}

	@Test
	public void getStatus() throws Exception {
		StreamDefinitionResource resource = new StreamDefinitionResource("ticktock",
				"time | log");
		resource.setStatus("unknown");
		Mockito.when(streamOperations.getStreamDefinition(Mockito.eq("ticktock")))
				.thenReturn(resource);
		Mockito.when(streamOperations.createStream(Mockito.anyString(),
				Mockito.anyString(), Mockito.anyBoolean())).thenReturn(resource);
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
				resource.setStatus("deploying");
				return null;
			}
		}).when(streamOperations).deploy(Mockito.eq("ticktock"), Mockito.anyMap());

		StreamDefinition streamDefinition = Stream.builder(client).name("ticktock").definition("time | log")
				.create();
		Mockito.verify(streamOperations, Mockito.times(1)).createStream(
				Mockito.eq("ticktock"), Mockito.eq("time | log"), Mockito.eq(false));
		Stream stream = streamDefinition.deploy();
		assertThat("deploying").isEqualTo(stream.getStatus());
	}

	@Test
	public void createStream() throws Exception {
		StreamDefinitionResource resource = new StreamDefinitionResource("ticktock",
				"time | log");
		resource.setStatus("deploying");
		Mockito.when(streamOperations.createStream(Mockito.anyString(),
				Mockito.anyString(), Mockito.anyBoolean())).thenReturn(resource);
		StreamApplication time = new StreamApplication("time");
		StreamApplication log = new StreamApplication("log");
		Stream.builder(client).name("ticktock").source(time).sink(log)
				.create();
		Mockito.verify(streamOperations, Mockito.times(1)).createStream(
				Mockito.eq("ticktock"), Mockito.eq("time | log"), Mockito.eq(false));
	}

	@Test
	public void testDuplicateNameWithLabel() throws Exception {
		StreamApplication filter2 = new StreamApplication("filter").label("filter2");
		Stream.builder(client).name("test").source(timeApplication).processor(filterApplication).processor(filter2)
				.sink(logApplication).create();
		Mockito.verify(streamOperations, Mockito.times(1)).createStream(
				Mockito.eq("test"), Mockito.eq("time | filter | filter2: filter | log"), Mockito.eq(false));
	}

	@Test(expected = IllegalStateException.class)
	public void testDuplicateNameNoLabel() throws Exception {
		Stream.builder(client).name("test").source(timeApplication).processor(filterApplication)
				.processor(filterApplication).sink(logApplication).create();
	}

	@Test
	public void undeploy() throws Exception {
		StreamDefinitionResource resource = new StreamDefinitionResource("ticktock",
				"time | log");
		resource.setStatus("deploying");
		Mockito.when(streamOperations.createStream(Mockito.anyString(),
				Mockito.anyString(), Mockito.anyBoolean())).thenReturn(resource);
		StreamApplication time = new StreamApplication("time");
		StreamApplication log = new StreamApplication("log");

		Stream stream = Stream.builder(client).name("ticktock").source(time).sink(log)
				.create().deploy();
		Mockito.verify(streamOperations, Mockito.times(1)).createStream(
				Mockito.eq("ticktock"), Mockito.eq("time | log"), Mockito.eq(false));
		Mockito.verify(streamOperations, Mockito.times(1)).deploy(Mockito.eq("ticktock"),
				Mockito.anyMap());
		stream.undeploy();
		Mockito.verify(streamOperations, Mockito.times(1))
				.undeploy(Mockito.eq("ticktock"));
	}

	@Test
	public void destroy() throws Exception {
		StreamDefinitionResource resource = new StreamDefinitionResource("ticktock",
				"time | log");
		resource.setStatus("deploying");
		Mockito.when(streamOperations.createStream(Mockito.anyString(),
				Mockito.anyString(), Mockito.anyBoolean())).thenReturn(resource);
		StreamApplication time = new StreamApplication("time");
		StreamApplication log = new StreamApplication("log");
		Stream stream = Stream.builder(client).name("ticktock").source(time).sink(log)
				.create().deploy();
		Mockito.verify(streamOperations, Mockito.times(1)).createStream(
				Mockito.eq("ticktock"), Mockito.eq("time | log"), Mockito.eq(false));
		Mockito.verify(streamOperations, Mockito.times(1)).deploy(Mockito.eq("ticktock"),
				Mockito.anyMap());
		stream.destroy();
		Mockito.verify(streamOperations, Mockito.times(1))
				.destroy(Mockito.eq("ticktock"));
	}
}
