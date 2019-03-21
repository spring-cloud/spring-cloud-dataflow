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

import java.util.Arrays;
import java.util.List;

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

	@Test
	public void testExclusionOfDataFlowAddedProperties() {

		List<String> dataFlowAddedProperties = Arrays.asList(
				DataFlowPropertyKeys.STREAM_APP_TYPE,
				DataFlowPropertyKeys.STREAM_APP_LABEL,
				DataFlowPropertyKeys.STREAM_NAME,
				StreamPropertyKeys.METRICS_TRIGGER_INCLUDES,
				StreamPropertyKeys.METRICS_KEY,
				StreamPropertyKeys.METRICS_PROPERTIES,
				BindingPropertyKeys.INPUT_GROUP,
				BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS,
				BindingPropertyKeys.OUTPUT_DESTINATION);

		for (String key : dataFlowAddedProperties) {
			String dslText = "foo --" + key + "=boza  | bar";

			System.out.println(dslText);
			StreamDefinition streamDefinition = new StreamDefinition("streamName", dslText);

			assertEquals("foo | bar",
					new StreamDefinitionToDslConverter().toDsl(streamDefinition.getAppDefinitions()));
		}
	}

	@Test
	public void testInputDestinationProperty() {

		String dslText = "foo --" + BindingPropertyKeys.INPUT_DESTINATION + "=boza  | bar";

		System.out.println(dslText);
		StreamDefinition streamDefinition = new StreamDefinition("streamName", dslText);

		assertEquals(":boza > foo | bar",
				new StreamDefinitionToDslConverter().toDsl(streamDefinition.getAppDefinitions()));
	}

	@Test
	public void testPropertyAutoQuotes() {

		StreamDefinition streamDefinition = new StreamDefinition("streamName", "foo | bar");


		StreamAppDefinition foo = streamDefinition.getAppDefinitions().get(0);
		StreamAppDefinition bar = streamDefinition.getAppDefinitions().get(1);

		StreamAppDefinition foo2 = StreamAppDefinition.Builder.from(foo)
				.setProperty("p1", "a b")
				.setProperty("p2", "'c d'")
				.setProperty("p3", "ef")
				.build("stream2");

		StreamAppDefinition bar2 = StreamAppDefinition.Builder.from(bar)
				.setProperty("p1", "a b")
				.setProperty("p2", "'c d'")
				.setProperty("p3", "ef")
				.build("stream2");

		assertEquals("foo --p1='a b' --p2='c d' --p3=ef | bar --p1='a b' --p2='c d' --p3=ef",
				new StreamDefinitionToDslConverter().toDsl(Arrays.asList(foo2, bar2)));
	}

}
