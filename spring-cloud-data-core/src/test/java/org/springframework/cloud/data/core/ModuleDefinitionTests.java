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

/**
 * @author Patrick Peralta
 */
public class ModuleDefinitionTests {

	@Test
	public void testBuilder() {
		ModuleDefinition definition = new ModuleDefinition.Builder()
				.setGroup("ticktock")
				.setIndex(0)
				.setName("time")
				.setLabel("label")
				.setBinding("output", "channel").build();

		assertEquals("ticktock", definition.getGroup());
		assertEquals("time", definition.getName());
		assertEquals("label", definition.getLabel());
		assertEquals(0, definition.getIndex());
		assertEquals(1, definition.getBindings().size());
		assertEquals("channel", definition.getBindings().get("output"));
	}

}