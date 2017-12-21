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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Christian Tzolov
 */
public class StreamDefinitionToDslConverterTests {

	@Test
	public void testStreamCreation() {
		reverseDslTest("time | log", 2);
	}

	@Ignore
	@Test
	public void quotesInParams() {
		reverseDslTest("foo --bar='payload.matches(''hello'')' | file", 2);
	}

	@Test
	public void quotesInParams2() {
		reverseDslTest("http --port=9700 | filter --expression=payload.matches('hello world') | file", 3);
	}

	@Test
	public void parameterizedApps() {
		reverseDslTest("foo --x=1 --y=two | bar --z=3", 2);
	}

	@Test
	public void testBindings3Apps() {
		reverseDslTest("time | filter | log", 3);
	}

	@Ignore
	@Test
	public void testXD2416_1() {
		reverseDslTest("http | transform --expression='payload.replace(\"abc\", \"\")' | log", 3);
	}

	@Ignore
	@Test
	public void testXD2416_2() {
		reverseDslTest("http | transform --expression='payload.replace(\"abc\", '''')' | log", 3);
	}

	@Test
	public void testSourceDestinationArgs() {
		reverseDslTest(":test --group=test > file", 1);
	}

	@Test
	public void testLabelsInStreams() {
		reverseDslTest("http | step1: transform --expression=payload.toUpperCase()" +
				" | step2: transform --expression=payload+'!' | log", 4);
	}

	@Test
	public void testLabelsInStreams2() {
		reverseDslTest("file | out: file", 2);
	}

	@Test
	public void testTabsInStreams() {
		reverseDslTest(":mainstream.http > counter", 1);
		reverseDslTest(":mainstream.step1 > jdbc", 1);
	}

	@Test
	public void sourceDestinationNameIsAppliedToSourceApp() {
		reverseDslTest(":foo > goo | blah | file", 3);
	}

	@Test
	public void sinkDestinationNameIsAppliedToSinkApp() {
		reverseDslTest("boo | blah | aaak > :foo", 3);
	}

	@Test
	public void testSinkNamedDestination() {
		reverseDslTest("bart > :foo", 1);
	}

	@Test
	public void testSourceNamedDestination() {
		reverseDslTest(":foo > boot", 1);
	}

	@Test
	public void testBridge() {
		reverseDslTest(":foo > :bar", 1);
	}

	private void reverseDslTest(String dslText, int expectedAppSize) {
		StreamDefinition streamDefinition = new StreamDefinition("streamName", dslText);
		assertEquals(expectedAppSize, streamDefinition.getAppDefinitions().size());

		assertEquals(streamDefinition.getDslText(),
				new StreamDefinitionToDslConverter().toDsl(streamDefinition));

		assertEquals(streamDefinition.getDslText(),
				new StreamDefinitionToDslConverter().toDsl(streamDefinition.getAppDefinitions()));
	}
}
