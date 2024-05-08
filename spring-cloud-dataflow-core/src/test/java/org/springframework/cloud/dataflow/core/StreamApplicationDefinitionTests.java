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



import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class StreamApplicationDefinitionTests {

	private static final String OUTPUT_BINDING_KEY = "spring.cloud.stream.bindings.output";

	@Test
	public void testBuilder() {
		StreamAppDefinition definition = new StreamAppDefinition.Builder().setRegisteredAppName("time")
				.setLabel("label").setApplicationType(ApplicationType.source).setProperty(OUTPUT_BINDING_KEY, "channel").build("ticktock");

		assertEquals("ticktock", definition.getStreamName());
		assertEquals("time", definition.getRegisteredAppName());
		assertEquals("label", definition.getName());
		assertEquals(ApplicationType.source, definition.getApplicationType());
		assertEquals(1, definition.getProperties().size());
		assertEquals("channel", definition.getProperties().get(OUTPUT_BINDING_KEY));
	}

}
