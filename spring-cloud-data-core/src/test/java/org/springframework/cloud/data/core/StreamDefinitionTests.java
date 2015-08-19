/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.core;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.cloud.data.core.dsl.ParseException;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Patrick Peralta
 */
public class StreamDefinitionTests {

	private static final String INPUT_BINDING_KEY = "spring.cloud.stream.bindings.input";

	private static final String OUTPUT_BINDING_KEY = "spring.cloud.stream.bindings.output";

	@Test
	public void testStreamCreation() {
		StreamDefinition stream = new StreamDefinition("ticktock", "time | log");
		assertEquals(2, stream.getModuleDefinitions().size());
		ModuleDefinition time = stream.getModuleDefinitions().get(0);
		assertEquals("time", time.getName());
		assertEquals("time", time.getLabel());
		assertEquals("ticktock.0", time.getParameters().get(OUTPUT_BINDING_KEY));
		assertFalse(time.getParameters().containsKey(INPUT_BINDING_KEY));

		ModuleDefinition log = stream.getModuleDefinitions().get(1);
		assertEquals("log", log.getName());
		assertEquals("log", log.getLabel());
		assertEquals("ticktock.0", log.getParameters().get(INPUT_BINDING_KEY));
		assertFalse(log.getParameters().containsKey(OUTPUT_BINDING_KEY));
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
		assertEquals("test.0", source.getParameters().get(OUTPUT_BINDING_KEY));
		assertEquals("bar", sink.getName());
		assertEquals("test", sink.getGroup());
		assertEquals(1, sink.getParameters().size());
		assertEquals("test.0", sink.getParameters().get(INPUT_BINDING_KEY));
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
		assertEquals(3, filterParameters.size());
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
		assertEquals(2, sinkParameters.size());
		assertEquals("3", sinkParameters.get("z"));
	}

	@Test
	public void sourceChannelNameIsAppliedToSourceModule() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", "topic:foo > goo | blah | file");
		List<ModuleDefinition> requests = streamDefinition.getModuleDefinitions();
		assertEquals(3, requests.size());
		assertEquals("topic:foo", requests.get(0).getParameters().get(INPUT_BINDING_KEY));
	}

	@Test
	public void sinkChannelNameIsAppliedToSinkModule() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", "boo | blah | aaak > queue:foo");
		List<ModuleDefinition> requests = streamDefinition.getModuleDefinitions();
		assertEquals(3, requests.size());
		assertEquals("queue:foo", requests.get(2).getParameters().get(OUTPUT_BINDING_KEY));
	}

	@Test
	public void simpleSinkNamedChannel() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", "bart > queue:foo");
		List<ModuleDefinition> requests = streamDefinition.getModuleDefinitions();
		assertEquals(1, requests.size());
		assertEquals("queue:foo", requests.get(0).getParameters().get(OUTPUT_BINDING_KEY));
	}

	@Test
	public void simpleSinkNamedChannelBadType() throws Exception {
		// The parser will identify this as a Named channel sink and thus badLog will be
		// labeled a source.
		// But badLog is a sink and there should be an exception thrown by the parser.
		boolean isException = false;
		try {
			new StreamDefinition("test", "badLog > :foo");
		}
		catch (Exception e) {
			isException = true;
		}
		assertTrue(isException);
	}

	@Test
	public void simpleSourceNamedChannel() throws Exception {
		StreamDefinition streamDefinition = new StreamDefinition("test", "queue:foo > boot");
		List<ModuleDefinition> requests = streamDefinition.getModuleDefinitions();
		assertEquals(1, requests.size());
		assertEquals("queue:foo", requests.get(0).getParameters().get(INPUT_BINDING_KEY));
	}

	@Test
	public void namedChannelsForbiddenInComposedModules() {
		try {
			new StreamDefinition("test", "queue:foo > boot");
		}
		catch (ParseException expected) {
			assertThat(expected.getMessage(),
					containsString("A named channel is not supported in this kind of definition"));
			assertThat(expected.getPosition(), is(0));
		}
		try {
			new StreamDefinition("test", "bart | goo > queue:foo");
		}
		catch (ParseException expected) {
			assertThat(expected.getMessage(),
					containsString("A named channel is not supported in this kind of definition"));
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
		assertEquals("ticktock.0", source.getParameters().get(OUTPUT_BINDING_KEY));
		assertFalse(source.getParameters().containsKey(INPUT_BINDING_KEY));
		assertEquals("log", sink.getLabel());
		assertEquals("ticktock.0", sink.getParameters().get(INPUT_BINDING_KEY));
		assertFalse(sink.getParameters().containsKey(OUTPUT_BINDING_KEY));
	}

	@Test
	public void testBindings3Modules() {
		StreamDefinition streamDefinition = new StreamDefinition("ticktock", "time | filter |log");
		List<ModuleDefinition> modules = streamDefinition.getModuleDefinitions();

		ModuleDefinition source = modules.get(0);
		ModuleDefinition processor = modules.get(1);
		ModuleDefinition sink = modules.get(2);

		assertEquals("time", source.getLabel());
		assertEquals("ticktock.0", source.getParameters().get(OUTPUT_BINDING_KEY));
		assertFalse(source.getParameters().containsKey(INPUT_BINDING_KEY));

		assertEquals("filter", processor.getLabel());
		assertEquals("ticktock.0", processor.getParameters().get(INPUT_BINDING_KEY));
		assertEquals("ticktock.1", processor.getParameters().get(OUTPUT_BINDING_KEY));

		assertEquals("log", sink.getLabel());
		assertEquals("ticktock.1", sink.getParameters().get(INPUT_BINDING_KEY));
		assertFalse(sink.getParameters().containsKey(OUTPUT_BINDING_KEY));
	}

}