package org.springframework.cloud.data.core.dsl;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.cloud.data.core.dsl.ParsingContext.*;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.data.core.ModuleDefinition;

/**
 * @author Mark Fisher
 * @author David Turanski
 */
public class ParserTests {

	private Parser parser;

	@Before
	public void setup() {
		parser = new Parser();
	}

	@Test
	public void simpleStream() {
		List<ModuleDefinition> requests = parser.parse("test", "foo | bar", stream);
		assertEquals(2, requests.size());
		ModuleDefinition sink = requests.get(0);
		ModuleDefinition source = requests.get(1);
		assertEquals("foo", source.getName());
		assertEquals("test", source.getGroup());
		assertEquals(0, source.getIndex());

		assertEquals(0, source.getParameters().size());
		assertEquals("bar", sink.getName());
		assertEquals("test", sink.getGroup());
		assertEquals(1, sink.getIndex());
		assertEquals(0, sink.getParameters().size());
	}

	@Test
	public void quotesInParams() {
		List<ModuleDefinition> requests = parser.parse("test", "foo --bar='payload.matches(''hello'')' | file",
				stream);
		assertEquals(2, requests.size());
		ModuleDefinition source = requests.get(1);
		assertEquals("foo", source.getName());
		assertEquals("test", source.getGroup());
		assertEquals(0, source.getIndex());
		Map<String, String> sourceParameters = source.getParameters();
		assertEquals(1, sourceParameters.size());
		assertEquals("payload.matches('hello')", sourceParameters.get("bar"));
	}

	@Test
	public void quotesInParams2() {
		List<ModuleDefinition> requests = parser.parse("test",
				"http --port=9700 | filter --expression=payload.matches('hello world') | file", stream);
		assertEquals(3, requests.size());
		ModuleDefinition filter = requests.get(1);
		assertEquals("filter", filter.getName());
		assertEquals("test", filter.getGroup());
		assertEquals(1, filter.getIndex());
		Map<String, String> sourceParameters = filter.getParameters();
		assertEquals(1, sourceParameters.size());
		assertEquals("payload.matches('hello world')", sourceParameters.get("expression"));
	}

	@Test
	public void parameterizedModules() {
		List<ModuleDefinition> requests = parser.parse("test", "foo --x=1 --y=two | bar --z=3", stream);
		assertEquals(2, requests.size());
		ModuleDefinition sink = requests.get(0);
		ModuleDefinition source = requests.get(1);
		assertEquals("foo", source.getName());
		assertEquals("test", source.getGroup());
		assertEquals(0, source.getIndex());
		Map<String, String> sourceParameters = source.getParameters();
		assertEquals(2, sourceParameters.size());
		assertEquals("1", sourceParameters.get("x"));
		assertEquals("two", sourceParameters.get("y"));
		assertEquals("bar", sink.getName());
		assertEquals("test", sink.getGroup());
		assertEquals(1, sink.getIndex());
		Map<String, String> sinkParameters = sink.getParameters();
		assertEquals(1, sinkParameters.size());
		assertEquals("3", sinkParameters.get("z"));
	}

	@Test
	public void sourceChannelNameIsAppliedToSourceModule() throws Exception {
		List<ModuleDefinition> requests = parser.parse("test", "topic:foo > goo | blah | file", stream);
		assertEquals(3, requests.size());
		assertEquals("topic:foo", requests.get(2).getBindings().get(Parser.INPUT_CHANNEL));
//		assertEquals(ModuleType.processor, requests.get(2).getType());
//		assertEquals(ModuleType.processor, requests.get(1).getType());
//		assertEquals(ModuleType.sink, requests.get(0).getType());
	}

	@Test
	public void sinkChannelNameIsAppliedToSinkModule() throws Exception {
		List<ModuleDefinition> requests = parser.parse("test", "boo | blah | aaak > queue:foo", stream);
		assertEquals(3, requests.size());
		assertEquals("queue:foo", requests.get(0).getBindings().get(Parser.OUTPUT_CHANNEL));
//		assertEquals(ModuleType.processor, requests.get(0).getType());
//		assertEquals(ModuleType.processor, requests.get(1).getType());
//		assertEquals(ModuleType.source, requests.get(2).getType());
	}

//	@Test
//	public void tap() throws Exception {
//		StreamDefinitionRepository streamRepo = mock(StreamDefinitionRepository.class);
//		parser = new XDStreamParser(streamRepo, moduleRegistry(),
//				new DefaultModuleOptionsMetadataResolver());
//		when(streamRepo.findOne("xxx")).thenReturn(new StreamDefinition("xxx", "http | file"));
//		List<ModuleDefinition> requests = parser.parse("test", "tap:stream:xxx.http > file", stream);
//		assertEquals(1, requests.size());
//		assertEquals("tap:stream:xxx.http.0", requests.get(0).getSourceChannelName());
//		assertEquals(ModuleType.sink, requests.get(0).getType());
//	}

	@Test
	public void simpleSinkNamedChannel() throws Exception {
		List<ModuleDefinition> requests = parser.parse("test", "bart > queue:foo", stream);
		assertEquals(1, requests.size());
		assertEquals("queue:foo", requests.get(0).getBindings().get(Parser.OUTPUT_CHANNEL));
	}

	@Test
	public void simpleSinkNamedChannelBadType() throws Exception {
		// The parser will identify this as a Named channel sink and thus badLog will be
		// labeled a source.
		// But badLog is a sink and there should be an exception thrown by the parser.
		boolean isException = false;
		try {
			parser.parse("test", "badLog > :foo", stream);
		}
		catch (Exception e) {
			isException = true;
		}
		assertTrue(isException);
	}

	@Test
	public void simpleSourceNamedChannel() throws Exception {
		List<ModuleDefinition> requests = parser.parse("test", "queue:foo > boot", stream);
		assertEquals(1, requests.size());
		assertEquals("queue:foo", requests.get(0).getBindings().get(Parser.INPUT_CHANNEL));
	}

	@Test
	public void namedChannelsForbiddenInComposedModules() {
		try {
			parser.parse("test", "queue:foo > boot", module);
		}
		catch (StreamDefinitionException expected) {
			assertThat(expected.getMessage(),
					containsString("A named channel is not supported in this kind of definition"));
			assertThat(expected.getPosition(), is(0));
		}
		try {
			parser.parse("test", "bart | goo > queue:foo", module);
		}
		catch (StreamDefinitionException expected) {
			assertThat(expected.getMessage(),
					containsString("A named channel is not supported in this kind of definition"));
			assertThat(expected.getPosition(), is(13));
		}
	}

}