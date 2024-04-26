/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

import java.util.List;
import java.util.Map;


import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.dsl.ParseException;
import org.springframework.cloud.dataflow.core.dsl.StreamParser;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Patrick Peralta
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 */
public class StreamDefinitionTests {

	StreamDefinitionService streamDefinitionService = new DefaultStreamDefinitionService();

	@Test
	public void testStreamCreation() {
		StreamDefinition stream = new StreamDefinition("ticktock", "time | log");
		assertEquals(2, this.streamDefinitionService.getAppDefinitions(stream).size());
		StreamAppDefinition time = this.streamDefinitionService.getAppDefinitions(stream).get(0);
		assertEquals("time", time.getName());
		assertEquals("time", time.getRegisteredAppName());
		assertEquals("ticktock.time", time.getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("ticktock", time.getProperties().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));
		assertFalse(time.getProperties().containsKey(BindingPropertyKeys.INPUT_DESTINATION));

		StreamAppDefinition log = this.streamDefinitionService.getAppDefinitions(stream).get(1);
		assertEquals("log", log.getName());
		assertEquals("log", log.getRegisteredAppName());
		assertEquals("ticktock.time", log.getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("ticktock", log.getProperties().get(BindingPropertyKeys.INPUT_GROUP));
		assertFalse(log.getProperties().containsKey(BindingPropertyKeys.OUTPUT_DESTINATION));
	}
	
	@Test
	public void testLongRunningNonStreamApps() {
		StreamDefinition sd = new StreamDefinition("something","aaa");
		assertEquals(ApplicationType.app, this.streamDefinitionService.getAppDefinitions(sd).get(0).getApplicationType());
		sd = new StreamDefinition("something","aaa|| bbb");
		assertEquals(ApplicationType.app, this.streamDefinitionService.getAppDefinitions(sd).get(0).getApplicationType());
		assertEquals(ApplicationType.app, this.streamDefinitionService.getAppDefinitions(sd).get(1).getApplicationType());
		sd = new StreamDefinition("something","aaa --aaa=bbb || bbb");
		assertEquals(ApplicationType.app, this.streamDefinitionService.getAppDefinitions(sd).get(0).getApplicationType());
		assertEquals(ApplicationType.app, this.streamDefinitionService.getAppDefinitions(sd).get(1).getApplicationType());
	}

	@Test
	public void simpleStream() {
		StreamDefinition streamDefinition = new StreamDefinition("test", "foo | bar");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertEquals(2, requests.size());
		StreamAppDefinition source = requests.get(0);
		StreamAppDefinition sink = requests.get(1);
		assertEquals("foo", source.getName());
		assertEquals("test", source.getStreamName());

		assertEquals(2, source.getProperties().size());
		assertEquals("test.foo", source.getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("test", source.getProperties().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));
		assertEquals("bar", sink.getName());
		assertEquals("test", sink.getStreamName());
		assertEquals(2, sink.getProperties().size());
		assertEquals("test.foo", sink.getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("test", sink.getProperties().get(BindingPropertyKeys.INPUT_GROUP));
	}

	@Test
	public void quotesInParams() {
		StreamDefinition streamDefinition = new StreamDefinition("test",
				"foo --bar='payload.matches(''hello'')' | " + "file");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertEquals(2, requests.size());
		StreamAppDefinition source = requests.get(0);
		assertEquals("foo", source.getName());
		assertEquals("test", source.getStreamName());
		Map<String, String> sourceParameters = source.getProperties();
		assertEquals(3, sourceParameters.size());
		assertEquals("payload.matches('hello')", sourceParameters.get("bar"));
	}

	@Test
	public void quotesInParams2() {
		StreamDefinition streamDefinition = new StreamDefinition("test",
				"http --port=9700 | filter --expression=payload.matches('hello world') | file");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertEquals(3, requests.size());
		StreamAppDefinition filter = requests.get(1);
		assertEquals("filter", filter.getName());
		assertEquals("test", filter.getStreamName());
		Map<String, String> filterParameters = filter.getProperties();
		assertEquals(5, filterParameters.size());
		assertEquals("payload.matches('hello world')", filterParameters.get("expression"));
	}

	@Test
	public void parameterizedApps() {
		StreamDefinition streamDefinition = new StreamDefinition("test", "foo --x=1 --y=two | bar --z=3");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertEquals(2, requests.size());
		StreamAppDefinition source = requests.get(0);
		StreamAppDefinition sink = requests.get(1);
		assertEquals("foo", source.getName());
		assertEquals("test", source.getStreamName());
		assertEquals(ApplicationType.source, source.getApplicationType());
		Map<String, String> sourceParameters = source.getProperties();
		assertEquals(4, sourceParameters.size());
		assertEquals("1", sourceParameters.get("x"));
		assertEquals("two", sourceParameters.get("y"));
		assertEquals("bar", sink.getName());
		assertEquals("test", sink.getStreamName());
		Map<String, String> sinkParameters = sink.getProperties();
		assertEquals(3, sinkParameters.size());
		assertEquals("3", sinkParameters.get("z"));
		assertEquals(ApplicationType.sink, sink.getApplicationType());
	}

	@Test
	public void sourceDestinationNameIsAppliedToSourceApp() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", ":foo > goo | blah | file");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertEquals(3, requests.size());
		assertEquals("foo", requests.get(0).getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("test", requests.get(0).getProperties().get(BindingPropertyKeys.INPUT_GROUP));
		assertEquals(ApplicationType.processor, requests.get(0).getApplicationType());
		assertEquals(ApplicationType.processor, requests.get(1).getApplicationType());
		assertEquals(ApplicationType.sink, requests.get(2).getApplicationType());
	}

	@Test
	public void sinkDestinationNameIsAppliedToSinkApp() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", "boo | blah | aaak > :foo");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertEquals(3, requests.size());
		assertEquals("foo", requests.get(2).getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));
	}

	@Test
	public void simpleSinkDestination() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", "bart > :foo");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertEquals(1, requests.size());
		assertEquals("foo", requests.get(0).getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));
	}

	@Test
	public void appWithBadDestination() throws Exception {
		boolean isException = false;
		try {
			new StreamParser("test", "app > foo").parse();
		}
		catch (Exception e) {
			isException = true;
		}
		assertTrue(isException);
	}

	@Test
	public void simpleSourceDestination() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", ":foo > boot");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertEquals(1, requests.size());
		assertEquals("foo", requests.get(0).getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("test", requests.get(0).getProperties().get(BindingPropertyKeys.INPUT_GROUP));
	}

	@Test
	public void destinationsForbiddenInComposedApps() {
		try {
			new StreamDefinition("test", ":foo > boot");
		}
		catch (ParseException expected) {
			assertThat(expected.getMessage()).contains("A destination is not supported in this kind of definition");
			assertThat(expected.getPosition()).isEqualTo(0);
		}
		try {
			new StreamDefinition("test", "bart | goo > :foo");
		}
		catch (ParseException expected) {
			assertThat(expected.getMessage()).contains("A destination is not supported in this kind of definition");
			assertThat(expected.getPosition()).isEqualTo(13);
		}
	}

	@Test
	public void testBindings2Apps() {
		StreamDefinition streamDefinition = new StreamDefinition("ticktock", "time | log");
		List<StreamAppDefinition> apps = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		StreamAppDefinition source = apps.get(0);
		StreamAppDefinition sink = apps.get(1);
		assertEquals("time", source.getRegisteredAppName());
		assertEquals("ticktock.time", source.getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("ticktock", source.getProperties().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));
		assertFalse(source.getProperties().containsKey(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("log", sink.getRegisteredAppName());
		assertEquals("ticktock.time", sink.getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("ticktock", sink.getProperties().get(BindingPropertyKeys.INPUT_GROUP));
		assertFalse(sink.getProperties().containsKey(BindingPropertyKeys.OUTPUT_DESTINATION));
	}

	@Test
	public void testBindings3Apps() {
		StreamDefinition streamDefinition = new StreamDefinition("ticktock", "time | filter |log");
		List<StreamAppDefinition> apps = this.streamDefinitionService.getAppDefinitions(streamDefinition);

		StreamAppDefinition source = apps.get(0);
		StreamAppDefinition processor = apps.get(1);
		StreamAppDefinition sink = apps.get(2);

		assertEquals("time", source.getRegisteredAppName());
		assertEquals("ticktock.time", source.getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("ticktock", source.getProperties().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));
		assertFalse(source.getProperties().containsKey(BindingPropertyKeys.INPUT_DESTINATION));

		assertEquals("filter", processor.getRegisteredAppName());
		assertEquals("ticktock.time", processor.getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("ticktock", processor.getProperties().get(BindingPropertyKeys.INPUT_GROUP));
		assertEquals("ticktock.filter", processor.getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("ticktock", processor.getProperties().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));

		assertEquals("log", sink.getRegisteredAppName());
		assertEquals("ticktock.filter", sink.getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("ticktock", sink.getProperties().get(BindingPropertyKeys.INPUT_GROUP));
		assertFalse(sink.getProperties().containsKey(BindingPropertyKeys.OUTPUT_DESTINATION));
	}
}
