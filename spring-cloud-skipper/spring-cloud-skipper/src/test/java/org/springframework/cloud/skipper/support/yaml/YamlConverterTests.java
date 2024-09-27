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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.skipper.support.yaml.YamlConversionStatus.ConversionMessage;
import org.springframework.cloud.skipper.support.yaml.YamlConverter.Builder;
import org.springframework.cloud.skipper.support.yaml.YamlConverter.Mode;

import static org.assertj.core.api.Assertions.assertThat;

public class YamlConverterTests {

	@Test
	public void conversionWithListItems() throws Exception {
		doConversionTest(
				"some.thing[0].a=first-a\n" +
				"some.thing[0].b=first-b\n" +
				"some.thing[1].a=second-a\n" +
				"some.thing[1].b=second-b\n",
				// ==>
				"some:\n" +
				"  thing:\n" +
				"  - a: first-a\n" +
				"    b: first-b\n" +
				"  - a: second-a\n" +
				"    b: second-b\n"
		);
	}

	@Test
	public void deepKeys1() throws Exception {
		doConversionTest(
				"hi.this.is.same=xxx.yyy\n",
				// ==>
				"hi:\n" +
				"  this:\n" +
				"    is:\n" +
				"      same: xxx.yyy\n"
		);

		Map<String, String> input = new HashMap<String, String>();
		input.put("hi.this.is.same", "xxx.yyy");
		doMapConversionTest(
				input,
				// ==>
				"hi:\n" +
				"  this:\n" +
				"    is:\n" +
				"      same: xxx.yyy\n"
		);

	}

	@Test
	public void deepKeys2() throws Exception {
		doConversionTest(
				"bye.this.is.same=xxx.yyy\n" +
				"hi.this.is.same=xxx.yyy\n",
				// ==>
				"bye:\n" +
				"  this:\n" +
				"    is:\n" +
				"      same: xxx.yyy\n" +
				"hi:\n" +
				"  this:\n" +
				"    is:\n" +
				"      same: xxx.yyy\n"
		);
		Map<String, String> input = new HashMap<String, String>();
		input.put("bye.this.is.same", "xxx.yyy");
		input.put("hi.this.is.same", "xxx.yyy");
		doMapConversionTest(
				input,
				// ==>
				"bye:\n" +
				"  this:\n" +
				"    is:\n" +
				"      same: xxx.yyy\n" +
				"hi:\n" +
				"  this:\n" +
				"    is:\n" +
				"      same: xxx.yyy\n"
		);
	}

	@Test
	public void hasComments() throws Exception {
		do_hasComments_test("#comment");
		do_hasComments_test("!comment");
		do_hasComments_test("    \t!comment");
		String yaml = do_hasComments_test("    #!comment");
		assertYaml(yaml,
				"other:\n" +
				"  property: othervalue\n" +
				"some:\n" +
				"  property: somevalue\n"
		);
	}

	private void assertYaml(String yaml, String expected) {
		assertThat(yaml).isEqualTo(expected);
	}

	@Test
	public void almostHasComments() throws Exception {
		doConversionTest(
			"my.hello=Good morning!\n" +
			"my.goodbye=See ya # later\n",
			// ==>
			"my:\n" +
			"  goodbye: 'See ya # later'\n" +
			"  hello: Good morning!\n"
		);
	}


	@Test
	public void simpleConversion() throws Exception {
		doConversionTest(
				"some.thing=vvvv\n" +
				"some.other.thing=blah\n",
				// ==>
				"some:\n" +
				"  other:\n" +
				"    thing: blah\n" +
				"  thing: vvvv\n"
		);
	}

	@Test
	public void emptyFileConversion() throws Exception {
		doConversionTest(
				"",
				// ==>
				""
		);
	}

	@Test
	public void unusualName() throws Exception {
		File input = createFile("no-extension",
				"server.port: 6789"
		);
		YamlConversionResult result = YamlConverter.builder().file(input).build().convert();
		assertOkStatus(result.getStatus());

		assertThat(result.getYaml()).isEqualTo("server:\n" + "  port: '6789'\n");
	}

	@Test
	public void multipleAssignmentProblem() throws Exception {
		do_conversionTest(
				"some.property=something\n" +
				"some.property=something-else",
				// ==>
				"some:\n" +
				"  property: something-else\n",
				(status) -> {
					assertThat(status.getSeverity()).isEqualTo(0);
				}
		);
	}

	@Test
	public void scalarAndMapConflict() throws Exception {
		do_conversionTest(
				"some.property=a-scalar\n" +
				"some.property.sub=sub-value",
				// ==>
				"some:\n" +
				"  property:\n" +
				"    sub: sub-value\n",
				(status) -> {
					assertStatus(status, YamlConversionStatus.ERROR,
							"Direct assignment 'some.property=a-scalar' can not be combined with sub-property assignment 'some.property.sub...'");
				}
		);
	}

	@Test
	public void scalarAndMapConflictFlatten() throws Exception {
		do_conversionTest(
				Mode.FLATTEN,
				"some.property=a-scalar\n" +
				"some.property.sub=sub-value",
				// ==>
				"some:\n" +
				"  property: a-scalar\n" +
				"  property.sub: sub-value\n",
				(status) -> {
					assertThat(status.getSeverity()).isEqualTo(0);
				}
		);
	}

	@Test
	public void forceFlatten() throws Exception {
		do_conversionTest(
				Mode.FLATTEN,
			Collections.singletonList("some.property"),
				"some.property.sub1.sub2=sub-value",
				// ==>
				"some:\n" +
				"  property:\n" +
				"    sub1.sub2: sub-value\n",
				(status) -> {
					assertThat(status.getSeverity()).isEqualTo(0);
				}
		);
	}

	@Test
	public void forceFlattenMulti() throws Exception {
		do_conversionTest(
				Mode.FLATTEN,
			Collections.singletonList("some.property"),
				"some.property.sub1.sub2=sub-value1\n" +
				"some.property.sub3.sub4=sub-value2",
				// ==>
				"some:\n" +
				"  property:\n" +
				"    sub1.sub2: sub-value1\n" +
				"    sub3.sub4: sub-value2\n",
				(status) -> {
					assertThat(status.getSeverity()).isEqualTo(0);
				}
		);
	}

	@Test
	public void forceFlattenMultiUseRegex() throws Exception {
		do_conversionTest(
				Mode.FLATTEN,
			Collections.singletonList("[a-z]*2\\.property"),
				"some1.property.sub1.sub2=sub-value1\n" +
				"some1.property.sub3.sub4=sub-value2\n" +
				"some2.property.sub5.sub6=sub-value1\n" +
				"some2.property.sub7.sub8=sub-value2",
				// ==>
				"some1:\n" +
				"  property:\n" +
				"    sub1:\n" +
				"      sub2: sub-value1\n" +
				"    sub3:\n" +
				"      sub4: sub-value2\n" +
				"some2:\n" +
				"  property:\n" +
				"    sub5.sub6: sub-value1\n" +
				"    sub7.sub8: sub-value2\n",
				(status) -> {
					assertThat(status.getSeverity()).isEqualTo(0);
				}
		);
	}

	@Test
	public void scalarAndMapConflictDeepFlatten() throws Exception {
		do_conversionTest(
				Mode.FLATTEN,
				"log4j.appender.stdout=org.apache.log4j.ConsoleAppender\n" +
				"log4j.appender.stdout.Target:System.out\n" +
				"log4j.appender.stdout.layout:org.apache.log4j.PatternLayout\n" +
				"log4j.appender.stdout.layout.ConversionPattern:%d{ABSOLUTE} %5p %c{1}:%L - %m%n\n" +
				"log4j.rootLogger:INFO, stdout\n" +
				"log4j.logger.org.hibernate:DEBUG\n" +
				"log4j.logger.org.hibernate.type:ALL\n",
				// ==>
				"log4j:\n" +
				"  appender:\n" +
				"    stdout: org.apache.log4j.ConsoleAppender\n" +
				"    stdout.Target: System.out\n" +
				"    stdout.layout: org.apache.log4j.PatternLayout\n" +
				"    stdout.layout.ConversionPattern: '%d{ABSOLUTE} %5p %c{1}:%L - %m%n'\n" +
				"  logger:\n" +
				"    org:\n" +
				"      hibernate: DEBUG\n" +
				"      hibernate.type: ALL\n" +
				"  rootLogger: INFO, stdout\n",
				(status) -> {
					assertThat(status.getSeverity()).isEqualTo(0);
				}
		);
	}

	@Test
	public void scalarAndMapConflictDeepFlatten2() throws Exception {
		do_conversionTest(
				Mode.FLATTEN,
				"log4j.appender.stdout=org.apache.log4j.ConsoleAppender\n" +
				"log4j.appender.stdout.Target:System.out\n" +
				"log4j.appender.stdout.layout:org.apache.log4j.PatternLayout\n" +
				"log4j.appender.stdout.layout.ConversionPattern:%d{ABSOLUTE} %5p %c{1}:%L - %m%n\n" +
				"\n" +
				"log4j.rootLogger:INFO, stdout\n" +
				"\n" +
				"log4j.logger.org.hibernate:DEBUG\n" +
				"\n" +
				"log4j.logger.org.hibernate.type:ALL\n" +
				"\n",
				// ==>
				"log4j:\n" +
				"  appender:\n" +
				"    stdout: org.apache.log4j.ConsoleAppender\n" +
				"    stdout.Target: System.out\n" +
				"    stdout.layout: org.apache.log4j.PatternLayout\n" +
				"    stdout.layout.ConversionPattern: '%d{ABSOLUTE} %5p %c{1}:%L - %m%n'\n" +
				"  logger:\n" +
				"    org:\n" +
				"      hibernate: DEBUG\n" +
				"      hibernate.type: ALL\n" +
				"  rootLogger: INFO, stdout\n",
				(status) -> {
					assertThat(status.getSeverity()).isEqualTo(0);
				}
		);
	}

	@Test
	public void scalarAndSequenceConflict() throws Exception {
		do_conversionTest(
				"some.property=a-scalar\n" +
				"some.property[0]=zero\n" +
				"some.property[1]=one\n",
				// ==>
				"some:\n" +
				"  property:\n" +
				"  - zero\n" +
				"  - one\n",
				(status) -> {
					assertStatus(status, YamlConversionStatus.ERROR,
							"Direct assignment 'some.property=a-scalar' can not be combined with sequence assignment 'some.property[0]...'");
				}
		);
	}

	@Test
	public void mapAndSequenceConflict() throws Exception {
		do_conversionTest(
				"some.property.abc=val1\n" +
				"some.property.def=val2\n" +
				"some.property[0]=zero\n" +
				"some.property[1]=one\n",
				// ==>
				"some:\n" +
				"  property:\n" +
				"    '0': zero\n" +
				"    '1': one\n" +
				"    abc: val1\n" +
				"    def: val2\n",
				(status) -> {
					assertStatus(status, YamlConversionStatus.WARNING,
							"'some.property' has some entries that look like list items and others that look like map entries");
				}
		);
	}

	@Test
	public void scalarAndMapAndSequenceConflict() throws Exception {
		do_conversionTest(
				"some.property=a-scalar\n" +
				"some.property.abc=val1\n" +
				"some.property.def=val2\n" +
				"some.property[0]=zero\n" +
				"some.property[1]=one\n",
				// ==>
				"some:\n" +
				"  property:\n" +
				"    '0': zero\n" +
				"    '1': one\n" +
				"    abc: val1\n" +
				"    def: val2\n",
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
