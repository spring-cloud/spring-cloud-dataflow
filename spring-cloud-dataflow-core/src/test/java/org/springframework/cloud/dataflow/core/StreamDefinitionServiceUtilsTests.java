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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
class StreamDefinitionServiceUtilsTests {

	StreamDefinitionService streamDefinitionService = new DefaultStreamDefinitionService();

	@Test
	void streamCreation() {
		reverseDslTest("time | log", 2);
	}

	@Disabled
	@Test
	void quotesInParams() {
		reverseDslTest("foo --bar='payload.matches(''hello'')' | file", 2);
	}

	@Test
	void quotesInParams2() {
		reverseDslTest("http --port=9700 | filter --expression=\"payload.matches('hello world')\" | file", 3);
	}

	@Test
	void parameterizedApps() {
		reverseDslTest("foo --x=1 --y=two | bar --z=3", 2);
	}

	@Test
	void bindings3Apps() {
		reverseDslTest("time | filter | log", 3);
	}

	@Test
	void xd24161() {
		reverseDslTest("http | transform --expression='payload.replace(\"abc\", \"\")' | log", 3);
	}

	@Disabled
	@Test
	void xd24162() {
		reverseDslTest("http | transform --expression='payload.replace(\"abc\", '''')' | log", 3);
	}

	@Test
	void sourceDestinationArgs() {
		reverseDslTest(":test > file --group=test", 1);
	}

	@Test
	void labelsInStreams() {
		reverseDslTest("http | step1: transform --expression=payload.toUpperCase()" +
				" | step2: transform --expression=payload+'!' | log", 4);
	}

	@Test
	void labelsInStreams2() {
		reverseDslTest("file | out: file", 2);
	}

	@Test
	void tabsInStreams() {
		reverseDslTest(":mainstream.http > counter", 1);
		reverseDslTest(":mainstream.step1 > jdbc", 1);
	}

	@Test
	void sourceDestinationNameIsAppliedToSourceApp() {
		reverseDslTest(":foo > goo | blah | file", 3);
	}

	@Test
	void sinkDestinationNameIsAppliedToSinkApp() {
		reverseDslTest("boo | blah | aaak > :foo", 3);
	}

	@Test
	void sinkNamedDestination() {
		reverseDslTest("bart > :foo", 1);
	}

	@Test
	void sourceNamedDestination() {
		reverseDslTest(":foo > boot", 1);
	}

	@Test
	void bridge() {
		reverseDslTest(":foo > :bar", 1);
	}

	private void reverseDslTest(String dslText, int expectedAppSize) {
		StreamDefinition streamDefinition = new StreamDefinition("streamName", dslText);
		assertThat(this.streamDefinitionService.getAppDefinitions(streamDefinition)).hasSize(expectedAppSize);

		assertThat(this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), this.streamDefinitionService.getAppDefinitions(streamDefinition))).isEqualTo(streamDefinition.getDslText());
	}

	@Test
	void streamDslAppPropertyWithHyphen() {
		String dslText = "foo --foo='key|value' | bar";

		System.out.println(dslText);
		StreamDefinition streamDefinition = new StreamDefinition("streamName", dslText);

		assertThat(this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), this.streamDefinitionService.getAppDefinitions(streamDefinition))).isEqualTo("foo --foo='key|value' | bar");
	}

	@Test
	void exclusionOfDataFlowAddedProperties() {

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

			assertThat(this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), this.streamDefinitionService.getAppDefinitions(streamDefinition))).isEqualTo("foo | bar");
		}
	}

	@Test
	void inputDestinationProperty() {

		String dslText = "foo --" + BindingPropertyKeys.INPUT_DESTINATION + "=boza  | bar";

		System.out.println(dslText);
		StreamDefinition streamDefinition = new StreamDefinition("streamName", dslText);

		assertThat(this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), this.streamDefinitionService.getAppDefinitions(streamDefinition))).isEqualTo(":boza > foo | bar");
	}

	@Test
	void propertyAutoQuotes() {

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

		assertThat(this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), new LinkedList(Arrays.asList(foo2, bar2)))).isEqualTo("foo --p1='a b' --p2=\"'c d'\" --p3=ef --p4=\"'i' 'j'\" --p5=\"k l\" | bar --p1='a b' --p2=\"'c d'\" --p3=ef");
	}

	@Test
	void autoQuotesOnSemicolonProperties() {

		StreamDefinition streamDefinition = new StreamDefinition("streamName",
				"http-source-kafka --server.port=9900 | couchbase-sink-kafka " +
						"--inputType=\"application/x-java-object;type=com.example.dto.InputDto\"");

		assertThat(this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), this.streamDefinitionService.getAppDefinitions(streamDefinition))).isEqualTo("http-source-kafka --server.port=9900 | couchbase-sink-kafka " +
				"--spring.cloud.stream.bindings.input.contentType='application/x-java-object;type=com.example.dto.InputDto'");


		streamDefinition = new StreamDefinition("stream2", "jdbc-mssql --cron='/10 * * * * *' " +
				"--max-messages=-1 --password='******' --query='UPDATE top (100) ASSURANCE SET assurance_flag = 1 " +
				"OUTPUT Inserted.* WHERE assurance_flag IS NULL' " +
				"--url='jdbc:sqlserver://db:1433;encrypt=false&databaseName=Spring' --username='*****' | " +
				"cust-processor | router --default-output-channel=out");

		assertThat(this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), this.streamDefinitionService.getAppDefinitions(streamDefinition))).isEqualTo("jdbc-mssql --cron='/10 * * * * *' " +
				"--max-messages=-1 --password='******' --query='UPDATE top (100) ASSURANCE SET assurance_flag = 1 " +
				"OUTPUT Inserted.* WHERE assurance_flag IS NULL' " +
				"--url='jdbc:sqlserver://db:1433;encrypt=false&databaseName=Spring' --username='*****' | " +
				"cust-processor | router --default-output-channel=out");

	}

	@Test
	void autoQuotesOnStarProperties() {

		StreamDefinition streamDefinition = new StreamDefinition("stream2", "jdbc-mssql --cron='/10 * * * * *' " +
				"--max-messages=-1 --password='******' --query='UPDATE top (100) ASSURANCE SET assurance_flag = 1 " +
				"OUTPUT Inserted.* WHERE assurance_flag IS NULL' " +
				"--url='jdbc:sqlserver://db:1433;encrypt=false&databaseName=Spring' --username='*****' | " +
				"cust-processor | router --default-output-channel=out");

		assertThat(this.streamDefinitionService.constructDsl(streamDefinition.getDslText(), this.streamDefinitionService.getAppDefinitions(streamDefinition))).isEqualTo("jdbc-mssql --cron='/10 * * * * *' " +
				"--max-messages=-1 --password='******' --query='UPDATE top (100) ASSURANCE SET assurance_flag = 1 " +
				"OUTPUT Inserted.* WHERE assurance_flag IS NULL' " +
				"--url='jdbc:sqlserver://db:1433;encrypt=false&databaseName=Spring' --username='*****' | " +
				"cust-processor | router --default-output-channel=out");
	}

}
