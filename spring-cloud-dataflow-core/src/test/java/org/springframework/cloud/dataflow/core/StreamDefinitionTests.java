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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.dsl.ParseException;
import org.springframework.cloud.dataflow.core.dsl.StreamParser;
/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Patrick Peralta
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
class StreamDefinitionTests {

	StreamDefinitionService streamDefinitionService = new DefaultStreamDefinitionService();

	@Test
	void streamCreation() {
		StreamDefinition stream = new StreamDefinition("ticktock", "time | log");
		assertThat(this.streamDefinitionService.getAppDefinitions(stream)).hasSize(2);
		StreamAppDefinition time = this.streamDefinitionService.getAppDefinitions(stream).get(0);
		assertThat(time.getName()).isEqualTo("time");
		assertThat(time.getRegisteredAppName()).isEqualTo("time");
		assertThat(time.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "ticktock.time");
		assertThat(time.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS, "ticktock");
		assertThat(time.getProperties()).doesNotContainKey(BindingPropertyKeys.INPUT_DESTINATION);

		StreamAppDefinition log = this.streamDefinitionService.getAppDefinitions(stream).get(1);
		assertThat(log.getName()).isEqualTo("log");
		assertThat(log.getRegisteredAppName()).isEqualTo("log");
		assertThat(log.getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "ticktock.time");
		assertThat(log.getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "ticktock");
		assertThat(log.getProperties()).doesNotContainKey(BindingPropertyKeys.OUTPUT_DESTINATION);
	}

	@Test
	void longRunningNonStreamApps() {
		StreamDefinition sd = new StreamDefinition("something","aaa");
		assertThat(this.streamDefinitionService.getAppDefinitions(sd).get(0).getApplicationType()).isEqualTo(ApplicationType.app);
		sd = new StreamDefinition("something","aaa|| bbb");
		assertThat(this.streamDefinitionService.getAppDefinitions(sd).get(0).getApplicationType()).isEqualTo(ApplicationType.app);
		assertThat(this.streamDefinitionService.getAppDefinitions(sd).get(1).getApplicationType()).isEqualTo(ApplicationType.app);
		sd = new StreamDefinition("something","aaa --aaa=bbb || bbb");
		assertThat(this.streamDefinitionService.getAppDefinitions(sd).get(0).getApplicationType()).isEqualTo(ApplicationType.app);
		assertThat(this.streamDefinitionService.getAppDefinitions(sd).get(1).getApplicationType()).isEqualTo(ApplicationType.app);
	}

	@Test
	void simpleStream() {
		StreamDefinition streamDefinition = new StreamDefinition("test", "foo | bar");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertThat(requests).hasSize(2);
		StreamAppDefinition source = requests.get(0);
		StreamAppDefinition sink = requests.get(1);
		assertThat(source.getName()).isEqualTo("foo");
		assertThat(source.getStreamName()).isEqualTo("test");

		assertThat(source.getProperties()).hasSize(2);
		assertThat(source.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "test.foo");
		assertThat(source.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS, "test");
		assertThat(sink.getName()).isEqualTo("bar");
		assertThat(sink.getStreamName()).isEqualTo("test");
		assertThat(sink.getProperties()).hasSize(2);
		assertThat(sink.getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "test.foo");
		assertThat(sink.getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "test");
	}

	@Test
	void quotesInParams() {
		StreamDefinition streamDefinition = new StreamDefinition("test",
				"foo --bar='payload.matches(''hello'')' | " + "file");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertThat(requests).hasSize(2);
		StreamAppDefinition source = requests.get(0);
		assertThat(source.getName()).isEqualTo("foo");
		assertThat(source.getStreamName()).isEqualTo("test");
		Map<String, String> sourceParameters = source.getProperties();
		assertThat(sourceParameters)
				.hasSize(3)
				.containsEntry("bar", "payload.matches('hello')");
	}

	@Test
	void quotesInParams2() {
		StreamDefinition streamDefinition = new StreamDefinition("test",
				"http --port=9700 | filter --expression=payload.matches('hello world') | file");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertThat(requests).hasSize(3);
		StreamAppDefinition filter = requests.get(1);
		assertThat(filter.getName()).isEqualTo("filter");
		assertThat(filter.getStreamName()).isEqualTo("test");
		Map<String, String> filterParameters = filter.getProperties();
		assertThat(filterParameters)
				.hasSize(5)
				.containsEntry("expression", "payload.matches('hello world')");
	}

	@Test
	void parameterizedApps() {
		StreamDefinition streamDefinition = new StreamDefinition("test", "foo --x=1 --y=two | bar --z=3");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertThat(requests).hasSize(2);
		StreamAppDefinition source = requests.get(0);
		StreamAppDefinition sink = requests.get(1);
		assertThat(source.getName()).isEqualTo("foo");
		assertThat(source.getStreamName()).isEqualTo("test");
		assertThat(source.getApplicationType()).isEqualTo(ApplicationType.source);
		Map<String, String> sourceParameters = source.getProperties();
		assertThat(sourceParameters)
				.hasSize(4)
				.containsEntry("x", "1")
				.containsEntry("y", "two");
		assertThat(sink.getName()).isEqualTo("bar");
		assertThat(sink.getStreamName()).isEqualTo("test");
		Map<String, String> sinkParameters = sink.getProperties();
		assertThat(sinkParameters)
				.hasSize(3)
				.containsEntry("z", "3");
		assertThat(sink.getApplicationType()).isEqualTo(ApplicationType.sink);
	}

	@Test
	void sourceDestinationNameIsAppliedToSourceApp() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", ":foo > goo | blah | file");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertThat(requests).hasSize(3);
		assertThat(requests.get(0).getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "foo");
		assertThat(requests.get(0).getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "test");
		assertThat(requests.get(0).getApplicationType()).isEqualTo(ApplicationType.processor);
		assertThat(requests.get(1).getApplicationType()).isEqualTo(ApplicationType.processor);
		assertThat(requests.get(2).getApplicationType()).isEqualTo(ApplicationType.sink);
	}

	@Test
	void sinkDestinationNameIsAppliedToSinkApp() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", "boo | blah | aaak > :foo");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertThat(requests).hasSize(3);
		assertThat(requests.get(2).getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "foo");
	}

	@Test
	void simpleSinkDestination() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", "bart > :foo");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertThat(requests).hasSize(1);
		assertThat(requests.get(0).getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "foo");
	}

	@Test
	void appWithBadDestination() throws Exception {
		boolean isException = false;
		try {
			new StreamParser("test", "app > foo").parse();
		}
		catch (Exception e) {
			isException = true;
		}
		assertThat(isException).isTrue();
	}

	@Test
	void simpleSourceDestination() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", ":foo > boot");
		List<StreamAppDefinition> requests = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		assertThat(requests).hasSize(1);
		assertThat(requests.get(0).getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "foo");
		assertThat(requests.get(0).getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "test");
	}

	@Test
	void destinationsForbiddenInComposedApps() {
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
	void bindings2Apps() {
		StreamDefinition streamDefinition = new StreamDefinition("ticktock", "time | log");
		List<StreamAppDefinition> apps = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		StreamAppDefinition source = apps.get(0);
		StreamAppDefinition sink = apps.get(1);
		assertThat(source.getRegisteredAppName()).isEqualTo("time");
		assertThat(source.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "ticktock.time");
		assertThat(source.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS, "ticktock");
		assertThat(source.getProperties().containsKey(BindingPropertyKeys.INPUT_DESTINATION)).isFalse();
		assertThat(sink.getRegisteredAppName()).isEqualTo("log");
		assertThat(sink.getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "ticktock.time");
		assertThat(sink.getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "ticktock");
		assertThat(sink.getProperties()).doesNotContainKey(BindingPropertyKeys.OUTPUT_DESTINATION);
	}

	@Test
	void bindings3Apps() {
		StreamDefinition streamDefinition = new StreamDefinition("ticktock", "time | filter |log");
		List<StreamAppDefinition> apps = this.streamDefinitionService.getAppDefinitions(streamDefinition);

		StreamAppDefinition source = apps.get(0);
		StreamAppDefinition processor = apps.get(1);
		StreamAppDefinition sink = apps.get(2);

		assertThat(source.getRegisteredAppName()).isEqualTo("time");
		assertThat(source.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "ticktock.time");
		assertThat(source.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS, "ticktock");
		assertThat(source.getProperties()).doesNotContainKey(BindingPropertyKeys.INPUT_DESTINATION);

		assertThat(processor.getRegisteredAppName()).isEqualTo("filter");
		assertThat(processor.getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "ticktock.time");
		assertThat(processor.getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "ticktock");
		assertThat(processor.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "ticktock.filter");
		assertThat(processor.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS, "ticktock");

		assertThat(sink.getRegisteredAppName()).isEqualTo("log");
		assertThat(sink.getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "ticktock.filter");
		assertThat(sink.getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "ticktock");
		assertThat(sink.getProperties()).doesNotContainKey(BindingPropertyKeys.OUTPUT_DESTINATION);
	}
}
