/*
 * Copyright 2015 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Parse tasks and verify either the correct abstract syntax tree is produced or the current exception comes out.
 *
 * @author Andy Clement
 * @author David Turanski
 * @author Michael Minella
 * @author Eric Bottard
 */
public class TaskParserTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AppNode appNode;

	@Test
	public void oneApp() {
		appNode = parse("foo");
		assertEquals("foo", appNode.getName());
		assertEquals(0, appNode.getArguments().length);
		assertEquals(0, appNode.startPos);
		assertEquals(3, appNode.endPos);
	}

	@Test
	public void hyphenatedAppName() {
		appNode = parse("gemfire-cq");
		assertEquals("(AppNode:gemfire-cq:0>10)", appNode.stringify(true));
	}

	// Apps can take parameters
	@Test
	public void oneAppWithParam() {
		AppNode ast = parse("foo --name=value");
		assertEquals("(AppNode:foo --name=value:0>16)", ast.stringify(true));
	}

	// Apps can take two parameters
	@Test
	public void oneAppWithTwoParams() {
		AppNode appNode = parse("foo --name=value --x=y");

		assertEquals("foo", appNode.getName());
		ArgumentNode[] args = appNode.getArguments();
		assertNotNull(args);
		assertEquals(2, args.length);
		assertEquals("name", args[0].getName());
		assertEquals("value", args[0].getValue());
		assertEquals("x", args[1].getName());
		assertEquals("y", args[1].getValue());

		assertEquals("(AppNode:foo --name=value --x=y:0>22)", appNode.stringify(true));
	}

	@Test
	public void testParameters() {
		String module = "gemfire-cq --query='Select * from /Stocks where symbol=''VMW''' --regionName=foo --foo=bar";
		AppNode gemfireApp = parse(module);
		Properties parameters = gemfireApp.getArgumentsAsProperties();
		assertEquals(3, parameters.size());
		assertEquals("Select * from /Stocks where symbol='VMW'", parameters.get("query"));
		assertEquals("foo", parameters.get("regionName"));
		assertEquals("bar", parameters.get("foo"));

		module = "test";
		parameters = parse(module).getArgumentsAsProperties();
		assertEquals(0, parameters.size());

		module = "foo --x=1 --y=two ";
		parameters = parse(module).getArgumentsAsProperties();
		assertEquals(2, parameters.size());
		assertEquals("1", parameters.get("x"));
		assertEquals("two", parameters.get("y"));

		module = "foo --x=1a2b --y=two ";
		parameters = parse(module).getArgumentsAsProperties();
		assertEquals(2, parameters.size());
		assertEquals("1a2b", parameters.get("x"));
		assertEquals("two", parameters.get("y"));

		module = "foo --x=2";
		parameters = parse(module).getArgumentsAsProperties();
		assertEquals(1, parameters.size());
		assertEquals("2", parameters.get("x"));

		module = "--foo = bar";
		try {
			parse(module);
			fail(module + " is invalid. Should throw exception");
		}
		catch (Exception e) {
			// success
		}
	}

	@Test
	public void testInvalidApps() {
		String config = "foo--x=13";
		TaskParser parser = new TaskParser("t", config);
		try {
			parser.parse();
			fail(config + " is invalid. Should throw exception");
		}
		catch (Exception e) {
			// success
		}
	}

	@Test
	public void expressions_xd159() {
		AppNode appNode = parse("transform --expression=--payload");
		Properties props = appNode.getArgumentsAsProperties();
		assertEquals("--payload", props.get("expression"));
	}

	@Test
	public void expressions_xd159_2() {
		// need quotes around an argument value with a space in it
		checkForParseError("transform --expression=new StringBuilder(payload).reverse()",
				DSLMessage.UNEXPECTED_DATA, 40);
	}

	@Test
	public void ensureTaskNamesValid_xd1344() {
		// Similar rules to a java identifier but also allowed '-' after the first char
		checkForIllegalTaskName("foo.bar", "task");
		checkForIllegalTaskName("-bar", "task");
		checkForIllegalTaskName(".bar", "task");
		checkForIllegalTaskName("foo-.-bar", "task");
		checkForIllegalTaskName("0foobar", "task");
		checkForIllegalTaskName("foo%bar", "task");
		parse("foo-bar", "task");
		parse("foo_bar", "task");
	}

	@Test
	public void expressions_xd159_3() {
		AppNode appNode = parse("transform --expression='new StringBuilder(payload).reverse()'");
		Properties props = appNode.getArgumentsAsProperties();
		assertEquals("new StringBuilder(payload).reverse()", props.get("expression"));
	}

	@Test
	public void expressions_xd159_4() {
		AppNode appNode = parse("transform --expression=\"'Hello, world!'\"");
		Properties props = appNode.getArgumentsAsProperties();
		assertEquals("'Hello, world!'", props.get("expression"));
		appNode = parse("transform --expression='''Hello, world!'''");
		props = appNode.getArgumentsAsProperties();
		assertEquals("'Hello, world!'", props.get("expression"));
		// Prior to the change for XD-1613, this error should point to the comma:
		// checkForParseError("foo |  transform --expression=''Hello, world!'' | bar", DSLMessage.UNEXPECTED_DATA,
		// 37);
		// but now it points to the !
		checkForParseError("transform --expression=''Hello, world!''", DSLMessage.UNEXPECTED_DATA, 37);
	}

	@Test
	public void expressions_gh1() {
		AppNode appNode = parse("filter --expression=\"payload == 'foo'\"");
		Properties props = appNode.getArgumentsAsProperties();
		assertEquals("payload == 'foo'", props.get("expression"));
	}

	@Test
	public void expressions_gh1_2() {
		AppNode appNode = parse("filter --expression='new Foo()'");
		Properties props = appNode.getArgumentsAsProperties();
		assertEquals("new Foo()", props.get("expression"));
	}

	@Test
	public void errorCases01() {
		checkForParseError(".", DSLMessage.EXPECTED_APPNAME, 0, ".");
		checkForParseError(";", DSLMessage.EXPECTED_APPNAME, 0, ";");
	}

	@Test
	public void errorCases04() {
		checkForParseError("foo bar=yyy", DSLMessage.MORE_INPUT, 4, "bar");
		checkForParseError("foo bar", DSLMessage.MORE_INPUT, 4, "bar");
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
		AppNode appNode = parse("transform --expression='payload.replace(\"abc\", \"\")'");
		assertEquals(appNode.getArgumentsAsProperties().get("expression"), "payload.replace(\"abc\", \"\")");

		appNode = parse("transform --expression='payload.replace(\"abc\", '''')'");
		assertEquals(appNode.getArgumentsAsProperties().get("expression"), "payload.replace(\"abc\", '')");
	}

	@Test
	public void testUnbalancedSingleQuotes() {
		checkForParseError("timestamp --format='YYYY", DSLMessage.NON_TERMINATING_QUOTED_STRING, 19);
	}

	@Test
	public void testUnbalancedDoubleQuotes() {
		checkForParseError("timestamp --format=\"YYYY", DSLMessage.NON_TERMINATING_DOUBLE_QUOTED_STRING, 19);
	}



	AppNode parse(String taskDefinition) {
		return new TaskParser(taskDefinition).parse();
	}

	AppNode parse(String taskName, String taskDefinition) {
		return new TaskParser(taskName, taskDefinition).parse();
	}

	private void checkForIllegalTaskName(String taskName, String taskDef) {
		try {
			AppNode sn = parse(taskName, taskDef);
			fail("expected to fail but parsed " + sn.stringify());
		}
		catch (ParseException e) {
			assertEquals(DSLMessage.ILLEGAL_TASK_NAME, e.getMessageCode());
			assertEquals(0, e.getPosition());
			assertEquals(taskName, e.getInserts()[0]);
		}
	}

	private void checkForParseError(String task, DSLMessage msg, int pos, Object... inserts) {
		try {
			AppNode sn = parse(task);
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
