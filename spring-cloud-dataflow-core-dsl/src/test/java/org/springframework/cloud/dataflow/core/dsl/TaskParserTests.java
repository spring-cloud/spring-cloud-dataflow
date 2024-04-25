/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.core.dsl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.dsl.graph.Graph;
import org.springframework.cloud.dataflow.core.dsl.graph.Link;
import org.springframework.cloud.dataflow.core.dsl.graph.Node;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the parser and visitor infrastructure. Check it accepts expected data and
 * correctly handles bad data. Check that the parsed out converts to/from the graph format
 * that the UI can use.
 *
 * @author Andy Clement
 * @author David Turanski
 * @author Michael Minella
 * @author Eric Bottard
 */
@SpringJUnitConfig
public class TaskParserTests {

	private TaskNode ctn;

	private TaskAppNode appNode;

	@Test
	public void oneApp() {
		TaskNode taskNode = parse("foo");
		assertFalse(taskNode.isComposed());
		TaskAppNode appNode = taskNode.getTaskApp();
		assertEquals("foo", appNode.getName());
		assertEquals(0, appNode.getArguments().length);
		assertEquals(0, appNode.startPos);
		assertEquals(3, appNode.endPos);
	}

	@Test
	public void hyphenatedAppName() {
		appNode = parse("gemfire-cq").getTaskApp();
		assertEquals("gemfire-cq:0>10", appNode.stringify(true));
	}

	@Test
	public void oneAppWithParam() {
		appNode = parse("foo --name=value").getTaskApp();
		assertEquals("foo --name=value:0>16", appNode.stringify(true));
	}

	@Test
	public void oneAppWithTwoParams() {
		appNode = parse("foo --name=value --x=y").getTaskApp();

		assertEquals("foo", appNode.getName());
		ArgumentNode[] args = appNode.getArguments();
		assertNotNull(args);
		assertEquals(2, args.length);
		assertEquals("name", args[0].getName());
		assertEquals("value", args[0].getValue());
		assertEquals("x", args[1].getName());
		assertEquals("y", args[1].getValue());

		assertEquals("foo --name=value --x=y:0>22", appNode.stringify(true));
	}

	@Test
	public void testParameters() {
		String module = "gemfire-cq --query='Select * from /Stocks where symbol=''VMW''' --regionName=foo --foo=bar";
		TaskAppNode gemfireApp = parse(module).getTaskApp();
		Map<String, String> parameters = gemfireApp.getArgumentsAsMap();
		assertEquals(3, parameters.size());
		assertEquals("Select * from /Stocks where symbol='VMW'", parameters.get("query"));
		assertEquals("foo", parameters.get("regionName"));
		assertEquals("bar", parameters.get("foo"));

		module = "test";
		parameters = parse(module).getTaskApp().getArgumentsAsMap();
		assertEquals(0, parameters.size());

		module = "foo --x=1 --y=two ";
		parameters = parse(module).getTaskApp().getArgumentsAsMap();
		assertEquals(2, parameters.size());
		assertEquals("1", parameters.get("x"));
		assertEquals("two", parameters.get("y"));

		module = "foo --x=1a2b --y=two ";
		parameters = parse(module).getTaskApp().getArgumentsAsMap();
		assertEquals(2, parameters.size());
		assertEquals("1a2b", parameters.get("x"));
		assertEquals("two", parameters.get("y"));

		module = "foo --x=2";
		parameters = parse(module).getTaskApp().getArgumentsAsMap();
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
		TaskParser parser = new TaskParser("t", config, true, true);
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
		appNode = parse("transform --expression=--payload").getTaskApp();
		Map<String, String> props = appNode.getArgumentsAsMap();
		assertEquals("--payload", props.get("expression"));
	}

	@Test
	public void expressions_xd159_2() {
		// need quotes around an argument value with a space in it
		checkForParseError("transform --expression=new StringBuilder(payload).reverse()", DSLMessage.TASK_MORE_INPUT,
				27);
		appNode = parse("transform --expression='new StringBuilder(payload).reverse()'").getTaskApp();
		assertEquals("new StringBuilder(payload).reverse()", appNode.getArgumentsAsMap().get("expression"));
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
		appNode = parse("transform --expression='new StringBuilder(payload).reverse()'").getTaskApp();
		Map<String, String> props = appNode.getArgumentsAsMap();
		assertEquals("new StringBuilder(payload).reverse()", props.get("expression"));
	}

	@Test
	public void expressions_xd159_4() {
		appNode = parse("transform --expression=\"'Hello, world!'\"").getTaskApp();
		Map<String, String> props = appNode.getArgumentsAsMap();
		assertEquals("'Hello, world!'", props.get("expression"));
		appNode = parse("transform --expression='''Hello, world!'''").getTaskApp();
		props = appNode.getArgumentsAsMap();
		assertEquals("'Hello, world!'", props.get("expression"));
		// Prior to the change for XD-1613, this error should point to the comma:
		// checkForParseError("foo | transform --expression=''Hello, world!'' | bar",
		// DSLMessage.UNEXPECTED_DATA, 37);
		// but now it points to the !
		checkForParseError("transform --expression=''Hello, world!''", DSLMessage.TASK_UNEXPECTED_DATA, 37);
	}

	@Test
	public void expressions_gh1() {
		appNode = parse("filter --expression=\"payload == 'foo'\"").getTaskApp();
		Map<String, String> props = appNode.getArgumentsAsMap();
		assertEquals("payload == 'foo'", props.get("expression"));
	}

	@Test
	public void expressions_gh1_2() {
		appNode = parse("filter --expression='new Foo()'").getTaskApp();
		Map<String, String> props = appNode.getArgumentsAsMap();
		assertEquals("new Foo()", props.get("expression"));
	}

	@Test
	public void errorCases01() {
		checkForParseError(".", DSLMessage.EXPECTED_APPNAME, 0, ".");
		assertEquals("a-_", parse("foo", "a-_", true).getTaskApp().getName());
		assertEquals("a_b", parse("foo", "a_b", true).getTaskApp().getName());
		checkForParseError(";", DSLMessage.EXPECTED_APPNAME, 0, ";");
	}

	@Test
	public void errorCases04() {
		checkForParseError("foo bar=yyy", DSLMessage.TASK_MORE_INPUT, 4, "bar");
		checkForParseError("foo bar", DSLMessage.TASK_MORE_INPUT, 4, "bar");
	}

	@Test
	public void shortArgValues_2499() {
		// This is the expected result when an argument value is missing:
		checkForParseError("aaa --bbb= --ccc=ddd", DSLMessage.EXPECTED_ARGUMENT_VALUE, 11);
		// From AbstractTokenizer.isArgValueIdentifierTerminator these are the 'special chars' that should
		// terminate an argument value if not quoted:
		// "|"   ";"   "\0"   " "   "\t"   ">"   "\r"   "\n"
		// (\0 is the sentinel, wouldn't expect that in user data)
		checkForParseError("aaa --bbb=| --ccc=ddd", DSLMessage.EXPECTED_ARGUMENT_VALUE, 10);
		checkForParseError("aaa --bbb=; --ccc=ddd", DSLMessage.EXPECTED_ARGUMENT_VALUE, 10);
		checkForParseError("aaa --bbb=> --ccc=ddd", DSLMessage.EXPECTED_ARGUMENT_VALUE, 10);
		// Not sure the tabs/etc here and handled quite right during tokenization but it does error as expected
		checkForParseError("aaa --bbb=	 --ccc=ddd", DSLMessage.EXPECTED_ARGUMENT_VALUE, 12);
		checkForParseError("aaa --bbb=\t --ccc=ddd", DSLMessage.EXPECTED_ARGUMENT_VALUE, 12);
		checkForParseError("aaa --bbb=\n --ccc=ddd", DSLMessage.EXPECTED_ARGUMENT_VALUE, 12);
	}

	@Test
	public void errorCases05() {
		checkForParseError("foo --", DSLMessage.OOD, 6);
		checkForParseError("foo --bar", DSLMessage.OOD, 9);
		checkForParseError("foo --bar=", DSLMessage.OOD, 10);
	}

	@Test
	public void errorCases06() {
		// Exception thrown by tokenizer, which doesn't know that the app name is missing
		checkForParseError("|", DSLMessage.TASK_DOUBLE_OR_REQUIRED, 0);
	}

	// Parameters must be constructed via adjacent tokens
	@Test
	public void needAdjacentTokensForParameters() {
		checkForParseError("foo -- name=value", DSLMessage.NO_WHITESPACE_BEFORE_ARG_NAME, 7);
		checkForParseError("foo --name =value", DSLMessage.NO_WHITESPACE_BEFORE_ARG_EQUALS, 11);
		checkForParseError("foo --name= value", DSLMessage.NO_WHITESPACE_BEFORE_ARG_VALUE, 12);
	}

	@Test
	public void testComposedOptionNameErros() {
		checkForParseError("foo --name.=value", DSLMessage.NOT_EXPECTED_TOKEN, 11);
		checkForParseError("foo --name .sub=value", DSLMessage.NO_WHITESPACE_IN_DOTTED_NAME, 11);
		checkForParseError("foo --name. sub=value", DSLMessage.NO_WHITESPACE_IN_DOTTED_NAME, 12);
	}

	@Test
	public void testXD2416() {
		appNode = parse("transform --expression='payload.replace(\"abc\", \"\")'").getTaskApp();
		assertEquals(appNode.getArgumentsAsMap().get("expression"), "payload.replace(\"abc\", \"\")");

		appNode = parse("transform --expression='payload.replace(\"abc\", '''')'").getTaskApp();
		assertEquals(appNode.getArgumentsAsMap().get("expression"), "payload.replace(\"abc\", '')");
	}

	@Test
	public void testUnbalancedSingleQuotes() {
		checkForParseError("timestamp --format='YYYY", DSLMessage.NON_TERMINATING_QUOTED_STRING, 19);
	}

	@Test
	public void testUnbalancedDoubleQuotes() {
		checkForParseError("timestamp --format=\"YYYY", DSLMessage.NON_TERMINATING_DOUBLE_QUOTED_STRING, 19);
	}

	private void checkForIllegalTaskName(String taskName, String taskDef) {
		try {
			appNode = parse(taskName, taskDef).getTaskApp();
			fail("expected to fail but parsed " + appNode.stringify());
		}
		catch (ParseException e) {
			assertEquals(DSLMessage.ILLEGAL_TASK_NAME, e.getMessageCode());
			assertEquals(0, e.getPosition());
			assertEquals(taskName, e.getInserts()[0]);
		}
	}

	@Test
	public void executableDsl() {
		TaskNode ctn = parse("foo", "appA && appB", true);
		List<TaskApp> taskApps = ctn.getTaskApps();
		assertEquals("appA", taskApps.get(0).getName());
		assertEquals("foo-appA", taskApps.get(0).getExecutableDSLName());
		assertEquals("appB", taskApps.get(1).getName());
		assertEquals("foo-appB", taskApps.get(1).getExecutableDSLName());

		ctn = parse("bar", "appC && goo: appC", true);
		taskApps = ctn.getTaskApps();
		assertEquals("appC", taskApps.get(0).getName());
		assertEquals("bar-appC", taskApps.get(0).getExecutableDSLName());
		assertEquals("appC", taskApps.get(1).getName());
		assertEquals("bar-goo", taskApps.get(1).getExecutableDSLName());

		// flows
		assertEquals("foo-appA", parse("foo", "appA", true).toExecutableDSL());
		assertEquals("foo-appA && foo-appB", parse("foo", "appA && appB", true).toExecutableDSL());
		assertEquals("foo-appA && foo-appB && foo-appC", parse("foo", "appA && appB && appC", true).toExecutableDSL());

		assertTaskApps("foo", "appA", "foo-appA");
		assertTaskApps("foo", "appA && appB", "foo-appA", "foo-appB");
		assertTaskApps("foo", "appA && appB && appC", "foo-appA", "foo-appB", "foo-appC");

		// arguments
		assertEquals("foo-appA", parse("foo", "appA --p1=v1 --p2=v2", true).toExecutableDSL());
		assertEquals("foo-appA && foo-appB", parse("foo", "appA --p2=v2 && appB --p3=v3", true).toExecutableDSL());
		assertTaskApps("foo", "appA --p1=v2", "foo-appA:p1=v2");
		assertTaskApps("foo", "appA --p1=v2 && goo: appB --p2=v2", "foo-appA:p1=v2", "foo-goo:p2=v2");
		assertTaskApps("foo", "appA 0->x:appA --p1=v1", "foo-appA", "foo-x:p1=v1");

		// labels
		assertEquals("bar-goo", parse("bar", "goo:appA", true).toExecutableDSL());
		assertEquals("fo-aaa && fo-bbb", parse("fo", "aaa: appA && bbb: appA", true).toExecutableDSL());

		assertTaskApps("bar", "goo:appA", "bar-goo");
		assertTaskApps("bar", "appA && goo: appA", "bar-appA", "bar-goo");

		// transitions
		assertEquals("foo-appA 'c'->foo-appC && foo-appB",
				parse("foo", "appA 'c'->appC && appB", true).toExecutableDSL());
		assertEquals("foo-appA 'c'->foo-appC 'd'->foo-appD && foo-appB",
				parse("foo", "appA 'c'->appC 'd'->appD && " + "appB", true).toExecutableDSL());
		assertEquals("foo-appA 1->foo-appC 2->foo-appD && foo-appB",
				parse("foo", "appA 1->appC 2->appD && appB", true).toExecutableDSL());
		assertEquals("foo-aaa 1->foo-appC 2->:aaa", parse("foo", "aaa: appA 1->appC 2->:aaa", true).toExecutableDSL());

		// splits
		assertEquals("<foo-appA || foo-appB>", parse("foo", "<appA || appB>", true).toExecutableDSL());
		assertEquals("<foo-appA || foo-appB && foo-appC>",
				parse("foo", "<appA || appB && appC>", true).toExecutableDSL());
		assertEquals("<<foo-appA && foo-appD || foo-appE> || foo-appB>",
				parse("foo", "<<appA && appD || appE> || " + "appB>", true).toExecutableDSL());
		assertEquals("<<foo-appA || foo-x> || foo-appB>",
				parse("foo", "<<appA || x: appA> || appB>", true).toExecutableDSL());

		// splits and flows
		assertEquals("foo-AAA && foo-FFF 'FAILED'->foo-EEE && <foo-BBB || foo-CCC> && foo-DDD",
				parse("foo", "AAA && " + "FFF 'FAILED' -> EEE && <BBB||CCC> && DDD", true).toExecutableDSL());
		assertTaskApps("foo", "AAA && FFF 'FAILED' -> EEE && <BBB||CCC> && DDD", "foo-AAA", "foo-FFF", "foo-EEE",
				"foo-BBB", "foo-CCC", "foo-DDD");
		assertEquals("<test-A || test-B> && <test-C || test-D>", parse("<A || B> && <C||D>", true).toExecutableDSL());
		assertEquals("<test-A || test-B || test-C> && <test-D || test-E>",
				parse("<A || B || C> && <D||E>", true).toExecutableDSL());
		assertEquals("<test-A || test-B || test-C> && test-D", parse("<A || B || C> && D", true).toExecutableDSL());
		assertEquals("<test-A || <test-B && test-C || test-D>>", parse("<A || <B && C || D>>", true).toExecutableDSL());
		assertEquals("<test-A || <test-B || test-D && test-E>>", parse("<A || <B || D && E>>", true).toExecutableDSL());

		ctn = parse("AAA 0->BBB");
		List<TransitionNode> transitions = ((TaskAppNode) ctn.getSequences().get(0).getSeriesElement(0))
				.getTransitions();
		assertEquals("0", transitions.get(0).getStatusToCheckInDSLForm());

		ctn = parse("AAA '0'->BBB");
		transitions = ((TaskAppNode) ctn.getSequences().get(0).getSeriesElement(0)).getTransitions();
		assertEquals("'0'", transitions.get(0).getStatusToCheckInDSLForm());

		ctn = parse("AAA *->BBB '*'->CCC");
		transitions = ((TaskAppNode) ctn.getSequences().get(0).getSeriesElement(0)).getTransitions();
		assertEquals("*", transitions.get(0).getStatusToCheckInDSLForm());
		assertEquals("'*'", transitions.get(1).getStatusToCheckInDSLForm());

		assertEquals("test-AAA 'failed'->test-BBB *->test-CCC",
				parse("AAA 'failed' -> BBB * -> CCC").toExecutableDSL());
		assertEquals("test-AAA 'failed'->test-BBB '*'->test-CCC",
				parse("AAA 'failed' -> BBB '*' -> CCC").toExecutableDSL());
		assertEquals("test-AAA 1->test-BBB 2->test-CCC", parse("AAA 1 -> BBB 2 -> CCC").toExecutableDSL());
	}

	@Test
	public void isComposedTask() {
		ctn = parse("appA 'foo' -> appB");
		assertTrue(ctn.isComposed());
		assertNull(ctn.getTaskApp());
		assertGraph("[0:START][1:appA][2:appB][3:END][0-1][foo:1-2][1-3][2-3]", "appA 'foo' -> appB");
		ctn = parse("appA");
		assertFalse(ctn.isComposed());
		assertNotNull(ctn.getTaskApp());
	}

	@Test
	public void basics() {
		Tokens tokens = new TaskTokenizer().getTokens("App1");
		assertToken(TokenKind.IDENTIFIER, "App1", 0, 4, tokens.next());
		tokens = new TaskTokenizer().getTokens("App1 && App2");
		assertToken(TokenKind.IDENTIFIER, "App1", 0, 4, tokens.next());
		assertToken(TokenKind.ANDAND, "&&", 5, 7, tokens.next());
		assertToken(TokenKind.IDENTIFIER, "App2", 8, 12, tokens.next());
		tokens = new TaskTokenizer().getTokens("< > -> ( )");
		assertToken(TokenKind.LT, "<", 0, 1, tokens.next());
		assertToken(TokenKind.GT, ">", 2, 3, tokens.next());
		assertToken(TokenKind.ARROW, "->", 4, 6, tokens.next());
		assertToken(TokenKind.OPEN_PAREN, "(", 7, 8, tokens.next());
		assertToken(TokenKind.CLOSE_PAREN, ")", 9, 10, tokens.next());
	}

	@Test
	public void tokenStreams() {
		Tokens tokens = new TaskTokenizer().getTokens("App1 0->App2 1->:Bar");
		assertTokens(tokens, TokenKind.IDENTIFIER, TokenKind.IDENTIFIER,
				TokenKind.ARROW, TokenKind.IDENTIFIER, TokenKind.IDENTIFIER,
				TokenKind.ARROW, TokenKind.COLON, TokenKind.IDENTIFIER);
		tokens = new TaskTokenizer().getTokens("App1 0->App2 'abc' ->   App3");
		assertTokens(tokens, TokenKind.IDENTIFIER, TokenKind.IDENTIFIER,
				TokenKind.ARROW, TokenKind.IDENTIFIER, TokenKind.LITERAL_STRING,
				TokenKind.ARROW, TokenKind.IDENTIFIER);
	}

	@Test
	public void singleApp() {
		ctn = parse("FooApp");
		assertEquals("FooApp", ctn.getTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(6, ctn.getEndPos());
		assertEquals("FooApp", ctn.stringify());
		LabelledTaskNode node = ctn.getStart();
		assertFalse(node.isSplit());
		assertTrue(node.isFlow());
		assertFlow(node, "FooApp");
		assertTrue(node.getSeriesElement(0).isTaskApp());
	}

	@Test
	public void twoAppFlow() {
		ctn = parse("FooApp  &&  BarApp");

		assertEquals("FooApp  &&  BarApp", ctn.getTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(18, ctn.getEndPos());
		assertEquals("FooApp && BarApp", ctn.stringify());

		LabelledTaskNode node = ctn.getStart();
		assertFalse(node.isSplit());
		assertTrue(node.isFlow());
		assertFalse(node.isTaskApp());

		FlowNode flow = (FlowNode) node;
		List<LabelledTaskNode> series = flow.getSeries();
		assertEquals(2, series.size());
		assertEquals(2, flow.getSeriesLength());
		assertTaskApp(series.get(0), "FooApp");
		assertTaskApp(flow.getSeriesElement(0), "FooApp");
		assertTaskApp(series.get(1), "BarApp");
		assertTaskApp(flow.getSeriesElement(1), "BarApp");
	}

	@Test
	public void appsInTaskDef() {
		ctn = parse("FooApp --p1=v1 --p2=v2");
		ctn = parse("FooApp --p1=v1 --p2=v2 && BarApp --p3=v3");
		ctn = parse("<FooApp || BarApp --p1=v1>");
		ctn = parse("FooApp --p1=v1 'something' -> GooApp --p2=v2 && SooApp --p3=v3");

		String spec = "FooApp --p1=v1";
		assertGraph("[0:START][1:FooApp:p1=v1][2:END][0-1][1-2]", spec);
		spec = "FooApp --p1=v1 --p2=v2";
		assertGraph("[0:START][1:FooApp:p1=v1:p2=v2][2:END][0-1][1-2]", spec);
		spec = "FooApp --p1=v1 && BarApp --p2=v2";
		assertGraph("[0:START][1:FooApp:p1=v1][2:BarApp:p2=v2][3:END][0-1][1-2][2-3]", spec);
		spec = "<FooApp || BarApp --p1=v1>";
		assertGraph("[0:START][1:FooApp][2:BarApp:p1=v1][3:END][0-1][0-2][1-3][2-3]", spec);
		spec = "FooApp --p1=v1 'something' -> GooApp --p2=v2 && SooApp --p3=v3";
		assertGraph("[0:START][1:FooApp:p1=v1][2:GooApp:p2=v2][3:SooApp:p3=v3][4:END][0-1][something:1-2][1-3][3-4][2"
				+ "-4]", spec);
	}

	@Test
	public void oneAppSplit() {
		ctn = parse("< FooApp>");

		assertEquals("< FooApp>", ctn.getTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(9, ctn.getEndPos());
		assertEquals("<FooApp>", ctn.stringify());

		LabelledTaskNode node = ctn.getStart();
		assertTrue(node.isFlow());
		node = node.getSeriesElement(0);
		assertTrue(node.isSplit());
		assertFalse(node.isTaskApp());

		SplitNode split = (SplitNode) node;
		List<LabelledTaskNode> series = split.getSeries();
		assertEquals(1, series.size());
		assertEquals(1, split.getSeriesLength());
		assertFlow(series.get(0), "FooApp");
		assertFlow(split.getSeriesElement(0), "FooApp");
	}

	@Test
	public void twoAppSplit() {
		ctn = parse("< FooApp  ||    BarApp>");

		assertEquals("< FooApp  ||    BarApp>", ctn.getTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(23, ctn.getEndPos());
		assertEquals("<FooApp || BarApp>", ctn.stringify());

		LabelledTaskNode node = ctn.getStart();
		assertTrue(node.isFlow());
		node = node.getSeriesElement(0);
		assertTrue(node.isSplit());
		assertFalse(node.isTaskApp());

		SplitNode split = (SplitNode) node;
		List<LabelledTaskNode> series = split.getSeries();
		assertEquals(2, series.size());
		assertEquals(2, split.getSeriesLength());
		assertFlow(series.get(0), "FooApp");
		assertFlow(split.getSeriesElement(0), "FooApp");
		assertFlow(series.get(1), "BarApp");
		assertFlow(split.getSeriesElement(1), "BarApp");
	}

	@Test
	public void appWithOneTransition() {
		ctn = parse("App1 0->App2");
		assertEquals("test", ctn.getName());
		assertEquals("App1 0->App2", ctn.getTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(12, ctn.getEndPos());
		assertEquals("App1 0->App2", ctn.stringify());
		LabelledTaskNode firstNode = ctn.getStart();
		assertTrue(firstNode.isFlow());
		List<TransitionNode> transitions = ((TaskAppNode) firstNode.getSeriesElement(0)).getTransitions();
		assertEquals(1, transitions.size());
		TransitionNode transition = transitions.get(0);
		assertEquals("0", transition.getStatusToCheck());
		assertEquals("App2", transition.getTargetDslText());
		assertEquals(5, transition.getStartPos());
		assertEquals(12, transition.getEndPos());
	}

	@Test
	public void appWithTwoTransitions() {
		ctn = parse("App1 0->App2 'abc' ->   App3");
		assertEquals("App1 0->App2 'abc' ->   App3", ctn.getTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(28, ctn.getEndPos());
		assertEquals("App1 0->App2 'abc'->App3", ctn.stringify());
		LabelledTaskNode node = ctn.getStart();
		assertTrue(node.isFlow());
		node = node.getSeriesElement(0);
		List<TransitionNode> transitions = ((TaskAppNode) node).getTransitions();
		assertEquals(2, transitions.size());
		TransitionNode transition = transitions.get(0);
		assertEquals("0", transition.getStatusToCheck());
		assertTrue(transition.isExitCodeCheck());
		assertEquals("App2", transition.getTargetDslText());
		assertEquals(5, transition.getStartPos());
		assertEquals(12, transition.getEndPos());
		transition = transitions.get(1);
		assertEquals("abc", transition.getStatusToCheck());
		assertFalse(transition.isExitCodeCheck());
		assertEquals("App3", transition.getTargetDslText());
		assertEquals(13, transition.getStartPos());
		assertEquals(28, transition.getEndPos());
	}

	@Test
	public void appWithWildcardTransitions() {
		ctn = parse("App1 *->App2 '*'->App3");
		assertEquals("App1 *->App2 '*'->App3", ctn.getTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(22, ctn.getEndPos());
		assertEquals("App1 *->App2 '*'->App3", ctn.stringify());
		LabelledTaskNode node = ctn.getStart();
		node = node.getSeriesElement(0);
		assertTrue(node.isTaskApp());
		List<TransitionNode> transitions = ((TaskAppNode) node).getTransitions();
		assertEquals(2, transitions.size());

		TransitionNode transition = transitions.get(0);
		assertEquals("*", transition.getStatusToCheck());
		assertTrue(transition.isExitCodeCheck());
		assertEquals("App2", transition.getTargetDslText());
		assertEquals(5, transition.getStartPos());
		assertEquals(12, transition.getEndPos());
		transition = transitions.get(1);
		assertEquals("*", transition.getStatusToCheck());
		assertFalse(transition.isExitCodeCheck());
		assertEquals("App3", transition.getTargetDslText());
		assertEquals(13, transition.getStartPos());
		assertEquals(22, transition.getEndPos());
	}

	@Test
	public void appWithLabelReferenceTransition() {
		ctn = parse("App1 'foo'->:something", false);
		assertEquals("App1 'foo'->:something", ctn.getTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(22, ctn.getEndPos());
		assertEquals("App1 'foo'->:something", ctn.stringify());
		LabelledTaskNode firstNode = ctn.getStart();
		assertFlow(firstNode, "App1");
		List<TransitionNode> transitions = ((TaskAppNode) firstNode.getSeriesElement(0)).getTransitions();
		assertEquals(1, transitions.size());
		TransitionNode transition = transitions.get(0);
		assertEquals("foo", transition.getStatusToCheck());
		assertFalse(transition.isExitCodeCheck());
		assertEquals(":something", transition.getTargetDslText());
		assertEquals("something", transition.getTargetLabel());
		assertEquals(5, transition.getStartPos());
		assertEquals(22, transition.getEndPos());
	}

	@Test
	public void splitMainComposedTaskOverMultipleLines() {
		ctn = parse("FooApp &&\nBarApp");
		assertFlow(ctn.getStart(), "FooApp", "BarApp");
		ctn = parse("FooApp\n&& BarApp");
		assertFlow(ctn.getStart(), "FooApp", "BarApp");
		ctn = parse("FooApp\n&&\nBarApp");
		assertFlow(ctn.getStart(), "FooApp", "BarApp");
		ctn = parse("FooApp 0->:a 1->:b &&\nBarApp 2->:c 3->:d", false);
		assertFlow(ctn.getStart(), "FooApp", "BarApp");
		ctn = parse("FooApp\n 0\n->:a\n 1->:b\n &&\nBarApp 2->:c 3->:d", false);
		assertFlow(ctn.getStart(), "FooApp", "BarApp");
		ctn = parse("<FooApp ||\nBarApp>");
		assertSplit(ctn.getStart().getSeriesElement(0), "FooApp", "BarApp");
		ctn = parse("<\nFooApp ||\nBarApp\n>");
		assertSplit(ctn.getStart().getSeriesElement(0), "FooApp", "BarApp");
	}

	@Test
	public void labelledElement() {
		ctn = parse("foo: appA");
		LabelledTaskNode start = ctn.getStart();
		assertEquals("foo", start.getLabelString());
		FlowNode f = (FlowNode) start;
		assertEquals("foo", f.getLabelString());
		assertEquals("appA", ((TaskAppNode) f.getSeriesElement(0)).getName());

		ctn = parse("foo: <appA || appB>");
		start = ctn.getStart();
		assertEquals("foo", start.getLabelString());
		SplitNode s = (SplitNode) start.getSeriesElement(0);
		assertSplit(s, "appA", "appB");

		ctn = parse("foo: appA && appB");
		start = ctn.getStart();
		assertEquals("foo", start.getLabelString());
		assertFlow(start, "appA", "appB");
	}

	@Test
	public void taskCollectorVisitor() {
		assertApps(parse("appA").getTaskApps(), "appA");
		assertApps(parse("appA && appB && appC").getTaskApps(), "appA", "appB", "appC");
		assertApps(parse("<appA || appB> && appC").getTaskApps(), "appA", "appB", "appC");
		// assertApps(parse("<appA || appB> && appC &&
		// appC").getTaskApps(),"appA","appB","appC");
		assertApps(parse("<appA || appB> && appC && boo: appC").getTaskApps(), "appA", "appB", "appC", "boo:appC");
	}

	@Test
	public void transitionToOtherSequence() {
		String spec = " appA 'fail'->:two && appB && appC;two: appD && appE";
		assertGraph("[0:START][1:appA][2:appB][3:appC][4:END][9:appD][10:appE]"
				+ "[0-1][1-2][2-3][3-4][fail:1-9][9-10][10-4]", spec);
	}

	@Test
	public void singleSplitToGraph() {
		String spec = "<appA 'fail'-> appB>";
		assertGraph("[0:START][1:appA][2:appB][3:END]"
				+ "[0-1][fail:1-2][1-3][2-3]", spec);
	}

	@Test
	public void secondarySequencesHaveFurtherTransitions() {
		String spec = " appA 'fail'->:two && appB;two: appD 'fail2'->:three && appE;three: appF && appG";
		assertGraph("[0:START][1:appA][2:appB][3:END][12:appD][13:appE][14:appF][15:appG]"
				+ "[0-1][1-2][2-3][fail:1-12][12-13][13-3][fail2:12-14][14-15][15-3]", spec);
	}

	@Test
	public void twoReferencesToSecondarySequence() {
		String spec = "appA 'fail'->:two && appB 'fail2'->:two && appC;two: appD && appE";
		assertGraph("[0:START][1:appA][2:appB][3:appC][4:END][9:appD][10:appE]"
				+ "[0-1][1-2][2-3][3-4][fail:1-9][fail2:2-9][9-10][10-4]", spec);
	}

	@Disabled
	@Test
	public void transitionToSplit() {
		String spec = "aa 'foo'->:split && bb && split: <cc || dd> && ee";
		// lets consider this a limitation for now.
		assertGraph("[0:START][1:aa][2:bb][3:cc][4:dd][5:ee][6:END]" + "[0-1][1-2]['foo':1-3][2-3][2-4][3-5][4-5][5-6]",
				spec);
	}

	@Test
	public void transitionToNonResolvedLabel() {
		String spec = "aa 'foo'->:split && bb && cc";
		TaskNode ctn = parse(spec, false);
		List<TaskValidationProblem> validationProblems = ctn.validate();
		assertEquals(1, validationProblems.size());
		assertEquals(DSLMessage.TASK_VALIDATION_TRANSITION_TARGET_LABEL_UNDEFINED,
				validationProblems.get(0).getMessage());
		assertEquals(3, validationProblems.get(0).getOffset());

		spec = "<aa 'foo'->:split && bb && cc || dd>";
		ctn = parse(spec, false);
		validationProblems = ctn.validate();
		assertEquals(1, validationProblems.size());
		assertEquals(DSLMessage.TASK_VALIDATION_TRANSITION_TARGET_LABEL_UNDEFINED,
				validationProblems.get(0).getMessage());
		assertEquals(4, validationProblems.get(0).getOffset());
	}

	@Test
	public void visitors() {
		ctn = parse("appA");
		TestVisitor tv = new TestVisitor();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >TA =TA[appA] <TA <F <SN[0]", tv.getString());

		ctn = parse("foo: appA");
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[foo: 0] >F =F[foo:] >TA =TA[foo: appA] <TA <F <SN[0]", tv.getString());

		ctn = parse("appA && appB");
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >TA =TA[appA] <TA >TA =TA[appB] <TA <F <SN[0]", tv.getString());

		ctn = parse("<appA || appB>");
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >S =S >F =F >TA =TA[appA] <TA <F >F =F >TA =TA[appB] <TA <F <S <F <SN[0]",
				tv.getString());

		ctn = parse("<appA && appB|| appC>");
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >S =S >F =F >TA =TA[appA] <TA >TA =TA[appB] <TA <F >F =F >TA =TA[appC] <TA <F <S "
				+ "<F <SN[0]", tv.getString());

		ctn = parse("appA 0->:foo", false);
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >TA =TA[appA] >T =T[0->:foo] <T <TA <F <SN[0]", tv.getString());

		ctn = parse("appA 0->appB");
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >TA =TA[appA] >T =T[0->appB] <T <TA <F <SN[0]", tv.getString());

		ctn = parse("appA;appB", false);
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >TA =TA[appA] <TA <F <SN[0] >SN[1] >F =F >TA =TA[appB] <TA <F <SN[1]",
				tv.getString());

		ctn = parse("appA && appB 0->:foo *->appC;foo: appD && appE", false);
		assertApps(ctn.getTaskApps(), "appA", "appB", "appC", "foo:appD", "appE");
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >TA =TA[appA] <TA >TA =TA[appB] >T =T[0->:foo] <T >T =T[*->appC] <T <TA <F <SN[0] "
				+ ">SN[foo: 1] >F =F[foo:] >TA =TA[foo: appD] <TA >TA =TA[appE] <TA <F <SN[1]", tv.getString());
	}

	@Test
	public void multiline() {
		ctn = parse("appA 0->:label1 && appB\nlabel1: appC");
	}

	@Test
	public void multiSequence() {
		TaskNode ctn = parse("appA\n  0->:foo\n  *->appB\n  && appE;foo: appC && appD");
		LabelledTaskNode start = ctn.getStart(); // get the root of the AST starting appA
		assertNotNull(start);
		List<LabelledTaskNode> sequences = ctn.getSequences();
		LabelledTaskNode labelledTaskNode = sequences.get(1);
		assertEquals("foo", labelledTaskNode.getLabelString());
		LabelledTaskNode fooSequence = ctn.getSequenceWithLabel("foo"); // get the AST for foo: ...
		assertNotNull(fooSequence);
		TestVisitor tv = new TestVisitor();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >TA =TA[appA] >T =T[0->:foo] <T >T =T[*->appB] <T <TA >TA =TA[appE] <TA <F <SN[0] "
				+ ">SN[foo: 1] >F =F[foo:] >TA =TA[foo: appC] <TA >TA =TA[appD] <TA <F <SN[1]", tv.getString());
	}

	@Test
	public void validator() {
		TaskValidatorVisitor validator = new TaskValidatorVisitor();
		ctn = parse("appA");
		ctn.accept(validator);
		assertFalse(validator.hasProblems());

		validator.reset();
		ctn = parse("appA;appB", false);
		ctn.accept(validator);
		List<TaskValidationProblem> problems = validator.getProblems();
		assertEquals(1, problems.size());
		assertEquals(DSLMessage.TASK_VALIDATION_SECONDARY_SEQUENCES_MUST_BE_NAMED, problems.get(0).getMessage());
		assertEquals(5, problems.get(0).getOffset());
		assertEquals("158E:(pos 5): secondary sequences must have labels or are unreachable",
				problems.get(0).toString());
		assertEquals("158E:(pos 5): secondary sequences must have labels or are unreachable\nappA;appB\n     ^\n",
				problems.get(0).toStringWithContext());

		validator.reset();
		ctn = parse("appA;foo: appB");
		ctn.accept(validator);
		assertFalse(validator.hasProblems());

		validator.reset();
		ctn = parse("appA;foo: appB\nappC", false);
		ctn.accept(validator);
		problems = validator.getProblems();
		assertEquals(1, problems.size());
		assertEquals(DSLMessage.TASK_VALIDATION_SECONDARY_SEQUENCES_MUST_BE_NAMED, problems.get(0).getMessage());
		assertEquals(15, problems.get(0).getOffset());
		assertEquals("158E:(pos 15): secondary sequences must have labels or are unreachable",
				problems.get(0).toString());
		assertEquals("158E:(pos 15): secondary sequences must have labels or are unreachable\nappC\n^\n",
				problems.get(0).toStringWithContext());

		validator.reset();
		ctn = parse("appA && appA", false);
		ctn.accept(validator);
		problems = validator.getProblems();
		assertEquals(1, problems.size());
		assertEquals(DSLMessage.TASK_VALIDATION_APP_NAME_ALREADY_IN_USE, problems.get(0).getMessage());
		assertEquals(8, problems.get(0).getOffset());
		validator.reset();
		ctn = parse("appA 'foo' -> appA", false);
		ctn.accept(validator);
		problems = validator.getProblems();
		assertEquals(1, problems.size());
		assertEquals(DSLMessage.TASK_VALIDATION_APP_NAME_ALREADY_IN_USE, problems.get(0).getMessage());
		assertEquals(14, problems.get(0).getOffset());
		validator.reset();
		ctn = parse("appA 'foo' -> appA: appB", false);
		ctn.accept(validator);
		problems = validator.getProblems();
		assertEquals(1, problems.size());
		assertEquals(DSLMessage.TASK_VALIDATION_LABEL_CLASHES_WITH_TASKAPP_NAME, problems.get(0).getMessage());
		assertEquals(14, problems.get(0).getOffset());
		validator.reset();
		ctn = parse("label1: appA 'foo' -> label1: appB", false);
		ctn.accept(validator);
		problems = validator.getProblems();
		assertEquals(1, problems.size());
		assertEquals(DSLMessage.TASK_VALIDATION_DUPLICATE_LABEL, problems.get(0).getMessage());
		assertEquals(22, problems.get(0).getOffset());
		validator.reset();
		ctn = parse("label1: appA 'foo' -> label1", false);
		ctn.accept(validator);
		problems = validator.getProblems();
		assertEquals(1, problems.size());
		assertEquals(DSLMessage.TASK_VALIDATION_APP_NAME_CLASHES_WITH_LABEL, problems.get(0).getMessage());
		assertEquals(22, problems.get(0).getOffset());
	}

	@Test
	public void labels() {
		// basic task
		ctn = parse("aaa: appA");
		LabelledTaskNode flow = ctn.getStart();
		assertEquals("aaa", flow.getLabelString());
		TaskAppNode taskApp = (TaskAppNode) flow.getSeriesElement(0);
		assertEquals("aaa", taskApp.getLabelString());

		// flows
		ctn = parse("aaa: appA && bbb: appB");
		taskApp = (TaskAppNode) ctn.getStart().getSeriesElement(1);
		assertEquals("bbb", taskApp.getLabelString());

		// splits
		ctn = parse("outer:<aaa: appA || bbb: appB>");
		flow = ctn.getStart();
		assertEquals("outer", flow.getLabelString());
		SplitNode s = (SplitNode) flow.getSeriesElement(0);
		assertEquals("outer", s.getLabelString());
		taskApp = (TaskAppNode) (s.getSeriesElement(0).getSeriesElement(0));
		assertEquals("aaa", taskApp.getLabelString());
		taskApp = (TaskAppNode) (s.getSeriesElement(1).getSeriesElement(0));
		assertEquals("bbb", taskApp.getLabelString());

		// parentheses
		ctn = parse("(aaa: appA && appB)");
		taskApp = (TaskAppNode) ctn.getStart().getSeriesElement(0);
		assertEquals("aaa", taskApp.getLabelString());

		checkForParseError("aaa: (appA)", DSLMessage.TASK_NO_LABELS_ON_PARENS, 5);
		checkForParseError("aaa: bbb: appA", DSLMessage.NO_DOUBLE_LABELS, 5);
		checkForParseError("aaa: >", DSLMessage.EXPECTED_APPNAME, 5);
		checkForParseError("aaa: &&", DSLMessage.EXPECTED_APPNAME, 5);
		checkForParseError("aaa:: appA", DSLMessage.EXPECTED_APPNAME, 4);
	}

	@Test
	public void badTransitions() {
		checkForParseError("App1 ->", DSLMessage.TASK_ARROW_SHOULD_BE_PRECEDED_BY_CODE, 5);
		checkForParseError("App1 0->x ->", DSLMessage.TASK_ARROW_SHOULD_BE_PRECEDED_BY_CODE, 10);
		checkForParseError("App1 ->xx", DSLMessage.TASK_ARROW_SHOULD_BE_PRECEDED_BY_CODE, 5);
		checkForParseError("App1 xx->", DSLMessage.OOD, 9);
	}

	@Test
	public void graphToText_1712() {
		assertGraph("[0:START][1:timestamp][2:END][0-1][1-2]", "timestamp");
		// In issue 1712 the addition of an empty properties map to the link damages the
		// generation of the DSL. It was expecting null if there are no properties.
		TaskNode ctn = parse("timestamp");
		Graph graph = ctn.toGraph();
		// Setting these to empty maps mirrors what the UI is doing in SCDF 1.3.0.M3
		graph.nodes.get(0).metadata = new HashMap<>();
		graph.nodes.get(1).metadata = new HashMap<>();
		graph.nodes.get(2).metadata = new HashMap<>();
		graph.links.get(0).properties = new HashMap<>();
		graph.links.get(1).properties = new HashMap<>();
		assertEquals("timestamp", graph.toDSLText());
	}
	
	@Test
	public void graphToText_3667() {
		assertGraph("[0:START][1:sql-executor-task:password=password:url=jdbc:postgresql://127.0.0.1:5432/postgres:script-location=/dataflow/scripts/test.sql:username=postgres]"+
					"[2:END][0-1][1-2]","sql-executor-task --script-location=/dataflow/scripts/test.sql --username=postgres --password=password --url=jdbc:postgresql://127.0.0.1:5432/postgres");
		
		assertGraph("[0:START][1:t1:timestamp][2:t2:timestamp][3:t3:timestamp][4:END][0-1][FAILED:1-2][1-3][3-4][2-4]",
					"t1: timestamp 'FAILED'->t2: timestamp && t3: timestamp");

		TaskNode ctn = parse("t1: timestamp 'FAILED'->t2: timestamp && t3: timestamp");
		Graph graph = ctn.toGraph();
		assertEquals("t1: timestamp 'FAILED'->t2: timestamp && t3: timestamp", graph.toDSLText());
		
		ctn = parse("t1: timestamp --format=aabbcc 'FAILED'->t2: timestamp && t3: timestamp --format=gghhii");
		graph = ctn.toGraph();
		assertEquals("t1: timestamp --format=aabbcc 'FAILED'->t2: timestamp && t3: timestamp --format=gghhii", graph.toDSLText());

		ctn = parse("t1: timestamp --format=aabbcc 'FAILED'->t2: timestamp --format=ddeeff && t3: timestamp --format=gghhii");
		graph = ctn.toGraph();
		Node node = graph.nodes.get(2);
		assertEquals("ddeeff",node.properties.get("format"));
		assertEquals("t1: timestamp --format=aabbcc 'FAILED'->t2: timestamp --format=ddeeff && t3: timestamp --format=gghhii", graph.toDSLText());
		
		assertGraph("[0:START][1:eee:timestamp:format=ttt][2:QQQQQ:timestamp:format=NOT-IN-TEXT][3:ooo:timestamp:format=yyyy][4:END][0-1][FAILED:1-2][1-3][3-4][2-4]",
				    "eee: timestamp --format=ttt 'FAILED'->QQQQQ: timestamp --format=NOT-IN-TEXT && ooo: timestamp --format=yyyy");
	}
	
	@Test
	public void graphToTextSingleAppInSplit() {
		// Note the graph here does not include anything special
		// to preserve the split because the split is unnecessary
		// and is removed when the text is recomputed for it.
		assertGraph("[0:START][1:AppA][2:END][0-1][1-2]","<AppA>");
		TaskNode ctn = parse("<AppA>");
		Graph graph = ctn.toGraph();
		assertEquals("AppA", graph.toDSLText());
		
		assertGraph("[0:START][1:AppA][2:AppB][3:END][0-1][1-2][2-3]","<AppA> && AppB");
		ctn = parse("<AppA> && AppB");
		graph = ctn.toGraph();
		assertEquals("AppA && AppB", graph.toDSLText());
		
		assertGraph("[0:START][1:AppA][2:AppC][3:AppB][4:END][0-1][99:1-2][1-3][2-3][3-4]","<AppA 99 -> AppC> && AppB");
		ctn = parse("<AppA 99->AppC> && AppB");
		graph = ctn.toGraph();
		assertEquals("<AppA 99->AppC> && AppB", graph.toDSLText());

		// Check it still does the right thing when the split does have multple:
		ctn = parse("<AppA 99->AppC || AppD> && AppB");
		graph = ctn.toGraph();
		assertEquals("<AppA 99->AppC || AppD> && AppB", graph.toDSLText());
		
		// This is the test specifically for issue 3263
		ctn = parse("<Import: timestamp 'Error2'->T2: timestamp 'Error'->T1: timestamp> && Backwards: timestamp");
		// Key thing to note from here is that the links from  the transition nodes connect 
		// Import to Backwards and don't go straight to END
		assertGraph("[0:START][1:Import:timestamp][2:T2:timestamp][3:T1:timestamp][4:Backwards:timestamp][5:END][0-1][Error2:1-2][Error:1-3][1-4][2-4][3-4][4-5]",
			"<Import: timestamp 'Error2'->T2: timestamp 'Error'->T1: timestamp> && Backwards: timestamp");
		graph = ctn.toGraph();
		assertEquals("<Import: timestamp 'Error2'->T2: timestamp 'Error'->T1: timestamp> && Backwards: timestamp", graph.toDSLText());
		
		// This is the variant of the above without the <...>
		// Now notice the links from the transition nodes go direct to END
		ctn = parse("Import: timestamp 'Error2'->T2: timestamp 'Error'->T1: timestamp && Backwards: timestamp");
		assertGraph("[0:START][1:Import:timestamp][2:T2:timestamp][3:T1:timestamp][4:Backwards:timestamp][5:END][0-1][Error2:1-2][Error:1-3][1-4][4-5][2-5][3-5]",
			"Import: timestamp 'Error2'->T2: timestamp 'Error'->T1: timestamp && Backwards: timestamp");
		graph = ctn.toGraph();
		assertEquals("Import: timestamp 'Error2'->T2: timestamp 'Error'->T1: timestamp && Backwards: timestamp", graph.toDSLText());
	}

	@Test
	public void graphToText() {
		assertGraph("[0:START][1:AppA][2:END][0-1][1-2]", "AppA");
		checkDSLToGraphAndBackToDSL("AppA");
		assertGraph("[0:START][1:AppA][2:AppB][3:END][0-1][1-2][2-3]", "AppA && AppB");
		checkDSLToGraphAndBackToDSL("AppA && AppB");
		assertGraph("[0:START][1:AppA][2:AppB][3:END][0-1][0-2][1-3][2-3]", "<AppA || AppB>");
		checkDSLToGraphAndBackToDSL("<AppA || AppB>");
		assertGraph("[0:START][1:AppA][2:AppB][3:AppC][4:END][0-1][1-2][0-3][2-4][3-4]", "<AppA && AppB || AppC>");
		checkDSLToGraphAndBackToDSL("<AppA && AppB || AppC>");
		assertGraph("[0:START][1:AppA][2:AppB][3:AppC][4:END][0-1][0-2][2-3][1-4][3-4]", "<AppA || AppB && AppC>");
		checkDSLToGraphAndBackToDSL("<AppA || AppB && AppC>");
		assertGraph("[0:START][1:AppA][2:AppB][3:AppC][4:AppD][5:END][0-1][1-2][0-3][3-4][2-5][4-5]",
				"<AppA && AppB " + "|| AppC && AppD>");
		checkDSLToGraphAndBackToDSL("<AppA && AppB || foo: AppB && AppC>");
		assertGraph("[0:START][1:AppA][2:AppB][3:AppC][4:END][0-1][1-2][1-3][2-4][3-4]", "AppA && <AppB || AppC>");
		checkDSLToGraphAndBackToDSL("AppA && <AppB || AppC>");
		assertGraph("[0:START][1:AppA][2:AppB][3:AppC][4:AppD][5:END][0-1][1-2][1-3][2-4][3-4][4-5]",
				"AppA && <AppB || AppC> && AppD");
		checkDSLToGraphAndBackToDSL("AppA && <AppB || AppC> && AppD");
		assertGraph(
				"[0:START][1:AppA][2:AppB][3:SYNC][4:AppC][5:AppD][6:END][0-1][0-2][1-3][2-3][3-4][3-5][4-6][5-6" + "]",
				"<AppA || AppB> && <AppC || AppD>");
		checkDSLToGraphAndBackToDSL("<AppA || AppB> && <AppC || AppD>");
		assertGraph("[0:START][1:AppA][2:AppB][3:AppC][4:SYNC][5:AppD][6:AppE][7:AppF][8:END][0-1][1-2][0-3][2-4][3-4"
				+ "][4-5][4-6][6-7][5-8][7-8]", "<AppA && AppB || AppC> && <AppD || AppE && AppF>");
		checkDSLToGraphAndBackToDSL("<AppA && AppB || AppC> && <AppD || AppE && AppF>");
		checkDSLToGraphAndBackToDSL("<AppA && AppB || AppC> && <AppD || AppE && AppF>");
		checkDSLToGraphAndBackToDSL("<foojob || bbb && ccc>");
		checkDSLToGraphAndBackToDSL("<a || b> && c");
		checkDSLToGraphAndBackToDSL("a && <b || c>");
		// Test that even though two transitions specify the same app and are in the same
		// flow, the
		// targets are different because the 'names' (i.e. labels) make them different.
		assertGraph("[0:START][1:AppA][2:AppB][3:x:AppC][4:AppD][5:y:AppC][6:END][0-1][1-2][0:2-3][2-4][0:4-5][4-6][3"
				+ "-6][5-6]", "AppA && AppB 0->x: AppC && AppD 0->y: AppC");
	}

	@Test
	public void textToGraphWithTransitions() {
		assertGraph("[0:START][1:AppA][2:AppE][3:AppB][4:END][0-1][0:1-2][1-3][3-4][2-4]", "AppA 0->AppE && AppB");
		checkDSLToGraphAndBackToDSL("AppA 0->AppE && AppB");
		assertGraph("[0:START][1:AppA][2:AppE][3:AppB][4:AppC][5:END][0-1][0:1-2][1-3][3-4][4-5][2-5]",
				"AppA 0->AppE" + " && AppB && AppC");
		checkDSLToGraphAndBackToDSL("AppA 0->AppE && AppB && AppC");
		assertGraph("[0:START][1:AppA][2:AppE][3:AppB][4:AppC][5:AppD][6:END][0-1][0:1-2][1-3][3-4][3-5][4-6][5-6][2"
				+ "-6]", "AppA 0->AppE && AppB && <AppC || AppD>");
		checkDSLToGraphAndBackToDSL("AppA 0->AppE && AppB && <AppC || AppD>");
		checkDSLToGraphAndBackToDSL("aaa 'FOO'->XXX 'B'->bbb1 '*'->ccc1 && bbb2 && ccc2");
		assertGraph("[0:START][1:x:AppA][2:y:AppB][3:END][0-1][0:1-2][1-3][2-3]", "x: AppA 0->y: AppB");
	}

	@Test
	public void graphToTextSplitWithTransition() {
		checkDSLToGraphAndBackToDSL("<Foo 'failed'->Kill || Bar>");
		checkDSLToGraphAndBackToDSL("<AppA 'failed'->Kill || AppB> && AppC");
	}

	@Test
	public void toDSLTextNestedSplits() {
		checkDSLToGraphAndBackToDSL("<aaa || ccc || ddd> && eee");
		checkDSLToGraphAndBackToDSL("<aaa || bbb && <ccc || ddd>> && eee");
		checkDSLToGraphAndBackToDSL("<aaa && <bbb || ccc> && foo || ddd && eee> && fff");
		checkDSLToGraphAndBackToDSL("<aaa && <bbb || ccc> || ddd && eee> && fff");
		checkDSLToGraphAndBackToDSL("<aaa && <bbb || ccc> || ddd && eee> && fff");
		checkDSLToGraphAndBackToDSL("<aaa || bbb && <ccc || ddd>> && <eee || fff>");
		checkDSLToGraphAndBackToDSL("<aaa || bbb && <ccc || ddd>> && <eee || fff> && <ggg || hhh>");
	}

	@Test
	public void errorExpectDoubleOr() {
		checkForParseError("<aa | bb>", DSLMessage.TASK_DOUBLE_OR_REQUIRED, 4);
		checkForParseError("<aa ||| bb>", DSLMessage.TASK_DOUBLE_OR_REQUIRED, 6);
	}

	@Test
	public void modeError() {
		try {
			new TaskParser("foo", "appA --p1=v1", false, true).parse();
			fail();
		}
		catch (CheckPointedParseException cppe) {
			assertEquals(DSLMessage.TASK_ARGUMENTS_NOT_ALLOWED_UNLESS_IN_APP_MODE, cppe.message);
		}
		try {
			new TaskParser("foo", "appA --p1=v1", true, true).parse();
		}
		catch (CheckPointedParseException cppe) {
			fail();
		}
	}

	@Test
	public void unexpectedDoubleAnd() {
		checkForParseError("aa  &&&& bb", DSLMessage.EXPECTED_APPNAME, 6, "&&");
	}

	@Test
	public void toDSLTextTransitions() {
		// [SHOULD-VALIDATE] There is no real route to bbb
		String spec = "aaa '*'->$END && bbb";
		assertEquals(spec, parse(spec).toDSL());
		assertGraph("[0:START][1:aaa][2:$END][3:bbb][4:END]" + "[0-1][*:1-2][1-3][3-4]", spec);
		checkDSLToGraphAndBackToDSL(spec);
	}

	@Test
	// You can't draw this on the graph, it would end up looking like "aaa | '*' = $END ||
	// bbb || ccc
	public void toDSLTextTransitionsSplit() {
		checkDSLToGraphAndBackToDSL("aaa '*'->$END && <bbb || ccc>");
	}

	@Test
	public void toDSLTextTransitionsFlow() {
		checkDSLToGraphAndBackToDSL("aaa '*'->$END && bbb && ccc");
	}

	@Test
	public void toDSLTextSplitFlowSplit() {
		checkDSLToGraphAndBackToDSL("<a || b> && foo && <c || d>");
		checkDSLToGraphAndBackToDSL("<a || b> && foo 'wibble'->$END && <c || d>");
		checkDSLToGraphAndBackToDSL("<a || b> && foo 'wibble'->$FAIL && <c || d>");
	}

	@Test
	public void toDSLTextFlowTransitions() {
		checkDSLToGraphAndBackToDSL("aaa 'COMPLETED'->kill1 'FOO'->kill2");
		checkDSLToGraphAndBackToDSL("aaa 'COMPLETED'->kill && bbb && ccc");
		checkDSLToGraphAndBackToDSL("aaa 'COMPLETED'->kill1 && bbb 'COMPLETED'->kill2 && ccc");
		checkDSLToGraphAndBackToDSL("aaa 'COMPLETED'->x: kill 'FOO'->bar && bbb 'COMPLETED'->y: kill && ccc");
	}

	@Test
	public void toDSLTextSplitTransitions() {
		checkDSLToGraphAndBackToDSL("<aaa 'COMPLETED'->kill || bbb> && ccc");
	}

	@Test
	public void toDSLTextLong() {
		checkDSLToGraphAndBackToDSL(
				"<aaa && fff || bbb && ggg && <ccc || ddd>> && eee && hhh && iii && <jjj || kkk && lll>");
	}

	@Test
	public void syncBetweenSplits() {
		String spec = "<a || b> && <c || d>";
		checkDSLToGraphAndBackToDSL(spec);
		assertGraph("[0:START][1:a][2:b][3:SYNC][4:c][5:d][6:END]" + "[0-1][0-2][1-3][2-3][3-4][3-5][4-6][5-6]", spec);
	}

	@Test
	public void toDSLTextManualSync() {
		// Here foo is effectively acting as a SYNC node
		String spec = "<a || b> && foo && <c || d>";
		checkDSLToGraphAndBackToDSL(spec);
	}

	@Test
	public void whitespace() {
		assertEquals("A && B", parse("A&&B").stringify());
		assertEquals("<A || B>", parse("<A||B>").stringify());
		assertEquals("<A && B || C>", parse("<A&&B||C>").stringify());
	}

	@Test
	public void endTransition() {
		String spec = "aaa 'broken'->$END";
		assertGraph("[0:START][1:aaa][2:$END][3:END][0-1][broken:1-2][1-3]", spec);
		checkDSLToGraphAndBackToDSL(spec);
	}

	// TODO not quoted state transition names
	@Test
	public void missingQuotes() {
		checkForParseError("appA BROKEN->$FAIL", DSLMessage.TASK_UNQUOTED_TRANSITION_CHECK_MUST_BE_NUMBER, 5, "BROKEN");
		checkForParseError("appA\n BROKEN->$FAIL", DSLMessage.TASK_UNQUOTED_TRANSITION_CHECK_MUST_BE_NUMBER, 6,
				"BROKEN");
	}

	@Test
	public void parentheses2() {
		TaskNode ctn = parse("<(jobA && jobB && jobC) || boo: jobC>");
		assertEquals("<jobA && jobB && jobC || boo: jobC>", ctn.stringify());
	}

	@Test
	public void funnyJobNames() {
		ctn = parse("a-b-c");
		assertFlow(ctn.getStart(), "a-b-c");
		ctn = parse("a-b-c && d-e-f");
		checkDSLToGraphAndBackToDSL("a-b-c && d-e-f");
		assertGraph("[0:START][1:a-b-c][2:d-e-f][3:END][0-1][1-2][2-3]", "a-b-c && d-e-f");
	}

	@Test
	public void names() {
		ctn = parse("aaaa: foo");
		List<LabelledTaskNode> sequences = ctn.getSequences();
		assertEquals("aaaa", sequences.get(0).getLabelString());
		ctn = parse("aaaa: foo && bar");
		sequences = ctn.getSequences();
		assertEquals("aaaa", sequences.get(0).getLabelString());
	}

	@Test
	public void nestedSplit1() {
		TaskNode ctn = parse("<<jobA || jobB> || jobC>");
		assertEquals("<<jobA || jobB> || jobC>", ctn.stringify());
		LabelledTaskNode start = ctn.getStart();
		assertInstanceOf(FlowNode.class, start);
		SplitNode split = (SplitNode) start.getSeriesElement(0);
		LabelledTaskNode seriesElement = split.getSeriesElement(0).getSeriesElement(0);
		assertInstanceOf(SplitNode.class, seriesElement);
		SplitNode split2 = (SplitNode) seriesElement;
		assertEquals(2, split2.getSeriesLength());
	}

	@Test
	public void nestedSplit2() {
		TaskNode ctn = parse("<jobA || <jobB || jobC> || jobD>");
		assertEquals("<jobA || <jobB || jobC> || jobD>", ctn.stringify());
		LabelledTaskNode start = ctn.getStart();
		assertTrue(start.isFlow());
		SplitNode split = (SplitNode) start.getSeriesElement(0);
		assertEquals(3, split.getSeriesLength());
		LabelledTaskNode seriesElement = split.getSeriesElement(1);
		SplitNode splitSeriesElement = (SplitNode) seriesElement.getSeriesElement(0);
		assertTrue(splitSeriesElement.isSplit());
		assertEquals(2, splitSeriesElement.getSeriesLength());
		assertEquals("<jobB || jobC>", splitSeriesElement.stringify());
		assertEquals("jobB",
				((TaskAppNode) splitSeriesElement.getSeriesElement(0).getSeriesElement(0)).getName());
	}

	@Test
	public void singleTransition() {
		TaskNode ctn = parse("foo 'completed'->bar");
		LabelledTaskNode start = ctn.getStart();
		start = start.getSeriesElement(0);
		assertInstanceOf(TaskAppNode.class, start);
		TaskAppNode ta = (TaskAppNode) start;
		List<TransitionNode> transitions = ta.getTransitions();
		assertEquals(1, transitions.size());
		assertEquals("completed", transitions.get(0).getStatusToCheck());
		assertEquals("bar", transitions.get(0).getTargetApp().getName());
	}

	@Test
	public void doubleTransition() {
		TaskNode ctn = parse("foo 'completed'->bar 'wibble'->wobble");
		LabelledTaskNode start = ctn.getStart();
		assertFlow(start, "foo");
		TaskAppNode ta = (TaskAppNode) start.getSeriesElement(0);
		List<TransitionNode> transitions = ta.getTransitions();
		assertEquals(2, transitions.size());
		assertEquals("completed", transitions.get(0).getStatusToCheck());
		assertEquals("bar", transitions.get(0).getTargetApp().getName());
		assertEquals("wibble", transitions.get(1).getStatusToCheck());
		assertEquals("wobble", transitions.get(1).getTargetApp().getName());
	}

	@Test
	public void moreSophisticatedScenarios_gh712_1a() {
		TaskNode ctn = parse(
				"<<jdbchdfs-local && spark-client || spark-cluster && two: spark-cluster> && timestamp || spark-yarn>");

		// Check it looks like the picture:
		// https://user-images.githubusercontent.com/1562654/38313990-27662f60-37da-11e8-9106-26688d631fae.png
		LabelledTaskNode start = ctn.getStart();
		FlowNode f1 = (FlowNode) start;
		assertEquals(1, f1.getSeriesLength());
		SplitNode s1 = (SplitNode) f1.getSeriesElement(0);
		assertEquals(2, s1.getSeriesLength());
		// This one is just spark-yarn
		assertFlow(s1.getSeriesElement(1), "spark-yarn");

		// This one is a flow of a split of jdbchdfs-local/spark-client and
		// spark-cluster/spark-cluster and then timestamp
		FlowNode f2 = (FlowNode) s1.getSeriesElement(0);
		assertEquals(2, f2.getSeriesLength());
		assertEquals("timestamp", ((TaskAppNode) f2.getSeriesElement(1)).getName());

		SplitNode s2 = (SplitNode) f2.getSeriesElement(0);
		assertEquals(2, s2.getSeriesLength());
		FlowNode s2fa = (FlowNode) s2.getSeriesElement(0);
		FlowNode s2fb = (FlowNode) s2.getSeriesElement(1);
		assertFlow(s2fa, "jdbchdfs-local", "spark-client");
		assertFlow(s2fb, "spark-cluster", "spark-cluster");

		Graph graph = ctn.toGraph();
		assertEquals(
				"[0:START][1:jdbchdfs-local][2:spark-client][3:spark-cluster][4:two:spark-cluster][5:timestamp][6:spark-yarn][7:END]"+
				"[0-1][1-2][0-3][3-4][2-5][4-5][0-6][5-7][6-7]",
				graph.toVerboseString());

		assertEquals(
				"<<jdbchdfs-local && spark-client || spark-cluster && two: spark-cluster> && timestamp || spark-yarn>",
				graph.toDSLText());
	}

	@Test
	public void moreSophisticatedScenarios_gh712_1b() {
		TaskNode ctn = parse("<<AA || BB> && CC || DD>");
		Graph graph = ctn.toGraph();
		assertEquals(
				"[0:START][1:AA][2:BB][3:CC][4:DD][5:END]" +
				"[0-1][0-2][1-3][2-3][0-4][3-5][4-5]",
				graph.toVerboseString());
		assertEquals("<<AA || BB> && CC || DD>", graph.toDSLText());
	}

	@Test
	public void moreSophisticatedScenarios_gh712_1c() {
		TaskNode ctn = parse("<<AA || BB> && CC && DD || EE>");
		Graph graph = ctn.toGraph();
		assertEquals(
				"[0:START][1:AA][2:BB][3:CC][4:DD][5:EE][6:END]" +
				"[0-1][0-2][1-3][2-3][3-4][0-5][4-6][5-6]",
				graph.toVerboseString());
		assertEquals("<<AA || BB> && CC && DD || EE>", graph.toDSLText());
		ctn = parse("<<AA || BB> && CC && DD || EE>");
		assertEquals("<<AA || BB> && CC && DD || EE>", ctn.toGraph().toDSLText());
	}

	@Test
	public void moreSophisticatedScenarios_gh712_1d() {
		TaskNode ctn = parse("<<AC && AD || AE && AF> && AG || AB>");
		assertEquals("<<AC && AD || AE && AF> && AG || AB>", ctn.toGraph().toDSLText());
		// Now include a transition
		ctn = parse("<<AC && AD || AE 'jumpOut'-> AH && AF> && AG || AB>");
		Graph graph = ctn.toGraph();
		assertEquals(
				"[0:START][1:AC][2:AD][3:AE][4:AH][5:AF][6:AG][7:AB][8:END]" +
				"[0-1][1-2][0-3][jumpOut:3-4][3-5][2-6][5-6][4-6][0-7][6-8][7-8]",
				graph.toVerboseString());
		// Key thing to observe above is the link from [4-6] which goes from
		// the transition target AH to the end of the split AG
		assertEquals("<<AC && AD || AE 'jumpOut'->AH && AF> && AG || AB>", graph.toDSLText());
	}

	@Test
	public void moreSophisticatedScenarios_gh712_1e() {
		TaskNode ctn = parse("<<AA || BB> && CC && DD || <EE || FF> && GG || HH>");
		Graph graph = ctn.toGraph();
		assertEquals(
				"[0:START][1:AA][2:BB][3:CC][4:DD][5:EE][6:FF][7:GG][8:HH][9:END]" +
				"[0-1][0-2][1-3][2-3][3-4][0-5][0-6][5-7][6-7][0-8][4-9][7-9][8-9]",
				graph.toVerboseString());
		assertEquals("<<AA || BB> && CC && DD || <EE || FF> && GG || HH>", graph.toDSLText());
	}

	@Test
	public void moreSophisticatedScenarios_gh712_1f() {
		// Multiple nested splits in parallel
		TaskNode ctn = parse("<<AA || BB> && CC || <DD || EE> && FF && GG || HH>");
		Graph graph = ctn.toGraph();
		assertEquals(
				"[0:START][1:AA][2:BB][3:CC][4:DD][5:EE][6:FF][7:GG][8:HH][9:END]"+
				"[0-1][0-2][1-3][2-3][0-4][0-5][4-6][5-6][6-7][0-8][3-9][7-9][8-9]",
				graph.toVerboseString());
		assertEquals("<<AA || BB> && CC || <DD || EE> && FF && GG || HH>", graph.toDSLText());
	}

	// Case2: expecting a validation error on the parse because the second spark-cluster
	// isn't labeled
	@Test
	public void moreSophisticatedScenarios_gh712_2() {
		try {
			parse("<<jdbchdfs-local && spark-client || spark-cluster && spark-cluster> && timestamp || spark-yarn>");
			fail();
		}
		catch (TaskValidationException tve) {
			List<TaskValidationProblem> validationProblems = tve.getValidationProblems();
			assertEquals(1, validationProblems.size());
			TaskValidationProblem tvp = validationProblems.get(0);
			assertEquals(53, tvp.getOffset());
			assertEquals(DSLMessage.TASK_VALIDATION_APP_NAME_ALREADY_IN_USE, tvp.getMessage());
		}
	}

	// Case3: no graph when 1 label included?
	@Test
	public void moreSophisticatedScenarios_gh712_3() {
		try {
			parse("<1: jdbchdfs-local && spark-client && timestamp || spark-cluster && spark-cluster && timestamp || spark-yarn>");
			fail();
		}
		catch (TaskValidationException tve) {
			System.out.println(tve);
			List<TaskValidationProblem> validationProblems = tve.getValidationProblems();
			assertEquals(2, validationProblems.size());
			TaskValidationProblem tvp = validationProblems.get(0);
			assertEquals(68, tvp.getOffset());
			assertEquals(DSLMessage.TASK_VALIDATION_APP_NAME_ALREADY_IN_USE, tvp.getMessage());
			tvp = validationProblems.get(1);
			assertEquals(85, tvp.getOffset());
			assertEquals(DSLMessage.TASK_VALIDATION_APP_NAME_ALREADY_IN_USE, tvp.getMessage());
		}
	}

	@Test
	public void wildcardTransition() {
		ctn = parse("foo '*'->wibble");
		assertEquals("foo '*'->wibble", ctn.toDSL());
		ctn = parse("foo \"*\"->wibble");
		assertEquals("foo \"*\"->wibble", ctn.toDSL());
	}

	@Test
	public void splitWithTransition() {
		String spec = "<foo 'completed'->kill || bar>";
		ctn = parse(spec);
		assertEquals(spec, ctn.toDSL());
	}

	@Test
	public void multiLine() {
		TaskNode ctn = parse("<foo\n" + "  'completed'->kill\n" + "  '*'->custard\n" + "  || bar>");
		assertEquals("<foo 'completed'->kill '*'->custard || bar>", ctn.stringify());
	}

	@Test
	public void emptyInput() {
		checkForParseError("", DSLMessage.OOD, 0);
	}

	@Test
	public void toGraph$END() {
		TaskNode ctn = parse("foo 'oranges'->$END");
		assertEquals("foo 'oranges'->$END", ctn.toDSL());
		assertGraph("[0:START][1:foo][2:$END][3:END][0-1][oranges:1-2][1-3]", "foo 'oranges'->$END");
		checkDSLToGraphAndBackToDSL("foo 'oranges'->$END");
	}

	@Test
	public void toGraph$FAIL() {
		String spec = "foo 'oranges'->$FAIL";
		assertEquals(spec, parse(spec).toDSL());
		assertGraph("[0:START][1:foo][2:$FAIL][3:END][0-1][oranges:1-2][1-3]", spec);
		checkDSLToGraphAndBackToDSL(spec);
	}

	// TODO should & end the boo job name? Don't think it does right now
	// js = parse("<foo | completed=boo& bar> || boo");

	@Test
	public void toGraphWithTransition2() {
		// The target transition node hoo is not elsewhere on the list
		String definition = "<foo 'completed'->hoo || bar> && boo && goo";
		assertGraph("[0:START][1:foo][2:hoo][3:bar][4:boo][5:goo][6:END]"
				+ "[0-1][completed:1-2][0-3][1-4][2-4][3-4][4-5][5-6]", definition);
		checkDSLToGraphAndBackToDSL(definition);
	}

	@Test
	public void spacesInProperties() {
		// If a property value in the graph has a space in, quote it when creating dsl
		// If a transition code in the graph is not numeric or * then quote it
		Graph graph = parse("aaa").toGraph();
		Node n = graph.nodes.get(1);

		// Set a property with space in it, if not quoted it should get quoted in
		// conversion
		Map<String, String> properties = new HashMap<>();
		properties.put("one", "bar");
		properties.put("two", "b ar");
		Node newNode = new Node(n.id, n.name, properties);
		graph.nodes.set(1, newNode);
		assertEquals("aaa --one=bar --two='b ar'", graph.toDSLText());

		graph.nodes.add(new Node("3", "bbb"));
		graph.links.add(new Link("1", "3", "tname"));
		assertEquals("aaa --one=bar --two='b ar' 'tname'->bbb", graph.toDSLText());

		graph.nodes.add(new Node("4", "ccc"));
		graph.links.add(new Link("1", "4", "*"));
		assertEquals("aaa --one=bar --two='b ar' 'tname'->bbb '*'->ccc", graph.toDSLText());

		graph.nodes.add(new Node("5", "ddd"));
		graph.links.add(new Link("1", "5", "3"));
		assertEquals("aaa --one=bar --two='b ar' 'tname'->bbb '*'->ccc 3->ddd", graph.toDSLText());

		// When going from DSL to graph, unquote property values and exit codes

		String dsl = "aaa --one=bar --two='b ar' 'tname'->bbb '*'->ccc 3->ddd";
		graph = parse(dsl).toGraph();
		n = graph.nodes.get(1);
		assertEquals("b ar", n.properties.get("two"));
		Link l = graph.links.get(1);
		assertEquals("tname", l.getTransitionName());
		l = graph.links.get(2);
		assertEquals("*", l.getTransitionName());
		l = graph.links.get(3);
		assertEquals("3", l.getTransitionName());
		assertEquals(dsl, graph.toDSLText());
	}

	@Test
	public void wildcardTransitions() {
		// When going from DSL to graph, unquote property values and exit codes
		String dsl = "aaa 'tname'->bbb '*'->ccc 3->ddd";
		assertGraph("[0:START][1:aaa][2:bbb][3:ccc][4:ddd][5:END][0-1][tname:1-2][*:1-3][3:1-4][1-5][2-5][3-5][4-5]",
				dsl);
		Graph graph = parse(dsl).toGraph();
		Link l = graph.links.get(1);
		assertEquals("tname", l.getTransitionName());
		l = graph.links.get(2);
		assertEquals("*", l.getTransitionName());
		l = graph.links.get(3);
		assertEquals("3", l.getTransitionName());
		assertEquals(dsl, graph.toDSLText());
	}

	@Test
	public void multiTransitionToSameTarget() {
		String spec = "foo 'failed'->bbb && bar 'failed'->bbc";
		assertGraph("[0:START][1:foo][2:bbb][3:bar][4:bbc][5:END][0-1][failed:1-2][1-3][failed:3-4][3-5][2-5][4-5]",
				spec);
		checkDSLToGraphAndBackToDSL(spec);
	}

	@Test
	public void extraneousDataError() {
		String jobSpecification = "<a || b> rubbish";
		checkForParseError(jobSpecification, DSLMessage.TASK_MORE_INPUT, 9, "rubbish");
	}

	@Test
	public void incorrectTransition() {
		checkForParseError("foo ||->bar", DSLMessage.TASK_MORE_INPUT, 4, "||");
	}

	// --

	private TaskNode parse(String dsltext) {
		TaskNode ctn = new TaskParser("test", dsltext, true, true).parse();
		return ctn;
	}

	private TaskNode parse(String name, String dsltext) {
		return new TaskParser(name, dsltext, true, true).parse();
	}

	private TaskNode parse(String dsltext, boolean validate) {
		TaskNode ctn = new TaskParser("test", dsltext, true, validate).parse();
		return ctn;
	}

	private TaskNode parse(String composedTaskName, String dsltext, boolean validate) {
		TaskNode ctn = new TaskParser(composedTaskName, dsltext, true, validate).parse();
		return ctn;
	}

	private void assertToken(TokenKind kind, String string, int start, int end, Token t) {
		assertEquals(kind, t.kind);
		assertEquals(string, t.getKind().hasPayload() ? t.stringValue() : new String(t.getKind().getTokenChars()));
		assertEquals(start, t.startPos);
		assertEquals(end, t.endPos);
	}

	private void assertTokens(Tokens tokens, TokenKind... expectedKinds) {
		for (int i = 0; i < expectedKinds.length; i++) {
			assertEquals(expectedKinds[i], tokens.next().getKind());
		}
	}

	private void assertTaskApp(LabelledTaskNode node, String taskAppName) {
		assertTrue(node.isTaskApp());
		assertEquals(((TaskAppNode) node).getName(), taskAppName);
	}

	private void assertFlow(LabelledTaskNode node, String... expectedApps) {
		assertInstanceOf(FlowNode.class, node);
		FlowNode flow = (FlowNode) node;
		List<LabelledTaskNode> series = flow.getSeries();
		assertEquals(expectedApps.length, series.size());
		assertEquals(expectedApps.length, flow.getSeriesLength());
		for (int a = 0; a < expectedApps.length; a++) {
			assertTaskApp(series.get(a), expectedApps[a]);
		}
	}

	private void assertSplit(LabelledTaskNode node, String... expectedApps) {
		assertInstanceOf(SplitNode.class, node);
		SplitNode split = (SplitNode) node;
		List<LabelledTaskNode> series = split.getSeries();
		assertEquals(expectedApps.length, series.size());
		assertEquals(expectedApps.length, split.getSeriesLength());
		for (int a = 0; a < expectedApps.length; a++) {
			FlowNode f = (FlowNode) series.get(a);
			assertEquals(1, f.getSeriesLength());
			assertTaskApp(f.getSeriesElement(0), expectedApps[a]);
		}
	}

	private ParseException checkForParseError(String dsl, DSLMessage msg, int pos, Object... inserts) {
		try {
			TaskNode ctn = parse(dsl);
			fail("expected to fail but parsed " + ctn.stringify());
			return null;
		}
		catch (ParseException e) {
			assertEquals(msg, e.getMessageCode());
			assertEquals(pos, e.getPosition());
			if (inserts != null) {
				for (int i = 0; i < inserts.length; i++) {
					assertEquals(inserts[i], e.getInserts()[i]);
				}
			}
			return e;
		}
	}

	private void assertApps(List<TaskApp> taskApps, String... expectedTaskAppNames) {

		assertEquals(expectedTaskAppNames.length, taskApps.size(), "Expected " + expectedTaskAppNames.length + " but was " + taskApps.size() + ": " + taskApps);
		Set<String> set2 = new HashSet<String>();
		for (TaskApp taskApp : taskApps) {
			StringBuilder s = new StringBuilder();
			if (taskApp.getLabel() != null) {
				s.append(taskApp.getLabel()).append(":");
			}
			s.append(taskApp.getName());
			for (Map.Entry<String, String> arg : taskApp.getArguments().entrySet()) {
				s.append(":").append(arg.getKey()).append("=").append(arg.getValue());
			}
			set2.add(s.toString());
		}
		for (String expectedTaskAppName : expectedTaskAppNames) {
			if (!set2.contains(expectedTaskAppName)) {
				fail("Expected set " + taskApps + " does not contain app '" + expectedTaskAppName + "'");
			}
			set2.remove(expectedTaskAppName);
		}
		if (set2.size() != 0) {
			fail("Unexpected app" + (set2.size() > 1 ? "s" : "") + " :" + set2);
		}
	}

	private void checkDSLToGraphAndBackToDSL(String specification) {
		TaskNode ctn = parse(specification);
		Graph graph = ctn.toGraph();
		assertEquals(specification, graph.toDSLText());
	}

	private void assertGraph(String expectedGraph, String dsl) {
		TaskNode ctn = parse(dsl);
		Graph graph = ctn.toGraph();
		assertEquals(expectedGraph, graph.toVerboseString());
	}

	private void assertTaskApps(String composedTaskName, String spec, String... expectedTaskApps) {
		ctn = parse(composedTaskName, spec, true);
		List<TaskApp> taskApps = ctn.getTaskApps();
		for (int i = 0; i < expectedTaskApps.length; i++) {
			String expectedTaskApp = expectedTaskApps[i];
			StringBuilder s = new StringBuilder();
			s.append(taskApps.get(i).getExecutableDSLName());
			if (taskApps.get(i).getArguments().size() != 0) {
				for (Map.Entry<String, String> arg : taskApps.get(i).getArguments().entrySet()) {
					s.append(":").append(arg.getKey()).append("=").append(arg.getValue());
				}
			}
			assertEquals(s.toString(), expectedTaskApp);
		}
	}

	static class TestVisitor extends TaskVisitor {
		private StringBuilder s = new StringBuilder();

		public void reset() {
			s = new StringBuilder();
		}

		public String getString() {
			return s.toString().trim();
		}

		@Override
		public boolean preVisitSequence(LabelledTaskNode firstNode, int sequenceNumber) {
			s.append(">SN[" + (firstNode.hasLabel() ? firstNode.getLabelString() + ": " : "") + sequenceNumber + "] ");
			return true;
		}

		@Override
		public void postVisitSequence(LabelledTaskNode firstNode, int sequenceNumber) {
			s.append("<SN[" + sequenceNumber + "] ");
		}

		@Override
		public boolean preVisit(FlowNode flow) {
			s.append(">F ");
			return true;
		}

		@Override
		public void visit(FlowNode flow) {
			s.append("=F" + (flow.hasLabel() ? "[" + flow.getLabelString() + ":]" : "") + " ");
		}

		@Override
		public void postVisit(FlowNode flow) {
			s.append("<F ");
		}

		@Override
		public boolean preVisit(SplitNode split) {
			s.append(">S ");
			return true;
		}

		@Override
		public void visit(SplitNode split) {
			s.append("=S" + (split.hasLabel() ? "[" + split.getLabelString() + ":]" : "") + " ");
		}

		@Override
		public void postVisit(SplitNode split) {
			s.append("<S ");
		}

		@Override
		public boolean preVisit(TaskAppNode taskApp) {
			s.append(">TA ");
			return true;
		}

		@Override
		public void visit(TaskAppNode taskApp) {
			s.append("=TA[" + (taskApp.hasLabel() ? taskApp.getLabelString() + ": " : "") + taskApp.getName() + "] ");
		}

		@Override
		public void postVisit(TaskAppNode taskApp) {
			s.append("<TA ");
		}

		@Override
		public boolean preVisit(TransitionNode transition) {
			s.append(">T ");
			return true;
		}

		@Override
		public void visit(TransitionNode transition) {
			s.append("=T["
					+ (transition.isExitCodeCheck() ? transition.getStatusToCheck()
							: "'" + transition.getStatusToCheck() + "'")
					+ "->" + (transition.isTargetApp() ? transition.getTargetApp().stringify()
							: ":" + transition.getTargetLabel())
					+ "] ");
		}

		@Override
		public void postVisit(TransitionNode transition) {
			s.append("<T ");
		}

	}

}
