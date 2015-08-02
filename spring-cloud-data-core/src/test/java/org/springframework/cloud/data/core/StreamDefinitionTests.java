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