/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.cloud.dataflow.core.dsl.ParseException;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Patrick Peralta
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 */
public class StreamDefinitionTests {

	@Test
	public void testStreamCreation() {
		StreamDefinition stream = new StreamDefinition("ticktock", "time | log");
		assertEquals(2, stream.getModuleDefinitions().size());
		ModuleDefinition time = stream.getModuleDefinitions().get(0);
		assertEquals("time", time.getName());
		assertEquals("time", time.getLabel());
		assertEquals("ticktock.time", time.getParameters().get(BindingProperties.OUTPUT_BINDING_KEY));
		assertFalse(time.getParameters().containsKey(BindingProperties.INPUT_BINDING_KEY));

		ModuleDefinition log = stream.getModuleDefinitions().get(1);
		assertEquals("log", log.getName());
		assertEquals("log", log.getLabel());
		assertEquals("ticktock.time", log.getParameters().get(BindingProperties.INPUT_BINDING_KEY));
		assertEquals("default", log.getParameters().get(BindingProperties.INPUT_GROUP_KEY));
		assertEquals("true", log.getParameters().get(BindingProperties.INPUT_DURABLE_SUBSCRIPTION_KEY));
		assertFalse(log.getParameters().containsKey(BindingProperties.OUTPUT_BINDING_KEY));
	}

	@Test
	public void simpleStream() {
		StreamDefinition streamDefinition = new StreamDefinition("test", "foo | bar");
		List<ModuleDefinition> requests = streamDefinition.getModuleDefinitions(); 
		assertEquals(2, requests.size());
		ModuleDefinition source = requests.get(0);
		ModuleDefinition sink = requests.get(1);
		assertEquals("foo", source.getName());
		assertEquals("test", source.getGroup());

		assertEquals(1, source.getParameters().size());
		assertEquals("test.foo", source.getParameters().get(BindingProperties.OUTPUT_BINDING_KEY));
		assertEquals("bar", sink.getName());
		assertEquals("test", sink.getGroup());
		assertEquals(3, sink.getParameters().size());
		assertEquals("test.foo", sink.getParameters().get(BindingProperties.INPUT_BINDING_KEY));
		assertEquals("default", sink.getParameters().get(BindingProperties.INPUT_GROUP_KEY));
		assertEquals("true", sink.getParameters().get(BindingProperties.INPUT_DURABLE_SUBSCRIPTION_KEY));
	}

	@Test
	public void quotesInParams() {
		StreamDefinition streamDefinition = new StreamDefinition("test", "foo --bar='payload.matches(''hello'')' | file");
		List<ModuleDefinition> requests = streamDefinition.getModuleDefinitions();
		assertEquals(2, requests.size());
		ModuleDefinition source = requests.get(0);
		assertEquals("foo", source.getName());
		assertEquals("test", source.getGroup());
		Map<String, String> sourceParameters = source.getParameters();
		assertEquals(2, sourceParameters.size());
		assertEquals("payload.matches('hello')", sourceParameters.get("bar"));
	}

	@Test
	public void quotesInParams2() {
		StreamDefinition streamDefinition = new StreamDefinition("test",
				"http --port=9700 | filter --expression=payload.matches('hello world') | file");
		List<ModuleDefinition> requests = streamDefinition.getModuleDefinitions();
		assertEquals(3, requests.size());
		ModuleDefinition filter = requests.get(1);
		assertEquals("filter", filter.getName());
		assertEquals("test", filter.getGroup());
		Map<String, String> filterParameters = filter.getParameters();
		assertEquals(5, filterParameters.size());
		assertEquals("payload.matches('hello world')", filterParameters.get("expression"));
	}

	@Test
	public void parameterizedModules() {
		StreamDefinition streamDefinition = new StreamDefinition("test", "foo --x=1 --y=two | bar --z=3");
		List<ModuleDefinition> requests = streamDefinition.getModuleDefinitions();
		assertEquals(2, requests.size());
		ModuleDefinition source = requests.get(0);
		ModuleDefinition sink = requests.get(1);
		assertEquals("foo", source.getName());
		assertEquals("test", source.getGroup());
		Map<String, String> sourceParameters = source.getParameters();
		assertEquals(3, sourceParameters.size());
		assertEquals("1", sourceParameters.get("x"));
		assertEquals("two", sourceParameters.get("y"));
		assertEquals("bar", sink.getName());
		assertEquals("test", sink.getGroup());
		Map<String, String> sinkParameters = sink.getParameters();
		assertEquals(4, sinkParameters.size());
		assertEquals("3", sinkParameters.get("z"));
	}

	@Test
	public void sourceDestinationNameIsAppliedToSourceModule() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", ":foo > goo | blah | file");
		List<ModuleDefinition> requests = streamDefinition.getModuleDefinitions();
		assertEquals(3, requests.size());
		assertEquals("foo", requests.get(0).getParameters().get(BindingProperties.INPUT_BINDING_KEY));
		assertEquals("test", requests.get(0).getParameters().get(BindingProperties.INPUT_GROUP_KEY));
	}

	@Test
	public void sinkDestinationNameIsAppliedToSinkModule() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", "boo | blah | aaak > :foo");
		List<ModuleDefinition> requests = streamDefinition.getModuleDefinitions();
		assertEquals(3, requests.size());
		assertEquals("foo", requests.get(2).getParameters().get(BindingProperties.OUTPUT_BINDING_KEY));
	}

	@Test
	public void simpleSinkDestination() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", "bart > :foo");
		List<ModuleDefinition> requests = streamDefinition.getModuleDefinitions();
		assertEquals(1, requests.size());
		assertEquals("foo", requests.get(0).getParameters().get(BindingProperties.OUTPUT_BINDING_KEY));
	}

	@Test
	public void moduleWithBadDestination() throws Exception {
		boolean isException = false;
		try {
			new StreamDefinition("test", "module > foo");
		}
		catch (Exception e) {
			isException = true;
		}
		assertTrue(isException);
	}

	@Test
	public void simpleSourceDestination() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", ":foo > boot");
		List<ModuleDefinition> requests = streamDefinition.getModuleDefinitions();
		assertEquals(1, requests.size());
		assertEquals("foo", requests.get(0).getParameters().get(BindingProperties.INPUT_BINDING_KEY));
		assertEquals("test", requests.get(0).getParameters().get(BindingProperties.INPUT_GROUP_KEY));
		assertEquals("true", requests.get(0).getParameters().get(BindingProperties.INPUT_DURABLE_SUBSCRIPTION_KEY));
	}

	@Test
	public void destinationsForbiddenInComposedModules() {
		try {
			new StreamDefinition("test", ":foo > boot");
		}
		catch (ParseException expected) {
			assertThat(expected.getMessage(),
					containsString("A destination is not supported in this kind of definition"));
			assertThat(expected.getPosition(), is(0));
		}
		try {
			new StreamDefinition("test", "bart | goo > :foo");
		}
		catch (ParseException expected) {
			assertThat(expected.getMessage(),
					containsString("A destination is not supported in this kind of definition"));
			assertThat(expected.getPosition(), is(13));
		}
	}

	@Test
	public void testBindings2Modules() {
		StreamDefinition streamDefinition = new StreamDefinition("ticktock", "time | log");
		List<ModuleDefinition> modules = streamDefinition.getModuleDefinitions();
		ModuleDefinition source = modules.get(0);
		ModuleDefinition sink = modules.get(1);
		assertEquals("time", source.getLabel());
		assertEquals("ticktock.time", source.getParameters().get(BindingProperties.OUTPUT_BINDING_KEY));
		assertFalse(source.getParameters().containsKey(BindingProperties.INPUT_BINDING_KEY));
		assertEquals("log", sink.getLabel());
		assertEquals("ticktock.time", sink.getParameters().get(BindingProperties.INPUT_BINDING_KEY));
		assertEquals("default", sink.getParameters().get(BindingProperties.INPUT_GROUP_KEY));
		assertEquals("true", sink.getParameters().get(BindingProperties.INPUT_DURABLE_SUBSCRIPTION_KEY));
		assertFalse(sink.getParameters().containsKey(BindingProperties.OUTPUT_BINDING_KEY));
	}

	@Test
	public void testBindings3Modules() {
		StreamDefinition streamDefinition = new StreamDefinition("ticktock", "time | filter |log");
		List<ModuleDefinition> modules = streamDefinition.getModuleDefinitions();

		ModuleDefinition source = modules.get(0);
		ModuleDefinition processor = modules.get(1);
		ModuleDefinition sink = modules.get(2);

		assertEquals("time", source.getLabel());
		assertEquals("ticktock.time", source.getParameters().get(BindingProperties.OUTPUT_BINDING_KEY));
		assertFalse(source.getParameters().containsKey(BindingProperties.INPUT_BINDING_KEY));

		assertEquals("filter", processor.getLabel());
		assertEquals("ticktock.time", processor.getParameters().get(BindingProperties.INPUT_BINDING_KEY));
		assertEquals("default", processor.getParameters().get(BindingProperties.INPUT_GROUP_KEY));
		assertEquals("true", processor.getParameters().get(BindingProperties.INPUT_DURABLE_SUBSCRIPTION_KEY));
		assertEquals("ticktock.filter", processor.getParameters().get(BindingProperties.OUTPUT_BINDING_KEY));

		assertEquals("log", sink.getLabel());
		assertEquals("ticktock.filter", sink.getParameters().get(BindingProperties.INPUT_BINDING_KEY));
		assertEquals("default", sink.getParameters().get(BindingProperties.INPUT_GROUP_KEY));
		assertEquals("true", sink.getParameters().get(BindingProperties.INPUT_DURABLE_SUBSCRIPTION_KEY));
		assertFalse(sink.getParameters().containsKey(BindingProperties.OUTPUT_BINDING_KEY));
	}

}