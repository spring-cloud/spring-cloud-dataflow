package org.springframework.cloud.data.core.parser;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.data.core.ModuleDefinition;
import org.springframework.cloud.data.core.dsl.StreamDefinitionException;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Patrick Peralta
 */
public class StreamDefinitionParserTests {

	private StreamDefinitionParser parser;

	@Before
	public void setup() {
		parser = new StreamDefinitionParser();
	}

	@Test
	public void simpleStream() {
		List<ModuleDefinition> requests = parser.parse("test", "foo | bar");
		assertEquals(2, requests.size());
		ModuleDefinition sink = requests.get(0);
		ModuleDefinition source = requests.get(1);
		assertEquals("foo", source.getName());
		assertEquals("test", source.getGroup());

		assertEquals(0, source.getParameters().size());
		assertEquals("bar", sink.getName());
		assertEquals("test", sink.getGroup());
		assertEquals(0, sink.getParameters().size());
	}

	@Test
	public void quotesInParams() {
		List<ModuleDefinition> requests = parser.parse("test", "foo --bar='payload.matches(''hello'')' | file");
		assertEquals(2, requests.size());
		ModuleDefinition source = requests.get(1);
		assertEquals("foo", source.getName());
		assertEquals("test", source.getGroup());
		Map<String, String> sourceParameters = source.getParameters();
		assertEquals(1, sourceParameters.size());
		assertEquals("payload.matches('hello')", sourceParameters.get("bar"));
	}

	@Test
	public void quotesInParams2() {
		List<ModuleDefinition> requests = parser.parse("test",
				"http --port=9700 | filter --expression=payload.matches('hello world') | file");
		assertEquals(3, requests.size());
		ModuleDefinition filter = requests.get(1);
		assertEquals("filter", filter.getName());
		assertEquals("test", filter.getGroup());
		Map<String, String> sourceParameters = filter.getParameters();
		assertEquals(1, sourceParameters.size());
		assertEquals("payload.matches('hello world')", sourceParameters.get("expression"));
	}

	@Test
	public void parameterizedModules() {
		List<ModuleDefinition> requests = parser.parse("test", "foo --x=1 --y=two | bar --z=3");
		assertEquals(2, requests.size());
		ModuleDefinition sink = requests.get(0);
		ModuleDefinition source = requests.get(1);
		assertEquals("foo", source.getName());
		assertEquals("test", source.getGroup());
		Map<String, String> sourceParameters = source.getParameters();
		assertEquals(2, sourceParameters.size());
		assertEquals("1", sourceParameters.get("x"));
		assertEquals("two", sourceParameters.get("y"));
		assertEquals("bar", sink.getName());
		assertEquals("test", sink.getGroup());
		Map<String, String> sinkParameters = sink.getParameters();
		assertEquals(1, sinkParameters.size());
		assertEquals("3", sinkParameters.get("z"));
	}

	@Test
	public void sourceChannelNameIsAppliedToSourceModule() throws Exception {
		List<ModuleDefinition> requests = parser.parse("test", "topic:foo > goo | blah | file");
		assertEquals(3, requests.size());
		assertEquals("topic:foo", requests.get(2).getBindings().get(StreamDefinitionParser.INPUT_CHANNEL));
	}

	@Test
	public void sinkChannelNameIsAppliedToSinkModule() throws Exception {
		List<ModuleDefinition> requests = parser.parse("test", "boo | blah | aaak > queue:foo");
		assertEquals(3, requests.size());
		assertEquals("queue:foo", requests.get(0).getBindings().get(StreamDefinitionParser.OUTPUT_CHANNEL));
	}

	@Test
	public void simpleSinkNamedChannel() throws Exception {
		List<ModuleDefinition> requests = parser.parse("test", "bart > queue:foo");
		assertEquals(1, requests.size());
		assertEquals("queue:foo", requests.get(0).getBindings().get(StreamDefinitionParser.OUTPUT_CHANNEL));
	}

	@Test
	public void simpleSinkNamedChannelBadType() throws Exception {
		// The parser will identify this as a Named channel sink and thus badLog will be
		// labeled a source.
		// But badLog is a sink and there should be an exception thrown by the parser.
		boolean isException = false;
		try {
			parser.parse("test", "badLog > :foo");
		}
		catch (Exception e) {
			isException = true;
		}
		assertTrue(isException);
	}

	@Test
	public void simpleSourceNamedChannel() throws Exception {
		List<ModuleDefinition> requests = parser.parse("test", "queue:foo > boot");
		assertEquals(1, requests.size());
		assertEquals("queue:foo", requests.get(0).getBindings().get(StreamDefinitionParser.INPUT_CHANNEL));
	}

	@Test
	public void namedChannelsForbiddenInComposedModules() {
		try {
			parser.parse("test", "queue:foo > boot");
		}
		catch (StreamDefinitionException expected) {
			assertThat(expected.getMessage(),
					containsString("A named channel is not supported in this kind of definition"));
			assertThat(expected.getPosition(), is(0));
		}
		try {
			parser.parse("test", "bart | goo > queue:foo");
		}
		catch (StreamDefinitionException expected) {
			assertThat(expected.getMessage(),
					containsString("A named channel is not supported in this kind of definition"));
			assertThat(expected.getPosition(), is(13));
		}
	}

	@Test
	public void testBindings2Modules() {
		List<ModuleDefinition> modules = parser.parse("ticktock", "time | log");

		assertEquals("log", modules.get(0).getLabel());
		assertEquals("ticktock.0", modules.get(0).getBindings().get(StreamDefinitionParser.INPUT_CHANNEL));
		assertFalse(modules.get(0).getBindings().containsKey(StreamDefinitionParser.OUTPUT_CHANNEL));

		assertEquals("time", modules.get(1).getLabel());
		assertEquals("ticktock.0", modules.get(1).getBindings().get(StreamDefinitionParser.OUTPUT_CHANNEL));
		assertFalse(modules.get(1).getBindings().containsKey(StreamDefinitionParser.INPUT_CHANNEL));
	}

	@Test
	public void testBindings3Modules() {
		List<ModuleDefinition> modules = parser.parse("ticktock", "time | filter |log");

		assertEquals("log", modules.get(0).getLabel());
		assertEquals("ticktock.1", modules.get(0).getBindings().get(StreamDefinitionParser.INPUT_CHANNEL));
		assertFalse(modules.get(0).getBindings().containsKey(StreamDefinitionParser.OUTPUT_CHANNEL));

		assertEquals("filter", modules.get(1).getLabel());
		assertEquals("ticktock.0", modules.get(1).getBindings().get(StreamDefinitionParser.INPUT_CHANNEL));
		assertEquals("ticktock.1", modules.get(1).getBindings().get(StreamDefinitionParser.OUTPUT_CHANNEL));

		assertEquals("time", modules.get(2).getLabel());
		assertEquals("ticktock.0", modules.get(2).getBindings().get(StreamDefinitionParser.OUTPUT_CHANNEL));
		assertFalse(modules.get(2).getBindings().containsKey(StreamDefinitionParser.INPUT_CHANNEL));
	}

}