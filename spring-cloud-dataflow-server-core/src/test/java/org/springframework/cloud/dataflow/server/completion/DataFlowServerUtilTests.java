/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.dataflow.server.completion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.server.DataFlowServerUtil;
import org.springframework.cloud.dataflow.server.support.CannotDetermineApplicationTypeException;

/**
 *
 * @author Gunnar Hillert
 *
 */
public class DataFlowServerUtilTests {

	@Test
	public void testDetermineApplicationType() {
		final StreamDefinition streamDefinition = new StreamDefinition("definition-name", "foo | myProcessor | bar");

		assertEquals(Integer.valueOf(3), Integer.valueOf(streamDefinition.getAppDefinitions().size()));

		final StreamAppDefinition streamAppDefinitionSource = streamDefinition.getAppDefinitions().get(0);
		final StreamAppDefinition streamAppDefinitionProcessor = streamDefinition.getAppDefinitions().get(1);
		final StreamAppDefinition streamAppDefinitionSink = streamDefinition.getAppDefinitions().get(2);

		assertEquals(ApplicationType.source, DataFlowServerUtil.determineApplicationType(streamAppDefinitionSource));
		assertEquals(ApplicationType.processor, DataFlowServerUtil.determineApplicationType(streamAppDefinitionProcessor));
		assertEquals(ApplicationType.sink, DataFlowServerUtil.determineApplicationType(streamAppDefinitionSink));

		assertEquals("foo", streamAppDefinitionSource.getName());
		assertEquals("myProcessor", streamAppDefinitionProcessor.getName());
		assertEquals("bar", streamAppDefinitionSink.getName());
	}

	@Test
	public void testDetermineApplicationTypeFailure() {
		final StreamDefinition streamDefinition = new StreamDefinition("definition-name", "foo");

		assertEquals(Integer.valueOf(1), Integer.valueOf(streamDefinition.getAppDefinitions().size()));

		try {
			for (StreamAppDefinition streamAppDefinition: streamDefinition.getAppDefinitions()) {
				DataFlowServerUtil.determineApplicationType(streamAppDefinition);
			}
		}
		catch (CannotDetermineApplicationTypeException e) {
			assertEquals("foo had neither input nor output set", e.getMessage());
			return;
		}

		fail("Expected a CannotDetermineApplicationTypeException to be thrown.");
	}
}
