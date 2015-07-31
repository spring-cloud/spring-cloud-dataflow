package org.springframework.cloud.data.core;

import static org.junit.Assert.*;


import org.junit.Test;

import org.springframework.cloud.data.core.parser.StreamDefinitionParser;

/**
 * @author Patrick Peralta
 */
public class StreamDefinitionTests {

	@Test
	public void testStreamCreation() {
		StreamDefinition stream = new StreamDefinition("ticktock", "time | log");
		assertEquals(2, stream.getModuleDefinitions().size());
		ModuleDefinition time = stream.getModuleDefinitions().get(0);
		assertEquals("time", time.getName());
		assertEquals("time", time.getLabel());
		assertEquals("ticktock.0", time.getBindings().get(StreamDefinitionParser.OUTPUT_CHANNEL));
		assertFalse(time.getBindings().containsKey(StreamDefinitionParser.INPUT_CHANNEL));

		ModuleDefinition log = stream.getModuleDefinitions().get(1);
		assertEquals("log", log.getName());
		assertEquals("log", log.getLabel());
		assertEquals("ticktock.0", log.getBindings().get(StreamDefinitionParser.INPUT_CHANNEL));
		assertFalse(log.getBindings().containsKey(StreamDefinitionParser.OUTPUT_CHANNEL));
	}

}