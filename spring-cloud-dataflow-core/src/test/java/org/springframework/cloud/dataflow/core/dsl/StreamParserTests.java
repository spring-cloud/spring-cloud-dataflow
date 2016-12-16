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

package org.springframework.cloud.dataflow.core.dsl;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Properties;

import org.junit.Test;

/**
 * Parse streams and verify either the correct abstract syntax tree is produced or the current exception comes out.
 *
 * @author Andy Clement
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @author Eric Bottard
 */
public class StreamParserTests {

	private StreamNode sn;


	// This is not a well formed stream but we are testing single app parsing
	@Test
	public void oneApp() {
		sn = parse("foo");
		assertEquals(1, sn.getAppNodes().size());
		AppNode appNode = sn.getApp("foo");
		assertEquals("foo", appNode.getName());
		assertEquals(0, appNode.getArguments().length);
		assertEquals(0, appNode.startPos);
		assertEquals(3, appNode.endPos);
	}

	@Test
	public void hyphenatedAppName() {
		sn = parse("gemfire-cq");
		assertEquals("[(AppNode:gemfire-cq:0>10)]", sn.stringify(true));
	}

	// Just to make the testing easier the parser supports stream naming easier.
	@Test
	public void streamNaming() {
		sn = parse("mystream = foo");
		assertEquals("[mystream = (AppNode:foo:11>14)]", sn.stringify(true));
		assertEquals("mystream", sn.getName());
	}

	@Test
	public void testStreamNameAsAppName() {
		String streamName = "bar";
		String stream = "bar = foo | bar";
		sn = parse(stream);
		assertEquals(streamName, sn.getName());
	}

	// Pipes are used to connect apps
	@Test
	public void twoApps() {
		StreamNode ast = parse("foo | bar");
		assertEquals("[(AppNode:foo:0>3)(AppNode:bar:6>9)]", ast.stringify(true));
	}

	// Apps can be labeled
	@Test
	public void appLabels() {
		StreamNode ast = parse("label: http");
		assertEquals("[((Label:label:0>5) AppNode:http:0>11)]", ast.stringify(true));
	}

	@Test
	public void appLabels3() {
		StreamNode ast = parse("food = http | label3: foo");
		assertEquals(
				"[food = (AppNode:http:7>11)((Label:label3:14>20) AppNode:foo:14>25)]",
				ast.stringify(true));

		sn = parse("http | foo: bar | file");
		assertEquals("[(AppNode:http)((Label:foo) AppNode:bar)(AppNode:file)]", sn.stringify());

		checkForParseError("http | foo: goggle: bar | file", DSLMessage.UNEXPECTED_DATA_AFTER_STREAMDEF,
				18);
		checkForParseError("http | foo :bar | file", DSLMessage.NO_WHITESPACE_BETWEEN_LABEL_NAME_AND_COLON, 11);
	}

	// Apps can take parameters
	@Test
	public void oneAppWithParam() {
		StreamNode ast = parse("foo --name=value");
		assertEquals("[(AppNode:foo --name=value:0>16)]", ast.stringify(true));
	}

	// Apps can take two parameters
	@Test
	public void oneAppWithTwoParams() {
		StreamNode sn = parse("foo --name=value --x=y");
		List<AppNode> appNodes = sn.getAppNodes();
		assertEquals(1, appNodes.size());

		AppNode mn = appNodes.get(0);
		assertEquals("foo", mn.getName());
		ArgumentNode[] args = mn.getArguments();
		assertNotNull(args);
		assertEquals(2, args.length);
		assertEquals("name", args[0].getName());
		assertEquals("value", args[0].getValue());
		assertEquals("x", args[1].getName());
		assertEquals("y", args[1].getValue());

		assertEquals("[(AppNode:foo --name=value --x=y:0>22)]", sn.stringify(true));
	}

	@Test
	public void testParameters() {
		String app = "gemfire-cq --query='Select * from /Stocks where symbol=''VMW''' --regionName=foo --foo=bar";
		StreamNode ast = parse(app);
		AppNode gemfireApp = ast.getApp("gemfire-cq");
		Properties parameters = gemfireApp.getArgumentsAsProperties();
		assertEquals(3, parameters.size());
		assertEquals("Select * from /Stocks where symbol='VMW'", parameters.get("query"));
		assertEquals("foo", parameters.get("regionName"));
		assertEquals("bar", parameters.get("foo"));

		app = "test";
		parameters = parse(app).getApp("test").getArgumentsAsProperties();
		assertEquals(0, parameters.size());

		app = "foo --x=1 --y=two ";
		parameters = parse(app).getApp("foo").getArgumentsAsProperties();
		assertEquals(2, parameters.size());
		assertEquals("1", parameters.get("x"));
		assertEquals("two", parameters.get("y"));

		app = "foo --x=1a2b --y=two ";
		parameters = parse(app).getApp("foo").getArgumentsAsProperties();
		assertEquals(2, parameters.size());
		assertEquals("1a2b", parameters.get("x"));
		assertEquals("two", parameters.get("y"));

		app = "foo --x=2";
		parameters = parse(app).getApp("foo").getArgumentsAsProperties();
		assertEquals(1, parameters.size());
		assertEquals("2", parameters.get("x"));

		app = "--foo = bar";
		try {
			parse(app);
			fail(app + " is invalid. Should throw exception");
		}
		catch (Exception e) {
			// success
		}
	}

	@Test
	public void testInvalidApps() {
		String config = "test | foo--x=13";
		StreamParser parser = new StreamParser("t", config);
		try {
			parser.parse();
			fail(config + " is invalid. Should throw exception");
		}
		catch (Exception e) {
			// success
		}
	}

	@Test
	public void tapWithLabelReference() {
		parse("mystream = http | filter | group1: transform | group2: transform | file");
		StreamNode ast = parse(":mystream.group1 > file");

		assertEquals("[(mystream.group1)>(AppNode:file)]", ast.stringify());
		ast = parse(":mystream.group2 > file");
		assertEquals("[(mystream.group2)>(AppNode:file)]", ast.stringify());
	}

	@Test
	public void tapWithQualifiedAppReference() {
		parse("mystream = http | foobar | file");
		StreamNode sn = parse(":mystream.foobar > file");
		assertEquals("[(mystream.foobar:1>16)>(AppNode:file:19>23)]", sn.stringify(true));
	}

	@Test
	public void expressions_xd159() {
		StreamNode ast = parse("foo | transform --expression=--payload | bar");
		AppNode mn = ast.getApp("transform");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("--payload", props.get("expression"));
	}

	@Test
	public void expressions_xd159_2() {
		// need quotes around an argument value with a space in it
		checkForParseError("foo | transform --expression=new StringBuilder(payload).reverse() | bar",
				DSLMessage.UNEXPECTED_DATA, 46);
	}

	@Test
	public void ensureStreamNamesValid_xd1344() {
		// Similar rules to a java identifier but also allowed '-' after the first char
		checkForIllegalStreamName("foo.bar", "http | transform | sink");
		checkForIllegalStreamName("-bar", "http | transform | sink");
		checkForIllegalStreamName(".bar", "http | transform | sink");
		checkForIllegalStreamName("foo-.-bar", "http | transform | sink");
		checkForIllegalStreamName("0foobar", "http | transform | sink");
		checkForIllegalStreamName("foo%bar", "http | transform | sink");
		parse("foo-bar", "http | transform | sink");
		parse("foo_bar", "http | transform | sink");
	}

	@Test
	public void expressions_xd159_3() {
		StreamNode ast = parse("foo |  transform --expression='new StringBuilder(payload).reverse()' | bar");
		AppNode mn = ast.getApp("transform");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("new StringBuilder(payload).reverse()", props.get("expression"));
	}

	@Test
	public void testUnbalancedSingleQuotes() {
		checkForParseError("foo | bar --expression='select foo", DSLMessage.NON_TERMINATING_QUOTED_STRING, 23);
	}

	@Test
	public void testUnbalancedDoubleQuotes() {
		checkForParseError("foo | bar --expression=\"select foo", DSLMessage.NON_TERMINATING_DOUBLE_QUOTED_STRING, 23);
	}

	@Test
	public void appArguments_xd1613() {
		StreamNode ast = null;

		// notice no space between the ' and final >
		ast = parse(":producer > transform --expression='payload.toUpperCase()' | filter --expression='payload.length() > 4'> :consumer");
		assertEquals("payload.toUpperCase()", ast.getApp("transform").getArguments()[0].getValue());
		assertEquals("payload.length() > 4", ast.getApp("filter").getArguments()[0].getValue());

		ast = parse("time | transform --expression='T(org.joda.time.format.DateTimeFormat).forPattern(\"yyyy-MM-dd HH:mm:ss\").parseDateTime(payload)'");
		assertEquals(
				"T(org.joda.time.format.DateTimeFormat).forPattern(\"yyyy-MM-dd HH:mm:ss\").parseDateTime(payload)",
				ast.getApp("transform").getArguments()[0].getValue());

		// allow for pipe/semicolon if quoted
		ast = parse("http | transform --outputType='text/plain|charset=UTF-8'  | log");
		assertEquals("text/plain|charset=UTF-8", ast.getApp("transform").getArguments()[0].getValue());

		ast = parse("http | transform --outputType='text/plain;charset=UTF-8'  | log");
		assertEquals("text/plain;charset=UTF-8", ast.getApp("transform").getArguments()[0].getValue());

		// Want to treat all of 'hi'+payload as the argument value
		ast = parse("http | transform --expression='hi'+payload | log");
		assertEquals("'hi'+payload", ast.getApp("transform").getArguments()[0].getValue());

		// Want to treat all of payload+'hi' as the argument value
		ast = parse("http | transform --expression=payload+'hi' | log");
		assertEquals("payload+'hi'", ast.getApp("transform").getArguments()[0].getValue());

		// Alternatively, can quote all around it to achieve the same thing
		ast = parse("http | transform --expression='payload+''hi''' | log");
		assertEquals("payload+'hi'", ast.getApp("transform").getArguments()[0].getValue());
		ast = parse("http | transform --expression='''hi''+payload' | log");
		assertEquals("'hi'+payload", ast.getApp("transform").getArguments()[0].getValue());

		ast = parse("http | transform --expression=\"payload+'hi'\" | log");
		assertEquals("payload+'hi'", ast.getApp("transform").getArguments()[0].getValue());
		ast = parse("http | transform --expression=\"'hi'+payload\" | log");
		assertEquals("'hi'+payload", ast.getApp("transform").getArguments()[0].getValue());

		ast = parse("http | transform --expression=payload+'hi'--param2='foobar' | log");
		assertEquals("payload+'hi'--param2='foobar'", ast.getApp("transform").getArguments()[0].getValue());

		ast = parse("http | transform --expression='hi'+payload--param2='foobar' | log");
		assertEquals("'hi'+payload--param2='foobar'", ast.getApp("transform").getArguments()[0].getValue());

		// This also works, which is cool
		ast = parse("http | transform --expression='hi'+'world' | log");
		assertEquals("'hi'+'world'", ast.getApp("transform").getArguments()[0].getValue());
		ast = parse("http | transform --expression=\"'hi'+'world'\" | log");
		assertEquals("'hi'+'world'", ast.getApp("transform").getArguments()[0].getValue());

		ast = parse("http | filter --expression=payload.matches('hello world') | log");
		assertEquals("payload.matches('hello world')", ast.getApp("filter").getArguments()[0].getValue());

		ast = parse("http | transform --expression='''hi''' | log");
		assertEquals("'hi'", ast.getApp("transform").getArguments()[0].getValue());

		ast = parse("http | transform --expression=\"''''hi''''\" | log");
		assertEquals("''''hi''''", ast.getApp("transform").getArguments()[0].getValue());
	}

	@Test
	public void expressions_xd159_4() {
		StreamNode ast = parse("foo |  transform --expression=\"'Hello, world!'\" | bar");
		AppNode mn = ast.getApp("transform");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("'Hello, world!'", props.get("expression"));
		ast = parse("foo |  transform --expression='''Hello, world!''' | bar");
		mn = ast.getApp("transform");
		props = mn.getArgumentsAsProperties();
		assertEquals("'Hello, world!'", props.get("expression"));
		// Prior to the change for XD-1613, this error should point to the comma:
		// checkForParseError("foo |  transform --expression=''Hello, world!'' | bar", DSLMessage.UNEXPECTED_DATA,
		// 37);
		// but now it points to the !
		checkForParseError("foo |  transform --expression=''Hello, world!'' | bar", DSLMessage.UNEXPECTED_DATA, 44);
	}

	@Test
	public void expressions_gh1() {
		StreamNode ast = parse("http --port=9014 | filter --expression=\"payload == 'foo'\" | log");
		AppNode mn = ast.getApp("filter");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("payload == 'foo'", props.get("expression"));
	}

	@Test
	public void expressions_gh1_2() {
		StreamNode ast = parse("http --port=9014 | filter --expression='new Foo()' | log");
		AppNode mn = ast.getApp("filter");
		Properties props = mn.getArgumentsAsProperties();
		assertEquals("new Foo()", props.get("expression"));
	}

	@Test
	public void sourceDestination() {
		StreamNode sn = parse(":foobar > file");
		assertEquals("[(foobar:1>7)>(AppNode:file:10>14)]", sn.stringify(true));
	}

	@Test
	public void sinkDestination() {
		StreamNode sn = parse("http > :foo");
		assertEquals("[(AppNode:http:0>4)>(foo:8>11)]", sn.stringify(true));
	}

	@Test
	public void destinationVariants() {
		checkForParseError("http > :test value", DSLMessage.UNEXPECTED_DATA_AFTER_STREAMDEF, 13);
		checkForParseError(":boo .xx > file", DSLMessage.NO_WHITESPACE_IN_DESTINATION_DEFINITION, 5);
		checkForParseError(":boo . xx > file", DSLMessage.NO_WHITESPACE_IN_DESTINATION_DEFINITION, 5);
		checkForParseError(":boo. xx > file", DSLMessage.NO_WHITESPACE_IN_DESTINATION_DEFINITION, 6);
		checkForParseError(":boo.xx. yy > file", DSLMessage.NO_WHITESPACE_IN_DESTINATION_DEFINITION, 9);
		checkForParseError(":boo.xx .yy > file", DSLMessage.NO_WHITESPACE_IN_DESTINATION_DEFINITION, 8);
		checkForParseError(":boo.xx . yy > file", DSLMessage.NO_WHITESPACE_IN_DESTINATION_DEFINITION, 8);

		sn = parse("wibble: http > :bar");
		assertEquals("[((Label:wibble) AppNode:http)>(bar)]", sn.stringify());
	}

	@Test
	public void sourceDestination2() {
		parse("foo = http | bar | file");
		StreamNode ast = parse(":foo.bar > file");
		assertEquals("[(foo.bar:1>8)>(AppNode:file:11>15)]", ast.stringify(true));
		assertEquals("foo.bar", ast.getSourceDestinationNode().getDestinationName());
	}

	@Test
	public void sourceTapDestination() {
		parse("mystream = http | file");
		StreamNode ast = parse(":mystream.http > file");
		assertEquals(
				"[(mystream.http:1>14)>(AppNode:file:17>21)]",
				ast.stringify(true));
		SourceDestinationNode sourceDestinationNode = ast.getSourceDestinationNode();
		assertEquals("mystream.http", sourceDestinationNode.getDestinationName());
	}

	@Test
	public void nameSpaceTestWithSpaces() {
		checkForParseError("trigger > :myjob   too", DSLMessage.UNEXPECTED_DATA_AFTER_STREAMDEF, 19, "too");
	}

	@Test
	public void errorCases01() {
		checkForParseError(".", DSLMessage.EXPECTED_APPNAME, 0, ".");
		checkForParseError(";", DSLMessage.EXPECTED_APPNAME, 0, ";");
	}

	@Test
	public void errorCases04() {
		checkForParseError("foo bar=yyy", DSLMessage.UNEXPECTED_DATA_AFTER_STREAMDEF, 4, "bar");
		checkForParseError("foo bar", DSLMessage.UNEXPECTED_DATA_AFTER_STREAMDEF, 4, "bar");
	}

	@Test
	public void errorCases05() {
		checkForParseError("foo --", DSLMessage.OOD, 6);
		checkForParseError("foo --bar", DSLMessage.OOD, 9);
		checkForParseError("foo --bar=", DSLMessage.OOD, 10);
	}

	@Test
	public void errorCases06() {
		checkForParseError("|", DSLMessage.EXPECTED_APPNAME, 0);
	}

	@Test
	public void errorCases07() {
		checkForParseError("foo > bar", DSLMessage.EXPECTED_DESTINATION_PREFIX, 6, "bar");
		checkForParseError(":foo >", DSLMessage.OOD, 6);
		checkForParseError(":foo > --2323", DSLMessage.EXPECTED_APPNAME, 7, "--");
		checkForParseError(":foo > *", DSLMessage.UNEXPECTED_DATA, 7, "*");
	}

	@Test
	public void errorCases08() {
		checkForParseError(":foo | bar", DSLMessage.EXPECTED_APPNAME, 0, ":");
	}

	@Test
	public void errorCases09() {
		checkForParseError("* = http | file", DSLMessage.UNEXPECTED_DATA, 0, "*");
		checkForParseError(": = http | file", DSLMessage.ILLEGAL_STREAM_NAME, 0, ":");
	}

	@Test
	public void duplicateExplicitLabels() {
		checkForParseError("xxx: http | xxx: file", DSLMessage.DUPLICATE_LABEL, 12, "xxx", "http", 0, "file", 1);
		checkForParseError("xxx: http | yyy: filter | transform | xxx: transform | file",
				DSLMessage.DUPLICATE_LABEL, 38, "xxx", "http", 0, "transform", 3);
		checkForParseError("xxx: http | yyy: filter | transform | xxx: transform | xxx: file",
				DSLMessage.DUPLICATE_LABEL, 38, "xxx", "http", 0, "transform", 3);
	}

	@Test
	public void addingALabelLiftsAmbiguity() {
		StreamNode ast = parse("file | out: file");
		assertEquals("file", ast.getAppNodes().get(0).getLabelName());
		assertEquals("out", ast.getAppNodes().get(1).getLabelName());

	}

	@Test
	public void duplicateImplicitLabels() {
		checkForParseError("http | filter | transform | transform | file",
				DSLMessage.DUPLICATE_LABEL, 28, "transform", "transform", 2, "transform", 3);
	}

	@Test
	public void tapWithLabels() {
		parse("mystream = http | flibble: transform | file");
		sn = parse(":mystream.flibble > file");
		assertEquals("mystream.flibble", sn.getSourceDestinationNode().getDestinationName());
	}

	@Test
	public void bridge01() {
		StreamNode sn = parse(":bar > :boo");
		assertEquals("[(bar:1>4)>(AppNode:bridge:5>6)>(boo:8>11)]", sn.stringify(true));
	}

	@Test
	public void testSourceDestinationArgs() {
		StreamNode sn = parse(":test --group=test > file");
		assertEquals("[(test:1>5 --group=test)>(AppNode:file:21>25)]", sn.stringify(true));
	}

	// Parameters must be constructed via adjacent tokens
	@Test
	public void needAdjacentTokensForParameters() {
		checkForParseError("foo -- name=value", DSLMessage.NO_WHITESPACE_BEFORE_ARG_NAME, 7);
		checkForParseError("foo --name =value", DSLMessage.NO_WHITESPACE_BEFORE_ARG_EQUALS, 11);
		checkForParseError("foo --name= value", DSLMessage.NO_WHITESPACE_BEFORE_ARG_VALUE, 12);
	}

	// ---

	@Test
	public void testComposedOptionNameErros() {
		checkForParseError("foo --name.=value", DSLMessage.NOT_EXPECTED_TOKEN, 11);
		checkForParseError("foo --name .sub=value", DSLMessage.NO_WHITESPACE_IN_DOTTED_NAME, 11);
		checkForParseError("foo --name. sub=value", DSLMessage.NO_WHITESPACE_IN_DOTTED_NAME, 12);
	}

	@Test
	public void testXD2416() {
		StreamNode ast = parse("http | transform --expression='payload.replace(\"abc\", \"\")' | log");
		assertThat((String)ast.getAppNodes().get(1).getArgumentsAsProperties().get("expression"), equalTo("payload.replace(\"abc\", \"\")"));

		ast = parse("http | transform --expression='payload.replace(\"abc\", '''')' | log");
		assertThat((String) ast.getAppNodes().get(1).getArgumentsAsProperties().get("expression"), equalTo("payload.replace(\"abc\", '')"));
	}

	StreamNode parse(String streamDefinition) {
		return new StreamParser(streamDefinition).parse();
	}

	StreamNode parse(String streamName, String streamDefinition) {
		return new StreamParser(streamName, streamDefinition).parse();
	}

	private void checkForIllegalStreamName(String streamName, String streamDef) {
		try {
			StreamNode sn = parse(streamName, streamDef);
			fail("expected to fail but parsed " + sn.stringify());
		}
		catch (ParseException e) {
			assertEquals(DSLMessage.ILLEGAL_STREAM_NAME, e.getMessageCode());
			assertEquals(0, e.getPosition());
			assertEquals(streamName, e.getInserts()[0]);
		}
	}

	private void checkForParseError(String stream, DSLMessage msg, int pos, Object... inserts) {
		try {
			StreamNode sn = parse(stream);
			fail("expected to fail but parsed " + sn.stringify());
		}
		catch (ParseException e) {
			assertEquals(msg, e.getMessageCode());
			assertEquals(pos, e.getPosition());
			if (inserts != null) {
				for (int i = 0; i < inserts.length; i++) {
					assertEquals(inserts[i], e.getInserts()[i]);
				}
			}
		}
	}
}
