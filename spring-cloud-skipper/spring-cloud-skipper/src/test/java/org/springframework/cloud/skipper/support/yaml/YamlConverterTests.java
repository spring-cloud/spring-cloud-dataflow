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

package org.springframework.cloud.skipper.support.yaml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.skipper.support.yaml.YamlConversionStatus.ConversionMessage;
import org.springframework.cloud.skipper.support.yaml.YamlConverter.Builder;
import org.springframework.cloud.skipper.support.yaml.YamlConverter.Mode;

import static org.assertj.core.api.Assertions.assertThat;

class YamlConverterTests {

	@Test
	void conversionWithListItems() throws Exception {
		doConversionTest(
				"""
				some.thing[0].a=first-a
				some.thing[0].b=first-b
				some.thing[1].a=second-a
				some.thing[1].b=second-b
				""",
				// ==>
				"""
				some:
				  thing:
				  - a: first-a
				    b: first-b
				  - a: second-a
				    b: second-b
				"""
		);
	}

	@Test
	void deepKeys1() throws Exception {
		doConversionTest(
				"hi.this.is.same=xxx.yyy\n",
				// ==>
				"""
				hi:
				  this:
				    is:
				      same: xxx.yyy
				"""
		);

		Map<String, String> input = new HashMap<String, String>();
		input.put("hi.this.is.same", "xxx.yyy");
		doMapConversionTest(
				input,
				// ==>
				"""
				hi:
				  this:
				    is:
				      same: xxx.yyy
				"""
		);

	}

	@Test
	void deepKeys2() throws Exception {
		doConversionTest(
				"""
				bye.this.is.same=xxx.yyy
				hi.this.is.same=xxx.yyy
				""",
				// ==>
				"""
				bye:
				  this:
				    is:
				      same: xxx.yyy
				hi:
				  this:
				    is:
				      same: xxx.yyy
				"""
		);
		Map<String, String> input = new HashMap<String, String>();
		input.put("bye.this.is.same", "xxx.yyy");
		input.put("hi.this.is.same", "xxx.yyy");
		doMapConversionTest(
				input,
				// ==>
				"""
				bye:
				  this:
				    is:
				      same: xxx.yyy
				hi:
				  this:
				    is:
				      same: xxx.yyy
				"""
		);
	}

	@Test
	void hasComments() throws Exception {
		do_hasComments_test("#comment");
		do_hasComments_test("!comment");
		do_hasComments_test("    \t!comment");
		String yaml = do_hasComments_test("    #!comment");
		assertYaml(yaml,
				"""
				other:
				  property: othervalue
				some:
				  property: somevalue
				"""
		);
	}

	private void assertYaml(String yaml, String expected) {
		assertThat(yaml).isEqualTo(expected);
	}

	@Test
	void almostHasComments() throws Exception {
		doConversionTest(
			"""
			my.hello=Good morning!
			my.goodbye=See ya # later
			""",
			// ==>
			"""
			my:
			  goodbye: 'See ya # later'
			  hello: Good morning!
			"""
		);
	}


	@Test
	void simpleConversion() throws Exception {
		doConversionTest(
				"""
				some.thing=vvvv
				some.other.thing=blah
				""",
				// ==>
				"""
				some:
				  other:
				    thing: blah
				  thing: vvvv
				"""
		);
	}

	@Test
	void emptyFileConversion() throws Exception {
		doConversionTest(
				"",
				// ==>
				""
		);
	}

	@Test
	void unusualName() throws Exception {
		File input = createFile("no-extension",
				"server.port: 6789"
		);
		YamlConversionResult result = YamlConverter.builder().file(input).build().convert();
		assertOkStatus(result.getStatus());

		assertThat(result.getYaml()).isEqualTo("server:\n" + "  port: '6789'\n");
	}

	@Test
	void multipleAssignmentProblem() throws Exception {
		do_conversionTest(
				"""
				some.property=something
				some.property=something-else""",
				// ==>
				"""
				some:
				  property: something-else
				""",
				(status) -> {
					assertThat(status.getSeverity()).isEqualTo(0);
				}
		);
	}

	@Test
	void scalarAndMapConflict() throws Exception {
		do_conversionTest(
				"""
				some.property=a-scalar
				some.property.sub=sub-value""",
				// ==>
				"""
				some:
				  property:
				    sub: sub-value
				""",
				(status) -> {
					assertStatus(status, YamlConversionStatus.ERROR,
							"Direct assignment 'some.property=a-scalar' can not be combined with sub-property assignment 'some.property.sub...'");
				}
		);
	}

	@Test
	void scalarAndMapConflictFlatten() throws Exception {
		do_conversionTest(
				Mode.FLATTEN,
				"""
				some.property=a-scalar
				some.property.sub=sub-value""",
				// ==>
				"""
				some:
				  property: a-scalar
				  property.sub: sub-value
				""",
				(status) -> {
					assertThat(status.getSeverity()).isEqualTo(0);
				}
		);
	}

	@Test
	void forceFlatten() throws Exception {
		do_conversionTest(
				Mode.FLATTEN,
			Collections.singletonList("some.property"),
				"some.property.sub1.sub2=sub-value",
				// ==>
				"""
				some:
				  property:
				    sub1.sub2: sub-value
				""",
				(status) -> {
					assertThat(status.getSeverity()).isEqualTo(0);
				}
		);
	}

	@Test
	void forceFlattenMulti() throws Exception {
		do_conversionTest(
				Mode.FLATTEN,
			Collections.singletonList("some.property"),
				"""
				some.property.sub1.sub2=sub-value1
				some.property.sub3.sub4=sub-value2""",
				// ==>
				"""
				some:
				  property:
				    sub1.sub2: sub-value1
				    sub3.sub4: sub-value2
				""",
				(status) -> {
					assertThat(status.getSeverity()).isEqualTo(0);
				}
		);
	}

	@Test
	void forceFlattenMultiUseRegex() throws Exception {
		do_conversionTest(
				Mode.FLATTEN,
			Collections.singletonList("[a-z]*2\\.property"),
				"""
				some1.property.sub1.sub2=sub-value1
				some1.property.sub3.sub4=sub-value2
				some2.property.sub5.sub6=sub-value1
				some2.property.sub7.sub8=sub-value2""",
				// ==>
				"""
				some1:
				  property:
				    sub1:
				      sub2: sub-value1
				    sub3:
				      sub4: sub-value2
				some2:
				  property:
				    sub5.sub6: sub-value1
				    sub7.sub8: sub-value2
				""",
				(status) -> {
					assertThat(status.getSeverity()).isEqualTo(0);
				}
		);
	}

	@Test
	void scalarAndMapConflictDeepFlatten() throws Exception {
		do_conversionTest(
				Mode.FLATTEN,
				"""
				log4j.appender.stdout=org.apache.log4j.ConsoleAppender
				log4j.appender.stdout.Target:System.out
				log4j.appender.stdout.layout:org.apache.log4j.PatternLayout
				log4j.appender.stdout.layout.ConversionPattern:%d{ABSOLUTE} %5p %c{1}:%L - %m%n
				log4j.rootLogger:INFO, stdout
				log4j.logger.org.hibernate:DEBUG
				log4j.logger.org.hibernate.type:ALL
				""",
				// ==>
				"""
				log4j:
				  appender:
				    stdout: org.apache.log4j.ConsoleAppender
				    stdout.Target: System.out
				    stdout.layout: org.apache.log4j.PatternLayout
				    stdout.layout.ConversionPattern: '%d{ABSOLUTE} %5p %c{1}:%L - %m%n'
				  logger:
				    org:
				      hibernate: DEBUG
				      hibernate.type: ALL
				  rootLogger: INFO, stdout
				""",
				(status) -> {
					assertThat(status.getSeverity()).isEqualTo(0);
				}
		);
	}

	@Test
	void scalarAndMapConflictDeepFlatten2() throws Exception {
		do_conversionTest(
				Mode.FLATTEN,
				"""
				log4j.appender.stdout=org.apache.log4j.ConsoleAppender
				log4j.appender.stdout.Target:System.out
				log4j.appender.stdout.layout:org.apache.log4j.PatternLayout
				log4j.appender.stdout.layout.ConversionPattern:%d{ABSOLUTE} %5p %c{1}:%L - %m%n
				
				log4j.rootLogger:INFO, stdout
				
				log4j.logger.org.hibernate:DEBUG
				
				log4j.logger.org.hibernate.type:ALL
				
				""",
				// ==>
				"""
				log4j:
				  appender:
				    stdout: org.apache.log4j.ConsoleAppender
				    stdout.Target: System.out
				    stdout.layout: org.apache.log4j.PatternLayout
				    stdout.layout.ConversionPattern: '%d{ABSOLUTE} %5p %c{1}:%L - %m%n'
				  logger:
				    org:
				      hibernate: DEBUG
				      hibernate.type: ALL
				  rootLogger: INFO, stdout
				""",
				(status) -> {
					assertThat(status.getSeverity()).isEqualTo(0);
				}
		);
	}

	@Test
	void scalarAndSequenceConflict() throws Exception {
		do_conversionTest(
				"""
				some.property=a-scalar
				some.property[0]=zero
				some.property[1]=one
				""",
				// ==>
				"""
				some:
				  property:
				  - zero
				  - one
				""",
				(status) -> {
					assertStatus(status, YamlConversionStatus.ERROR,
							"Direct assignment 'some.property=a-scalar' can not be combined with sequence assignment 'some.property[0]...'");
				}
		);
	}

	@Test
	void mapAndSequenceConflict() throws Exception {
		do_conversionTest(
				"""
				some.property.abc=val1
				some.property.def=val2
				some.property[0]=zero
				some.property[1]=one
				""",
				// ==>
				"""
				some:
				  property:
				    '0': zero
				    '1': one
				    abc: val1
				    def: val2
				""",
				(status) -> {
					assertStatus(status, YamlConversionStatus.WARNING,
							"'some.property' has some entries that look like list items and others that look like map entries");
				}
		);
	}

	@Test
	void scalarAndMapAndSequenceConflict() throws Exception {
		do_conversionTest(
				"""
				some.property=a-scalar
				some.property.abc=val1
				some.property.def=val2
				some.property[0]=zero
				some.property[1]=one
				""",
				// ==>
				"""
				some:
				  property:
				    '0': zero
				    '1': one
				    abc: val1
				    def: val2
				""",
				(status) -> {
					assertStatus(status, YamlConversionStatus.ERROR,
							"Direct assignment 'some.property=a-scalar' can not be combined with sub-property assignment 'some.property.abc...'. ");
					assertStatus(status, YamlConversionStatus.ERROR,
							"'some.property' has some entries that look like list items and others that look like map entries");
				}
		);
	}

	private void doConversionTest(String input, String expectedOutput) throws Exception {
		do_conversionTest(input, expectedOutput, (status) -> {
			assertThat(status.getSeverity()).isEqualTo(YamlConversionStatus.OK);
		});
	}

	private void do_conversionTest(String input, String expectedOutput, Checker<YamlConversionStatus> statusChecker) throws Exception {
		do_conversionTest(Mode.DEFAULT, input, expectedOutput, statusChecker);
	}

	private void do_conversionTest(Mode mode, String input, String expectedOutput, Checker<YamlConversionStatus> statusChecker) throws Exception {
		do_conversionTest(mode, null, input, expectedOutput, statusChecker);
	}

	private void do_conversionTest(Mode mode, List<String> keyspaces, String input, String expectedOutput, Checker<YamlConversionStatus> statusChecker) throws Exception {
		File propertiesFile = createFile("application.properties", input);
		assertThat(propertiesFile).exists();
		Builder builder = YamlConverter.builder().mode(mode).file(propertiesFile);
		if (keyspaces != null) {
			for (String keyspace : keyspaces) {
				builder.flat(keyspace);
			}
		}
		YamlConversionResult result = builder.build().convert();
		statusChecker.check(result.getStatus());
		assertThat(result.getYaml()).isEqualTo(expectedOutput);
	}

	private void doMapConversionTest(Map<String, String> input, String expectedOutput) throws Exception {
		do_mapConversionTest(input, expectedOutput, (status) -> {
			assertThat(status.getSeverity()).isEqualTo(YamlConversionStatus.OK);
		});
	}

	private void do_mapConversionTest(Map<String, String> input, String expectedOutput, Checker<YamlConversionStatus> statusChecker) throws Exception {
		YamlConversionResult result = YamlConverter.builder().map(input).build().convert();
		statusChecker.check(result.getStatus());
		assertThat(result.getYaml()).isEqualTo(expectedOutput);
	}

	private File createFile(String prefix, String content) {
		try {
			File file = File.createTempFile(prefix, null);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.append(content);
			bw.close();
			return file;
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private String do_hasComments_test(String comment) throws Exception {
		File propsFile = createFile("application.properties",
				"some.property=somevalue\n" +
				comment + "\n" +
				"other.property=othervalue"
		);
		YamlConversionResult result = YamlConverter.builder().file(propsFile).build().convert();
		assertStatus(result.getStatus(), YamlConversionStatus.WARNING, "has comments, which will be lost");
		return result.getYaml();
	}

	private void assertOkStatus(YamlConversionStatus s) {
		assertThat(s.getSeverity()).isEqualTo(YamlConversionStatus.OK);
	}

	private void assertStatus(YamlConversionStatus status, int expectedSeverity, String expectedMessageFragment) {
		assertThat(status.getSeverity()).isEqualTo(expectedSeverity);
		StringBuilder allMessages = new StringBuilder();
		for (ConversionMessage entry : status.getEntries()) {
			allMessages.append(entry.getMessage());
			allMessages.append("\n-------------\n");
		}
		assertContains(expectedMessageFragment, allMessages.toString());
	}

	private void assertContains(String required, String inData) {
		assert inData.indexOf(required) != -1;
	}

	public interface Checker<T> {
		void check(T it) throws Exception;
	}
}
