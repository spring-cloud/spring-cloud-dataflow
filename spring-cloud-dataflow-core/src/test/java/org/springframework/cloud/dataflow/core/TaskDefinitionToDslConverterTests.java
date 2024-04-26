/*
 * Copyright 2018 the original author or authors.
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
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 */
public class TaskDefinitionToDslConverterTests {

	@Test
	public void testTaskDsl() {
		assertEquals("foo --prop2=value2 --prop1=value1",
				new TaskDefinitionToDslConverter().toDsl(new TaskDefinition("myTask", "foo --prop1=value1 --prop2=value2")));
	}

	@Test
	public void testExclusionOfDataFlowAddedProperties() {

		List<String> dataFlowAddedProperties = Collections.singletonList(
			TaskDefinition.SPRING_CLOUD_TASK_NAME);

		for (String key : dataFlowAddedProperties) {
			String dslText = "foo --" + key + "=boza";

			System.out.println(dslText);
			TaskDefinition taskDefinition = new TaskDefinition("streamName", dslText);

			assertEquals("foo",
					new TaskDefinitionToDslConverter().toDsl(taskDefinition));
		}
	}

	@Test
	public void testPropertyAutoQuotes() {

		TaskDefinition taskDefinition = new TaskDefinition("fooTask", "foo");

		TaskDefinition fooTask = TaskDefinition.TaskDefinitionBuilder.from(taskDefinition)

				.setProperty("p1", "a b")
				.setProperty("p2", "'c d'")
				.setProperty("p3", "ef")
				.setProperty("p4", "'i' 'j'")
				.setProperty("p5", "\"k l\"")
				.build();

		assertEquals("foo --p1='a b' --p2=\"'c d'\" --p3=ef --p4=\"'i' 'j'\" --p5=\"k l\"",
				new TaskDefinitionToDslConverter().toDsl(fooTask));
	}

	@Test
	public void autoQuotesOnStarProperties() {

		TaskDefinition taskDefinition = new TaskDefinition("fooTask", "jdbc-mssql --cron='/10 * * * * *' " +
				"--max-messages=-1 --password='******' --query='UPDATE top (100) ASSURANCE SET assurance_flag = 1 " +
				"OUTPUT Inserted.* WHERE assurance_flag IS NULL' " +
				"--url='jdbc:sqlserver://db:1433;encrypt=false&databaseName=Spring' --username='*****'");

		assertEquals("jdbc-mssql --cron='/10 * * * * *' " +
						"--max-messages=-1 --password='******' --query='UPDATE top (100) ASSURANCE SET assurance_flag = 1 " +
						"OUTPUT Inserted.* WHERE assurance_flag IS NULL' " +
						"--url='jdbc:sqlserver://db:1433;encrypt=false&databaseName=Spring' --username='*****'",
				new TaskDefinitionToDslConverter().toDsl(taskDefinition));

	}

	@Test
	public void compositeTaskDsl() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			TaskDefinition taskDefinition = new TaskDefinition("composedTaskName", "foo && bar");
			new TaskDefinitionToDslConverter().toDsl(taskDefinition);
		});
	}

}
