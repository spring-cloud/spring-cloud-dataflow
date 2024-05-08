/*
 * Copyright 2015-2021 the original author or authors.
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
import java.util.LinkedList;
import java.util.List;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 */
public class StreamDefinitionServiceUtilsTests {

	StreamDefinitionService streamDefinitionService = new DefaultStreamDefinitionService();

	@Test
	public void testStreamCreation() {
		reverseDslTest("time | log", 2);
	}

	@Disabled
	@Test
	public void quotesInParams() {
		reverseDslTest("foo --bar='payload.matches(''hello'')' | file", 2);
	}

	@Test
	public void quotesInParams2() {
		reverseDslTest("http --port=9700 | filter --expression=\"payload.matches('hello world')\" | file", 3);
	}

	@Test
	public void parameterizedApps() {
		reverseDslTest("foo --x=1 --y=two | bar --z=3", 2);
	}

	@Test
	public void testBindings3Apps() {
		reverseDslTest("time | filter | log", 3);
	}

	@Disabled
	@Test
	public void testXD2416_1() {
		reverseDslTest("http | transform --expression='payload.replace(\"abc\", \"\")' | log", 3);
	}

	@Disabled
	@Test
	public void testXD2416_2() {
		reverseDslTest("http | transform --expression='payload.replace(\"abc\", '''')' | log", 3);
	}

	@Test
	public void testSourceDestinationArgs() {
		reverseDslTest(":test > file --group=test", 1);
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
		assertEquals(expectedAppSize, this.streamDefinitionService.getAppDefinitions(streamDefinition).size());

		assertEquals(streamDefinition.getDslText(),
				this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), this.streamDefinitionService.getAppDefinitions(streamDefinition)));
	}

	@Test
	public void testStreamDslAppPropertyWithHyphen() {
		String dslText = "foo --foo='key|value' | bar";

		System.out.println(dslText);
		StreamDefinition streamDefinition = new StreamDefinition("streamName", dslText);

		assertEquals("foo --foo='key|value' | bar",
				this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), this.streamDefinitionService.getAppDefinitions(streamDefinition)));
	}

	@Test
	public void testExclusionOfDataFlowAddedProperties() {

		List<String> dataFlowAddedProperties = Arrays.asList(
				DataFlowPropertyKeys.STREAM_APP_TYPE,
				DataFlowPropertyKeys.STREAM_APP_LABEL,
				DataFlowPropertyKeys.STREAM_NAME,
				BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS,
				BindingPropertyKeys.OUTPUT_DESTINATION);

		for (String key : dataFlowAddedProperties) {
			String dslText = "foo --" + key + "=boza  | bar";

			System.out.println(dslText);
			StreamDefinition streamDefinition = new StreamDefinition("streamName", dslText);

			assertEquals("foo | bar",
					this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), this.streamDefinitionService.getAppDefinitions(streamDefinition)));
		}
	}

	@Test
	public void testInputDestinationProperty() {

		String dslText = "foo --" + BindingPropertyKeys.INPUT_DESTINATION + "=boza  | bar";

		System.out.println(dslText);
		StreamDefinition streamDefinition = new StreamDefinition("streamName", dslText);

		assertEquals(":boza > foo | bar",
				this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), this.streamDefinitionService.getAppDefinitions(streamDefinition)));
	}

	@Test
	public void testPropertyAutoQuotes() {

		String streamName = "stream2";

		StreamDefinition streamDefinition = new StreamDefinition(streamName, "foo | bar");

		StreamAppDefinition foo = this.streamDefinitionService.getAppDefinitions(streamDefinition).get(0);
		StreamAppDefinition bar = this.streamDefinitionService.getAppDefinitions(streamDefinition).get(1);

		StreamAppDefinition foo2 = StreamAppDefinition.Builder.from(foo)
				.setProperty("p1", "a b")
				.setProperty("p2", "'c d'")
				.setProperty("p3", "ef")
				.setProperty("p4", "'i' 'j'")
				.setProperty("p5", "\"k l\"")
				.build(streamName);

		StreamAppDefinition bar2 = StreamAppDefinition.Builder.from(bar)
				.setProperty("p1", "a b")
				.setProperty("p2", "'c d'")
				.setProperty("p3", "ef")
				.build(streamName);

		assertEquals("foo --p1='a b' --p2=\"'c d'\" --p3=ef --p4=\"'i' 'j'\" --p5=\"k l\" | bar --p1='a b' --p2=\"'c d'\" --p3=ef",
				this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), new LinkedList(Arrays.asList(foo2, bar2))));
	}

	@Test
	public void autoQuotesOnSemicolonProperties() {

		StreamDefinition streamDefinition = new StreamDefinition("streamName",
				"http-source-kafka --server.port=9900 | couchbase-sink-kafka " +
						"--inputType=\"application/x-java-object;type=com.example.dto.InputDto\"");

		assertEquals("http-source-kafka --server.port=9900 | couchbase-sink-kafka " +
						"--spring.cloud.stream.bindings.input.contentType='application/x-java-object;type=com.example.dto.InputDto'",
				this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), this.streamDefinitionService.getAppDefinitions(streamDefinition)));


		streamDefinition = new StreamDefinition("stream2", "jdbc-mssql --cron='/10 * * * * *' " +
				"--max-messages=-1 --password='******' --query='UPDATE top (100) ASSURANCE SET assurance_flag = 1 " +
				"OUTPUT Inserted.* WHERE assurance_flag IS NULL' " +
				"--url='jdbc:sqlserver://db:1433;encrypt=false&databaseName=Spring' --username='*****' | " +
				"cust-processor | router --default-output-channel=out");

		assertEquals("jdbc-mssql --cron='/10 * * * * *' " +
						"--max-messages=-1 --password='******' --query='UPDATE top (100) ASSURANCE SET assurance_flag = 1 " +
						"OUTPUT Inserted.* WHERE assurance_flag IS NULL' " +
						"--url='jdbc:sqlserver://db:1433;encrypt=false&databaseName=Spring' --username='*****' | " +
						"cust-processor | router --default-output-channel=out",
				this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), this.streamDefinitionService.getAppDefinitions(streamDefinition)));

	}

	@Test
	public void autoQuotesOnStarProperties() {

		StreamDefinition streamDefinition = new StreamDefinition("stream2", "jdbc-mssql --cron='/10 * * * * *' " +
				"--max-messages=-1 --password='******' --query='UPDATE top (100) ASSURANCE SET assurance_flag = 1 " +
				"OUTPUT Inserted.* WHERE assurance_flag IS NULL' " +
				"--url='jdbc:sqlserver://db:1433;encrypt=false&databaseName=Spring' --username='*****' | " +
				"cust-processor | router --default-output-channel=out");

		assertEquals("jdbc-mssql --cron='/10 * * * * *' " +
						"--max-messages=-1 --password='******' --query='UPDATE top (100) ASSURANCE SET assurance_flag = 1 " +
						"OUTPUT Inserted.* WHERE assurance_flag IS NULL' " +
						"--url='jdbc:sqlserver://db:1433;encrypt=false&databaseName=Spring' --username='*****' | " +
						"cust-processor | router --default-output-channel=out",
				this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), this.streamDefinitionService.getAppDefinitions(streamDefinition)));
	}

}
