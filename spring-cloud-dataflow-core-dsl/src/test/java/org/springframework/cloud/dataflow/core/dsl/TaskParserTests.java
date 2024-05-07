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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Test the parser and visitor infrastructure. Check it accepts expected data and
 * correctly handles bad data. Check that the parsed out converts to/from the graph format
 * that the UI can use.
 *
 * @author Andy Clement
 * @author David Turanski
 * @author Michael Minella
 * @author Eric Bottard
 * @author Corneil du Plessis
 */
public class TaskParserTests {

	private TaskNode ctn;

	private TaskAppNode appNode;

	@Test
	public void oneApp() {
		TaskNode taskNode = parse("foo");
		assertThat(taskNode.isComposed()).isFalse();
		TaskAppNode appNode = taskNode.getTaskApp();
		assertThat(appNode.getName()).isEqualTo("foo");
		assertThat(appNode.getArguments().length).isEqualTo(0);
		assertThat(appNode.startPos).isEqualTo(0);
		assertThat(appNode.endPos).isEqualTo(3);
	}

	@Test
	public void hyphenatedAppName() {
		appNode = parse("gemfire-cq").getTaskApp();
		assertThat(appNode.stringify(true)).isEqualTo("gemfire-cq:0>10");
	}

	@Test
	public void oneAppWithParam() {
		appNode = parse("foo --name=value").getTaskApp();
		assertThat(appNode.stringify(true)).isEqualTo("foo --name=value:0>16");
	}

	@Test
	public void oneAppWithTwoParams() {
		appNode = parse("foo --name=value --x=y").getTaskApp();

		assertThat(appNode.getName()).isEqualTo("foo");
		ArgumentNode[] args = appNode.getArguments();
		assertThat(args).isNotNull();
		assertThat(args.length).isEqualTo(2);
		assertThat(args[0].getName()).isEqualTo("name");
		assertThat(args[0].getValue()).isEqualTo("value");
		assertThat(args[1].getName()).isEqualTo("x");
		assertThat(args[1].getValue()).isEqualTo("y");

		assertThat(appNode.stringify(true)).isEqualTo("foo --name=value --x=y:0>22");
	}

	@Test
	public void testParameters() {
		String module = "gemfire-cq --query='Select * from /Stocks where symbol=''VMW''' --regionName=foo --foo=bar";
		TaskAppNode gemfireApp = parse(module).getTaskApp();
		Map<String, String> parameters = gemfireApp.getArgumentsAsMap();
		assertThat(parameters.size()).isEqualTo(3);
		assertThat(parameters.get("query")).isEqualTo("Select * from /Stocks where symbol='VMW'");
		assertThat(parameters.get("regionName")).isEqualTo("foo");
		assertThat(parameters.get("foo")).isEqualTo("bar");

		module = "test";
		parameters = parse(module).getTaskApp().getArgumentsAsMap();
		assertThat(parameters.size()).isEqualTo(0);

		module = "foo --x=1 --y=two ";
		parameters = parse(module).getTaskApp().getArgumentsAsMap();
		assertThat(parameters.size()).isEqualTo(2);
		assertThat(parameters.get("x")).isEqualTo("1");
		assertThat(parameters.get("y")).isEqualTo("two");

		module = "foo --x=1a2b --y=two ";
		parameters = parse(module).getTaskApp().getArgumentsAsMap();
		assertThat(parameters.size()).isEqualTo(2);
		assertThat(parameters.get("x")).isEqualTo("1a2b");
		assertThat(parameters.get("y")).isEqualTo("two");

		module = "foo --x=2";
		parameters = parse(module).getTaskApp().getArgumentsAsMap();
		assertThat(parameters.size()).isEqualTo(1);
		assertThat(parameters.get("x")).isEqualTo("2");

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
		assertThat(props.get("expression")).isEqualTo("--payload");
	}

	@Test
	public void expressions_xd159_2() {
		// need quotes around an argument value with a space in it
		checkForParseError("transform --expression=new StringBuilder(payload).reverse()", DSLMessage.TASK_MORE_INPUT,
				27);
		appNode = parse("transform --expression='new StringBuilder(payload).reverse()'").getTaskApp();
		assertThat(appNode.getArgumentsAsMap().get("expression")).isEqualTo("new StringBuilder(payload).reverse()");
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
		assertThat(props.get("expression")).isEqualTo("new StringBuilder(payload).reverse()");
	}

	@Test
	public void expressions_xd159_4() {
		appNode = parse("transform --expression=\"'Hello, world!'\"").getTaskApp();
		Map<String, String> props = appNode.getArgumentsAsMap();
		assertThat(props.get("expression")).isEqualTo("'Hello, world!'");
		appNode = parse("transform --expression='''Hello, world!'''").getTaskApp();
		props = appNode.getArgumentsAsMap();
		assertThat(props.get("expression")).isEqualTo("'Hello, world!'");
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
		assertThat(props.get("expression")).isEqualTo("payload == 'foo'");
	}

	@Test
	public void expressions_gh1_2() {
		appNode = parse("filter --expression='new Foo()'").getTaskApp();
		Map<String, String> props = appNode.getArgumentsAsMap();
		assertThat(props.get("expression")).isEqualTo("new Foo()");
	}

	@Test
	public void errorCases01() {
		checkForParseError(".", DSLMessage.EXPECTED_APPNAME, 0, ".");
		assertThat(parse("foo", "a-_", true).getTaskApp().getName()).isEqualTo("a-_");
		assertThat(parse("foo", "a_b", true).getTaskApp().getName()).isEqualTo("a_b");
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
		assertThat(appNode.getArgumentsAsMap().get("expression")).isEqualTo("payload.replace(\"abc\", \"\")");

		appNode = parse("transform --expression='payload.replace(\"abc\", '''')'").getTaskApp();
		assertThat(appNode.getArgumentsAsMap().get("expression")).isEqualTo("payload.replace(\"abc\", '')");
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
			assertThat(e.getMessageCode()).isEqualTo(DSLMessage.ILLEGAL_TASK_NAME);
			assertThat(e.getPosition()).isEqualTo(0);
			assertThat(e.getInserts()[0]).isEqualTo(taskName);
		}
	}

	@Test
	public void executableDsl() {
		TaskNode ctn = parse("foo", "appA && appB", true);
		List<TaskApp> taskApps = ctn.getTaskApps();
		assertThat(taskApps.get(0).getName()).isEqualTo("appA");
		assertThat(taskApps.get(0).getExecutableDSLName()).isEqualTo("foo-appA");
		assertThat(taskApps.get(1).getName()).isEqualTo("appB");
		assertThat(taskApps.get(1).getExecutableDSLName()).isEqualTo("foo-appB");

		ctn = parse("bar", "appC && goo: appC", true);
		taskApps = ctn.getTaskApps();
		assertThat(taskApps.get(0).getName()).isEqualTo("appC");
		assertThat(taskApps.get(0).getExecutableDSLName()).isEqualTo("bar-appC");
		assertThat(taskApps.get(1).getName()).isEqualTo("appC");
		assertThat(taskApps.get(1).getExecutableDSLName()).isEqualTo("bar-goo");

		// flows
		assertThat(parse("foo", "appA", true).toExecutableDSL()).isEqualTo("foo-appA");
		assertThat(parse("foo", "appA && appB", true).toExecutableDSL()).isEqualTo("foo-appA && foo-appB");
		assertThat(parse("foo", "appA && appB && appC", true).toExecutableDSL()).isEqualTo("foo-appA && foo-appB && foo-appC");

		assertTaskApps("foo", "appA", "foo-appA");
		assertTaskApps("foo", "appA && appB", "foo-appA", "foo-appB");
		assertTaskApps("foo", "appA && appB && appC", "foo-appA", "foo-appB", "foo-appC");

		// arguments
		assertThat(parse("foo", "appA --p1=v1 --p2=v2", true).toExecutableDSL()).isEqualTo("foo-appA");
		assertThat(parse("foo", "appA --p2=v2 && appB --p3=v3", true).toExecutableDSL()).isEqualTo("foo-appA && foo-appB");
		assertTaskApps("foo", "appA --p1=v2", "foo-appA:p1=v2");
		assertTaskApps("foo", "appA --p1=v2 && goo: appB --p2=v2", "foo-appA:p1=v2", "foo-goo:p2=v2");
		assertTaskApps("foo", "appA 0->x:appA --p1=v1", "foo-appA", "foo-x:p1=v1");

		// labels
		assertThat(parse("bar", "goo:appA", true).toExecutableDSL()).isEqualTo("bar-goo");
		assertThat(parse("fo", "aaa: appA && bbb: appA", true).toExecutableDSL()).isEqualTo("fo-aaa && fo-bbb");

		assertTaskApps("bar", "goo:appA", "bar-goo");
		assertTaskApps("bar", "appA && goo: appA", "bar-appA", "bar-goo");

		// transitions
		assertThat(parse("foo", "appA 'c'->appC && appB", true).toExecutableDSL()).isEqualTo("foo-appA 'c'->foo-appC && foo-appB");
		assertThat(parse("foo", "appA 'c'->appC 'd'->appD && " + "appB", true).toExecutableDSL()).isEqualTo("foo-appA 'c'->foo-appC 'd'->foo-appD && foo-appB");
		assertThat(parse("foo", "appA 1->appC 2->appD && appB", true).toExecutableDSL()).isEqualTo("foo-appA 1->foo-appC 2->foo-appD && foo-appB");
		assertThat(parse("foo", "aaa: appA 1->appC 2->:aaa", true).toExecutableDSL()).isEqualTo("foo-aaa 1->foo-appC 2->:aaa");

		// splits
		assertThat(parse("foo", "<appA || appB>", true).toExecutableDSL()).isEqualTo("<foo-appA || foo-appB>");
		assertThat(parse("foo", "<appA || appB && appC>", true).toExecutableDSL()).isEqualTo("<foo-appA || foo-appB && foo-appC>");
		assertThat(parse("foo", "<<appA && appD || appE> || " + "appB>", true).toExecutableDSL()).isEqualTo("<<foo-appA && foo-appD || foo-appE> || foo-appB>");
		assertThat(parse("foo", "<<appA || x: appA> || appB>", true).toExecutableDSL()).isEqualTo("<<foo-appA || foo-x> || foo-appB>");

		// splits and flows
		assertThat(parse("foo", "AAA && " + "FFF 'FAILED' -> EEE && <BBB||CCC> && DDD", true).toExecutableDSL()).isEqualTo("foo-AAA && foo-FFF 'FAILED'->foo-EEE && <foo-BBB || foo-CCC> && foo-DDD");
		assertTaskApps("foo", "AAA && FFF 'FAILED' -> EEE && <BBB||CCC> && DDD", "foo-AAA", "foo-FFF", "foo-EEE",
				"foo-BBB", "foo-CCC", "foo-DDD");
		assertThat(parse("<A || B> && <C||D>", true).toExecutableDSL()).isEqualTo("<test-A || test-B> && <test-C || test-D>");
		assertThat(parse("<A || B || C> && <D||E>", true).toExecutableDSL()).isEqualTo("<test-A || test-B || test-C> && <test-D || test-E>");
		assertThat(parse("<A || B || C> && D", true).toExecutableDSL()).isEqualTo("<test-A || test-B || test-C> && test-D");
		assertThat(parse("<A || <B && C || D>>", true).toExecutableDSL()).isEqualTo("<test-A || <test-B && test-C || test-D>>");
		assertThat(parse("<A || <B || D && E>>", true).toExecutableDSL()).isEqualTo("<test-A || <test-B || test-D && test-E>>");

		ctn = parse("AAA 0->BBB");
		List<TransitionNode> transitions = ((TaskAppNode) ctn.getSequences().get(0).getSeriesElement(0))
				.getTransitions();
		assertThat(transitions.get(0).getStatusToCheckInDSLForm()).isEqualTo("0");

		ctn = parse("AAA '0'->BBB");
		transitions = ((TaskAppNode) ctn.getSequences().get(0).getSeriesElement(0)).getTransitions();
		assertThat(transitions.get(0).getStatusToCheckInDSLForm()).isEqualTo("'0'");

		ctn = parse("AAA *->BBB '*'->CCC");
		transitions = ((TaskAppNode) ctn.getSequences().get(0).getSeriesElement(0)).getTransitions();
		assertThat(transitions.get(0).getStatusToCheckInDSLForm()).isEqualTo("*");
		assertThat(transitions.get(1).getStatusToCheckInDSLForm()).isEqualTo("'*'");

		assertThat(parse("AAA 'failed' -> BBB * -> CCC").toExecutableDSL()).isEqualTo("test-AAA 'failed'->test-BBB *->test-CCC");
		assertThat(parse("AAA 'failed' -> BBB '*' -> CCC").toExecutableDSL()).isEqualTo("test-AAA 'failed'->test-BBB '*'->test-CCC");
		assertThat(parse("AAA 1 -> BBB 2 -> CCC").toExecutableDSL()).isEqualTo("test-AAA 1->test-BBB 2->test-CCC");
	}

	@Test
	public void isComposedTask() {
		ctn = parse("appA 'foo' -> appB");
		assertThat(ctn.isComposed()).isTrue();
		assertThat(ctn.getTaskApp()).isNull();
		assertGraph("[0:START][1:appA][2:appB][3:END][0-1][foo:1-2][1-3][2-3]", "appA 'foo' -> appB");
		ctn = parse("appA");
		assertThat(ctn.isComposed()).isFalse();
		assertThat(ctn.getTaskApp()).isNotNull();
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
		assertThat(ctn.getTaskText()).isEqualTo("FooApp");
		assertThat(ctn.getStartPos()).isEqualTo(0);
		assertThat(ctn.getEndPos()).isEqualTo(6);
		assertThat(ctn.stringify()).isEqualTo("FooApp");
		LabelledTaskNode node = ctn.getStart();
		assertThat(node.isSplit()).isFalse();
		assertThat(node.isFlow()).isTrue();
		assertFlow(node, "FooApp");
		assertThat(node.getSeriesElement(0).isTaskApp()).isTrue();
	}

	@Test
	public void twoAppFlow() {
		ctn = parse("FooApp  &&  BarApp");

		assertThat(ctn.getTaskText()).isEqualTo("FooApp  &&  BarApp");
		assertThat(ctn.getStartPos()).isEqualTo(0);
		assertThat(ctn.getEndPos()).isEqualTo(18);
		assertThat(ctn.stringify()).isEqualTo("FooApp && BarApp");

		LabelledTaskNode node = ctn.getStart();
		assertThat(node.isSplit()).isFalse();
		assertThat(node.isFlow()).isTrue();
		assertThat(node.isTaskApp()).isFalse();

		FlowNode flow = (FlowNode) node;
		List<LabelledTaskNode> series = flow.getSeries();
		assertThat(series.size()).isEqualTo(2);
		assertThat(flow.getSeriesLength()).isEqualTo(2);
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

		assertThat(ctn.getTaskText()).isEqualTo("< FooApp>");
		assertThat(ctn.getStartPos()).isEqualTo(0);
		assertThat(ctn.getEndPos()).isEqualTo(9);
		assertThat(ctn.stringify()).isEqualTo("<FooApp>");

		LabelledTaskNode node = ctn.getStart();
		assertThat(node.isFlow()).isTrue();
		node = node.getSeriesElement(0);
		assertThat(node.isSplit()).isTrue();
		assertThat(node.isTaskApp()).isFalse();

		SplitNode split = (SplitNode) node;
		List<LabelledTaskNode> series = split.getSeries();
		assertThat(series.size()).isEqualTo(1);
		assertThat(split.getSeriesLength()).isEqualTo(1);
		assertFlow(series.get(0), "FooApp");
		assertFlow(split.getSeriesElement(0), "FooApp");
	}

	@Test
	public void twoAppSplit() {
		ctn = parse("< FooApp  ||    BarApp>");

		assertThat(ctn.getTaskText()).isEqualTo("< FooApp  ||    BarApp>");
		assertThat(ctn.getStartPos()).isEqualTo(0);
		assertThat(ctn.getEndPos()).isEqualTo(23);
		assertThat(ctn.stringify()).isEqualTo("<FooApp || BarApp>");

		LabelledTaskNode node = ctn.getStart();
		assertThat(node.isFlow()).isTrue();
		node = node.getSeriesElement(0);
		assertThat(node.isSplit()).isTrue();
		assertThat(node.isTaskApp()).isFalse();

		SplitNode split = (SplitNode) node;
		List<LabelledTaskNode> series = split.getSeries();
		assertThat(series.size()).isEqualTo(2);
		assertThat(split.getSeriesLength()).isEqualTo(2);
		assertFlow(series.get(0), "FooApp");
		assertFlow(split.getSeriesElement(0), "FooApp");
		assertFlow(series.get(1), "BarApp");
		assertFlow(split.getSeriesElement(1), "BarApp");
	}

	@Test
	public void appWithOneTransition() {
		ctn = parse("App1 0->App2");
		assertThat(ctn.getName()).isEqualTo("test");
		assertThat(ctn.getTaskText()).isEqualTo("App1 0->App2");
		assertThat(ctn.getStartPos()).isEqualTo(0);
		assertThat(ctn.getEndPos()).isEqualTo(12);
		assertThat(ctn.stringify()).isEqualTo("App1 0->App2");
		LabelledTaskNode firstNode = ctn.getStart();
		assertThat(firstNode.isFlow()).isTrue();
		List<TransitionNode> transitions = ((TaskAppNode) firstNode.getSeriesElement(0)).getTransitions();
		assertThat(transitions.size()).isEqualTo(1);
		TransitionNode transition = transitions.get(0);
		assertThat(transition.getStatusToCheck()).isEqualTo("0");
		assertThat(transition.getTargetDslText()).isEqualTo("App2");
		assertThat(transition.getStartPos()).isEqualTo(5);
		assertThat(transition.getEndPos()).isEqualTo(12);
	}

	@Test
	public void appWithTwoTransitions() {
		ctn = parse("App1 0->App2 'abc' ->   App3");
		assertThat(ctn.getTaskText()).isEqualTo("App1 0->App2 'abc' ->   App3");
		assertThat(ctn.getStartPos()).isEqualTo(0);
		assertThat(ctn.getEndPos()).isEqualTo(28);
		assertThat(ctn.stringify()).isEqualTo("App1 0->App2 'abc'->App3");
		LabelledTaskNode node = ctn.getStart();
		assertThat(node.isFlow()).isTrue();
		node = node.getSeriesElement(0);
		List<TransitionNode> transitions = ((TaskAppNode) node).getTransitions();
		assertThat(transitions.size()).isEqualTo(2);
		TransitionNode transition = transitions.get(0);
		assertThat(transition.getStatusToCheck()).isEqualTo("0");
		assertThat(transition.isExitCodeCheck()).isTrue();
		assertThat(transition.getTargetDslText()).isEqualTo("App2");
		assertThat(transition.getStartPos()).isEqualTo(5);
		assertThat(transition.getEndPos()).isEqualTo(12);
		transition = transitions.get(1);
		assertThat(transition.getStatusToCheck()).isEqualTo("abc");
		assertThat(transition.isExitCodeCheck()).isFalse();
		assertThat(transition.getTargetDslText()).isEqualTo("App3");
		assertThat(transition.getStartPos()).isEqualTo(13);
		assertThat(transition.getEndPos()).isEqualTo(28);
	}

	@Test
	public void appWithWildcardTransitions() {
		ctn = parse("App1 *->App2 '*'->App3");
		assertThat(ctn.getTaskText()).isEqualTo("App1 *->App2 '*'->App3");
		assertThat(ctn.getStartPos()).isEqualTo(0);
		assertThat(ctn.getEndPos()).isEqualTo(22);
		assertThat(ctn.stringify()).isEqualTo("App1 *->App2 '*'->App3");
		LabelledTaskNode node = ctn.getStart();
		node = node.getSeriesElement(0);
		assertThat(node.isTaskApp()).isTrue();
		List<TransitionNode> transitions = ((TaskAppNode) node).getTransitions();
		assertThat(transitions.size()).isEqualTo(2);

		TransitionNode transition = transitions.get(0);
		assertThat(transition.getStatusToCheck()).isEqualTo("*");
		assertThat(transition.isExitCodeCheck()).isTrue();
		assertThat(transition.getTargetDslText()).isEqualTo("App2");
		assertThat(transition.getStartPos()).isEqualTo(5);
		assertThat(transition.getEndPos()).isEqualTo(12);
		transition = transitions.get(1);
		assertThat(transition.getStatusToCheck()).isEqualTo("*");
		assertThat(transition.isExitCodeCheck()).isFalse();
		assertThat(transition.getTargetDslText()).isEqualTo("App3");
		assertThat(transition.getStartPos()).isEqualTo(13);
		assertThat(transition.getEndPos()).isEqualTo(22);
	}

	@Test
	public void appWithLabelReferenceTransition() {
		ctn = parse("App1 'foo'->:something", false);
		assertThat(ctn.getTaskText()).isEqualTo("App1 'foo'->:something");
		assertThat(ctn.getStartPos()).isEqualTo(0);
		assertThat(ctn.getEndPos()).isEqualTo(22);
		assertThat(ctn.stringify()).isEqualTo("App1 'foo'->:something");
		LabelledTaskNode firstNode = ctn.getStart();
		assertFlow(firstNode, "App1");
		List<TransitionNode> transitions = ((TaskAppNode) firstNode.getSeriesElement(0)).getTransitions();
		assertThat(transitions.size()).isEqualTo(1);
		TransitionNode transition = transitions.get(0);
		assertThat(transition.getStatusToCheck()).isEqualTo("foo");
		assertThat(transition.isExitCodeCheck()).isFalse();
		assertThat(transition.getTargetDslText()).isEqualTo(":something");
		assertThat(transition.getTargetLabel()).isEqualTo("something");
		assertThat(transition.getStartPos()).isEqualTo(5);
		assertThat(transition.getEndPos()).isEqualTo(22);
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
		assertThat(start.getLabelString()).isEqualTo("foo");
		FlowNode f = (FlowNode) start;
		assertThat(f.getLabelString()).isEqualTo("foo");
		assertThat(((TaskAppNode) f.getSeriesElement(0)).getName()).isEqualTo("appA");

		ctn = parse("foo: <appA || appB>");
		start = ctn.getStart();
		assertThat(start.getLabelString()).isEqualTo("foo");
		SplitNode s = (SplitNode) start.getSeriesElement(0);
		assertSplit(s, "appA", "appB");

		ctn = parse("foo: appA && appB");
		start = ctn.getStart();
		assertThat(start.getLabelString()).isEqualTo("foo");
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
		assertThat(validationProblems.size()).isEqualTo(1);
		assertThat(validationProblems.get(0).getMessage()).isEqualTo(DSLMessage.TASK_VALIDATION_TRANSITION_TARGET_LABEL_UNDEFINED);
		assertThat(validationProblems.get(0).getOffset()).isEqualTo(3);

		spec = "<aa 'foo'->:split && bb && cc || dd>";
		ctn = parse(spec, false);
		validationProblems = ctn.validate();
		assertThat(validationProblems.size()).isEqualTo(1);
		assertThat(validationProblems.get(0).getMessage()).isEqualTo(DSLMessage.TASK_VALIDATION_TRANSITION_TARGET_LABEL_UNDEFINED);
		assertThat(validationProblems.get(0).getOffset()).isEqualTo(4);
	}

	@Test
	public void visitors() {
		ctn = parse("appA");
		TestVisitor tv = new TestVisitor();
		ctn.accept(tv);
		assertThat(tv.getString()).isEqualTo(">SN[0] >F =F >TA =TA[appA] <TA <F <SN[0]");

		ctn = parse("foo: appA");
		tv.reset();
		ctn.accept(tv);
		assertThat(tv.getString()).isEqualTo(">SN[foo: 0] >F =F[foo:] >TA =TA[foo: appA] <TA <F <SN[0]");

		ctn = parse("appA && appB");
		tv.reset();
		ctn.accept(tv);
		assertThat(tv.getString()).isEqualTo(">SN[0] >F =F >TA =TA[appA] <TA >TA =TA[appB] <TA <F <SN[0]");

		ctn = parse("<appA || appB>");
		tv.reset();
		ctn.accept(tv);
		assertThat(tv.getString()).isEqualTo(">SN[0] >F =F >S =S >F =F >TA =TA[appA] <TA <F >F =F >TA =TA[appB] <TA <F <S <F <SN[0]");

		ctn = parse("<appA && appB|| appC>");
		tv.reset();
		ctn.accept(tv);
		assertThat(tv.getString()).isEqualTo(">SN[0] >F =F >S =S >F =F >TA =TA[appA] <TA >TA =TA[appB] <TA <F >F =F >TA =TA[appC] <TA <F <S "
		+ "<F <SN[0]");

		ctn = parse("appA 0->:foo", false);
		tv.reset();
		ctn.accept(tv);
		assertThat(tv.getString()).isEqualTo(">SN[0] >F =F >TA =TA[appA] >T =T[0->:foo] <T <TA <F <SN[0]");

		ctn = parse("appA 0->appB");
		tv.reset();
		ctn.accept(tv);
		assertThat(tv.getString()).isEqualTo(">SN[0] >F =F >TA =TA[appA] >T =T[0->appB] <T <TA <F <SN[0]");

		ctn = parse("appA;appB", false);
		tv.reset();
		ctn.accept(tv);
		assertThat(tv.getString()).isEqualTo(">SN[0] >F =F >TA =TA[appA] <TA <F <SN[0] >SN[1] >F =F >TA =TA[appB] <TA <F <SN[1]");

		ctn = parse("appA && appB 0->:foo *->appC;foo: appD && appE", false);
		assertApps(ctn.getTaskApps(), "appA", "appB", "appC", "foo:appD", "appE");
		tv.reset();
		ctn.accept(tv);
		assertThat(tv.getString()).isEqualTo(">SN[0] >F =F >TA =TA[appA] <TA >TA =TA[appB] >T =T[0->:foo] <T >T =T[*->appC] <T <TA <F <SN[0] "
		+ ">SN[foo: 1] >F =F[foo:] >TA =TA[foo: appD] <TA >TA =TA[appE] <TA <F <SN[1]");
	}

	@Test
	public void multiline() {
		ctn = parse("appA 0->:label1 && appB\nlabel1: appC");
	}

	@Test
	public void multiSequence() {
		TaskNode ctn = parse("appA\n  0->:foo\n  *->appB\n  && appE;foo: appC && appD");
		LabelledTaskNode start = ctn.getStart(); // get the root of the AST starting appA
		assertThat(start).isNotNull();
		List<LabelledTaskNode> sequences = ctn.getSequences();
		LabelledTaskNode labelledTaskNode = sequences.get(1);
		assertThat(labelledTaskNode.getLabelString()).isEqualTo("foo");
		LabelledTaskNode fooSequence = ctn.getSequenceWithLabel("foo"); // get the AST for foo: ...
		assertThat(fooSequence).isNotNull();
		TestVisitor tv = new TestVisitor();
		ctn.accept(tv);
		assertThat(tv.getString()).isEqualTo(">SN[0] >F =F >TA =TA[appA] >T =T[0->:foo] <T >T =T[*->appB] <T <TA >TA =TA[appE] <TA <F <SN[0] "
		+ ">SN[foo: 1] >F =F[foo:] >TA =TA[foo: appC] <TA >TA =TA[appD] <TA <F <SN[1]");
	}

	@Test
	public void validator() {
		TaskValidatorVisitor validator = new TaskValidatorVisitor();
		ctn = parse("appA");
		ctn.accept(validator);
		assertThat(validator.hasProblems()).isFalse();

		validator.reset();
		ctn = parse("appA;appB", false);
		ctn.accept(validator);
		List<TaskValidationProblem> problems = validator.getProblems();
		assertThat(problems.size()).isEqualTo(1);
		assertThat(problems.get(0).getMessage()).isEqualTo(DSLMessage.TASK_VALIDATION_SECONDARY_SEQUENCES_MUST_BE_NAMED);
		assertThat(problems.get(0).getOffset()).isEqualTo(5);
		assertThat(problems.get(0).toString()).isEqualTo("158E:(pos 5): secondary sequences must have labels or are unreachable");
		assertThat(problems.get(0).toStringWithContext()).isEqualTo("158E:(pos 5): secondary sequences must have labels or are unreachable\nappA;appB\n     ^\n");

		validator.reset();
		ctn = parse("appA;foo: appB");
		ctn.accept(validator);
		assertThat(validator.hasProblems()).isFalse();

		validator.reset();
		ctn = parse("appA;foo: appB\nappC", false);
		ctn.accept(validator);
		problems = validator.getProblems();
		assertThat(problems.size()).isEqualTo(1);
		assertThat(problems.get(0).getMessage()).isEqualTo(DSLMessage.TASK_VALIDATION_SECONDARY_SEQUENCES_MUST_BE_NAMED);
		assertThat(problems.get(0).getOffset()).isEqualTo(15);
		assertThat(problems.get(0).toString()).isEqualTo("158E:(pos 15): secondary sequences must have labels or are unreachable");
		assertThat(problems.get(0).toStringWithContext()).isEqualTo("158E:(pos 15): secondary sequences must have labels or are unreachable\nappC\n^\n");

		validator.reset();
		ctn = parse("appA && appA", false);
		ctn.accept(validator);
		problems = validator.getProblems();
		assertThat(problems.size()).isEqualTo(1);
		assertThat(problems.get(0).getMessage()).isEqualTo(DSLMessage.TASK_VALIDATION_APP_NAME_ALREADY_IN_USE);
		assertThat(problems.get(0).getOffset()).isEqualTo(8);
		validator.reset();
		ctn = parse("appA 'foo' -> appA", false);
		ctn.accept(validator);
		problems = validator.getProblems();
		assertThat(problems.size()).isEqualTo(1);
		assertThat(problems.get(0).getMessage()).isEqualTo(DSLMessage.TASK_VALIDATION_APP_NAME_ALREADY_IN_USE);
		assertThat(problems.get(0).getOffset()).isEqualTo(14);
		validator.reset();
		ctn = parse("appA 'foo' -> appA: appB", false);
		ctn.accept(validator);
		problems = validator.getProblems();
		assertThat(problems.size()).isEqualTo(1);
		assertThat(problems.get(0).getMessage()).isEqualTo(DSLMessage.TASK_VALIDATION_LABEL_CLASHES_WITH_TASKAPP_NAME);
		assertThat(problems.get(0).getOffset()).isEqualTo(14);
		validator.reset();
		ctn = parse("label1: appA 'foo' -> label1: appB", false);
		ctn.accept(validator);
		problems = validator.getProblems();
		assertThat(problems.size()).isEqualTo(1);
		assertThat(problems.get(0).getMessage()).isEqualTo(DSLMessage.TASK_VALIDATION_DUPLICATE_LABEL);
		assertThat(problems.get(0).getOffset()).isEqualTo(22);
		validator.reset();
		ctn = parse("label1: appA 'foo' -> label1", false);
		ctn.accept(validator);
		problems = validator.getProblems();
		assertThat(problems.size()).isEqualTo(1);
		assertThat(problems.get(0).getMessage()).isEqualTo(DSLMessage.TASK_VALIDATION_APP_NAME_CLASHES_WITH_LABEL);
		assertThat(problems.get(0).getOffset()).isEqualTo(22);
	}

	@Test
	public void labels() {
		// basic task
		ctn = parse("aaa: appA");
		LabelledTaskNode flow = ctn.getStart();
		assertThat(flow.getLabelString()).isEqualTo("aaa");
		TaskAppNode taskApp = (TaskAppNode) flow.getSeriesElement(0);
		assertThat(taskApp.getLabelString()).isEqualTo("aaa");

		// flows
		ctn = parse("aaa: appA && bbb: appB");
		taskApp = (TaskAppNode) ctn.getStart().getSeriesElement(1);
		assertThat(taskApp.getLabelString()).isEqualTo("bbb");

		// splits
		ctn = parse("outer:<aaa: appA || bbb: appB>");
		flow = ctn.getStart();
		assertThat(flow.getLabelString()).isEqualTo("outer");
		SplitNode s = (SplitNode) flow.getSeriesElement(0);
		assertThat(s.getLabelString()).isEqualTo("outer");
		taskApp = (TaskAppNode) (s.getSeriesElement(0).getSeriesElement(0));
		assertThat(taskApp.getLabelString()).isEqualTo("aaa");
		taskApp = (TaskAppNode) (s.getSeriesElement(1).getSeriesElement(0));
		assertThat(taskApp.getLabelString()).isEqualTo("bbb");

		// parentheses
		ctn = parse("(aaa: appA && appB)");
		taskApp = (TaskAppNode) ctn.getStart().getSeriesElement(0);
		assertThat(taskApp.getLabelString()).isEqualTo("aaa");

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
		assertThat(graph.toDSLText()).isEqualTo("timestamp");
	}
	
	@Test
	public void graphToText_3667() {
		assertGraph("[0:START][1:sql-executor-task:password=password:url=jdbc:postgresql://127.0.0.1:5432/postgres:script-location=/dataflow/scripts/test.sql:username=postgres]"+
					"[2:END][0-1][1-2]","sql-executor-task --script-location=/dataflow/scripts/test.sql --username=postgres --password=password --url=jdbc:postgresql://127.0.0.1:5432/postgres");
		
		assertGraph("[0:START][1:t1:timestamp][2:t2:timestamp][3:t3:timestamp][4:END][0-1][FAILED:1-2][1-3][3-4][2-4]",
					"t1: timestamp 'FAILED'->t2: timestamp && t3: timestamp");

		TaskNode ctn = parse("t1: timestamp 'FAILED'->t2: timestamp && t3: timestamp");
		Graph graph = ctn.toGraph();
		assertThat(graph.toDSLText()).isEqualTo("t1: timestamp 'FAILED'->t2: timestamp && t3: timestamp");
		
		ctn = parse("t1: timestamp --format=aabbcc 'FAILED'->t2: timestamp && t3: timestamp --format=gghhii");
		graph = ctn.toGraph();
		assertThat(graph.toDSLText()).isEqualTo("t1: timestamp --format=aabbcc 'FAILED'->t2: timestamp && t3: timestamp --format=gghhii");

		ctn = parse("t1: timestamp --format=aabbcc 'FAILED'->t2: timestamp --format=ddeeff && t3: timestamp --format=gghhii");
		graph = ctn.toGraph();
		Node node = graph.nodes.get(2);
		assertThat(node.properties.get("format")).isEqualTo("ddeeff");
		assertThat(graph.toDSLText()).isEqualTo("t1: timestamp --format=aabbcc 'FAILED'->t2: timestamp --format=ddeeff && t3: timestamp --format=gghhii");
		
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
		assertThat(graph.toDSLText()).isEqualTo("AppA");
		
		assertGraph("[0:START][1:AppA][2:AppB][3:END][0-1][1-2][2-3]","<AppA> && AppB");
		ctn = parse("<AppA> && AppB");
		graph = ctn.toGraph();
		assertThat(graph.toDSLText()).isEqualTo("AppA && AppB");
		
		assertGraph("[0:START][1:AppA][2:AppC][3:AppB][4:END][0-1][99:1-2][1-3][2-3][3-4]","<AppA 99 -> AppC> && AppB");
		ctn = parse("<AppA 99->AppC> && AppB");
		graph = ctn.toGraph();
		assertThat(graph.toDSLText()).isEqualTo("<AppA 99->AppC> && AppB");

		// Check it still does the right thing when the split does have multple:
		ctn = parse("<AppA 99->AppC || AppD> && AppB");
		graph = ctn.toGraph();
		assertThat(graph.toDSLText()).isEqualTo("<AppA 99->AppC || AppD> && AppB");
		
		// This is the test specifically for issue 3263
		ctn = parse("<Import: timestamp 'Error2'->T2: timestamp 'Error'->T1: timestamp> && Backwards: timestamp");
		// Key thing to note from here is that the links from  the transition nodes connect 
		// Import to Backwards and don't go straight to END
		assertGraph("[0:START][1:Import:timestamp][2:T2:timestamp][3:T1:timestamp][4:Backwards:timestamp][5:END][0-1][Error2:1-2][Error:1-3][1-4][2-4][3-4][4-5]",
			"<Import: timestamp 'Error2'->T2: timestamp 'Error'->T1: timestamp> && Backwards: timestamp");
		graph = ctn.toGraph();
		assertThat(graph.toDSLText()).isEqualTo("<Import: timestamp 'Error2'->T2: timestamp 'Error'->T1: timestamp> && Backwards: timestamp");
		
		// This is the variant of the above without the <...>
		// Now notice the links from the transition nodes go direct to END
		ctn = parse("Import: timestamp 'Error2'->T2: timestamp 'Error'->T1: timestamp && Backwards: timestamp");
		assertGraph("[0:START][1:Import:timestamp][2:T2:timestamp][3:T1:timestamp][4:Backwards:timestamp][5:END][0-1][Error2:1-2][Error:1-3][1-4][4-5][2-5][3-5]",
			"Import: timestamp 'Error2'->T2: timestamp 'Error'->T1: timestamp && Backwards: timestamp");
		graph = ctn.toGraph();
		assertThat(graph.toDSLText()).isEqualTo("Import: timestamp 'Error2'->T2: timestamp 'Error'->T1: timestamp && Backwards: timestamp");
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
			fail("");
		}
		catch (CheckPointedParseException cppe) {
			assertThat(cppe.message).isEqualTo(DSLMessage.TASK_ARGUMENTS_NOT_ALLOWED_UNLESS_IN_APP_MODE);
		}
		try {
			new TaskParser("foo", "appA --p1=v1", true, true).parse();
		}
		catch (CheckPointedParseException cppe) {
			fail("");
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
		assertThat(parse(spec).toDSL()).isEqualTo(spec);
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
		assertThat(parse("A&&B").stringify()).isEqualTo("A && B");
		assertThat(parse("<A||B>").stringify()).isEqualTo("<A || B>");
		assertThat(parse("<A&&B||C>").stringify()).isEqualTo("<A && B || C>");
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
		assertThat(ctn.stringify()).isEqualTo("<jobA && jobB && jobC || boo: jobC>");
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
		assertThat(sequences.get(0).getLabelString()).isEqualTo("aaaa");
		ctn = parse("aaaa: foo && bar");
		sequences = ctn.getSequences();
		assertThat(sequences.get(0).getLabelString()).isEqualTo("aaaa");
	}

	@Test
	public void nestedSplit1() {
		TaskNode ctn = parse("<<jobA || jobB> || jobC>");
		assertThat(ctn.stringify()).isEqualTo("<<jobA || jobB> || jobC>");
		LabelledTaskNode start = ctn.getStart();
		assertInstanceOf(FlowNode.class, start);
		SplitNode split = (SplitNode) start.getSeriesElement(0);
		LabelledTaskNode seriesElement = split.getSeriesElement(0).getSeriesElement(0);
		assertInstanceOf(SplitNode.class, seriesElement);
		SplitNode split2 = (SplitNode) seriesElement;
		assertThat(split2.getSeriesLength()).isEqualTo(2);
	}

	@Test
	public void nestedSplit2() {
		TaskNode ctn = parse("<jobA || <jobB || jobC> || jobD>");
		assertThat(ctn.stringify()).isEqualTo("<jobA || <jobB || jobC> || jobD>");
		LabelledTaskNode start = ctn.getStart();
		assertThat(start.isFlow()).isTrue();
		SplitNode split = (SplitNode) start.getSeriesElement(0);
		assertThat(split.getSeriesLength()).isEqualTo(3);
		LabelledTaskNode seriesElement = split.getSeriesElement(1);
		SplitNode splitSeriesElement = (SplitNode) seriesElement.getSeriesElement(0);
		assertThat(splitSeriesElement.isSplit()).isTrue();
		assertThat(splitSeriesElement.getSeriesLength()).isEqualTo(2);
		assertThat(splitSeriesElement.stringify()).isEqualTo("<jobB || jobC>");
		assertThat(((TaskAppNode) splitSeriesElement.getSeriesElement(0).getSeriesElement(0)).getName()).isEqualTo("jobB");
	}

	@Test
	public void singleTransition() {
		TaskNode ctn = parse("foo 'completed'->bar");
		LabelledTaskNode start = ctn.getStart();
		start = start.getSeriesElement(0);
		assertInstanceOf(TaskAppNode.class, start);
		TaskAppNode ta = (TaskAppNode) start;
		List<TransitionNode> transitions = ta.getTransitions();
		assertThat(transitions.size()).isEqualTo(1);
		assertThat(transitions.get(0).getStatusToCheck()).isEqualTo("completed");
		assertThat(transitions.get(0).getTargetApp().getName()).isEqualTo("bar");
	}

	@Test
	public void doubleTransition() {
		TaskNode ctn = parse("foo 'completed'->bar 'wibble'->wobble");
		LabelledTaskNode start = ctn.getStart();
		assertFlow(start, "foo");
		TaskAppNode ta = (TaskAppNode) start.getSeriesElement(0);
		List<TransitionNode> transitions = ta.getTransitions();
		assertThat(transitions.size()).isEqualTo(2);
		assertThat(transitions.get(0).getStatusToCheck()).isEqualTo("completed");
		assertThat(transitions.get(0).getTargetApp().getName()).isEqualTo("bar");
		assertThat(transitions.get(1).getStatusToCheck()).isEqualTo("wibble");
		assertThat(transitions.get(1).getTargetApp().getName()).isEqualTo("wobble");
	}

	@Test
	public void moreSophisticatedScenarios_gh712_1a() {
		TaskNode ctn = parse(
				"<<jdbchdfs-local && spark-client || spark-cluster && two: spark-cluster> && timestamp || spark-yarn>");

		// Check it looks like the picture:
		// https://user-images.githubusercontent.com/1562654/38313990-27662f60-37da-11e8-9106-26688d631fae.png
		LabelledTaskNode start = ctn.getStart();
		FlowNode f1 = (FlowNode) start;
		assertThat(f1.getSeriesLength()).isEqualTo(1);
		SplitNode s1 = (SplitNode) f1.getSeriesElement(0);
		assertThat(s1.getSeriesLength()).isEqualTo(2);
		// This one is just spark-yarn
		assertFlow(s1.getSeriesElement(1), "spark-yarn");

		// This one is a flow of a split of jdbchdfs-local/spark-client and
		// spark-cluster/spark-cluster and then timestamp
		FlowNode f2 = (FlowNode) s1.getSeriesElement(0);
		assertThat(f2.getSeriesLength()).isEqualTo(2);
		assertThat(((TaskAppNode) f2.getSeriesElement(1)).getName()).isEqualTo("timestamp");

		SplitNode s2 = (SplitNode) f2.getSeriesElement(0);
		assertThat(s2.getSeriesLength()).isEqualTo(2);
		FlowNode s2fa = (FlowNode) s2.getSeriesElement(0);
		FlowNode s2fb = (FlowNode) s2.getSeriesElement(1);
		assertFlow(s2fa, "jdbchdfs-local", "spark-client");
		assertFlow(s2fb, "spark-cluster", "spark-cluster");

		Graph graph = ctn.toGraph();
		assertThat(graph.toVerboseString()).isEqualTo("[0:START][1:jdbchdfs-local][2:spark-client][3:spark-cluster][4:two:spark-cluster][5:timestamp][6:spark-yarn][7:END]" +
		"[0-1][1-2][0-3][3-4][2-5][4-5][0-6][5-7][6-7]");

		assertThat(graph.toDSLText()).isEqualTo("<<jdbchdfs-local && spark-client || spark-cluster && two: spark-cluster> && timestamp || spark-yarn>");
	}

	@Test
	public void moreSophisticatedScenarios_gh712_1b() {
		TaskNode ctn = parse("<<AA || BB> && CC || DD>");
		Graph graph = ctn.toGraph();
		assertThat(graph.toVerboseString()).isEqualTo("[0:START][1:AA][2:BB][3:CC][4:DD][5:END]" +
		"[0-1][0-2][1-3][2-3][0-4][3-5][4-5]");
		assertThat(graph.toDSLText()).isEqualTo("<<AA || BB> && CC || DD>");
	}

	@Test
	public void moreSophisticatedScenarios_gh712_1c() {
		TaskNode ctn = parse("<<AA || BB> && CC && DD || EE>");
		Graph graph = ctn.toGraph();
		assertThat(graph.toVerboseString()).isEqualTo("[0:START][1:AA][2:BB][3:CC][4:DD][5:EE][6:END]" +
		"[0-1][0-2][1-3][2-3][3-4][0-5][4-6][5-6]");
		assertThat(graph.toDSLText()).isEqualTo("<<AA || BB> && CC && DD || EE>");
		ctn = parse("<<AA || BB> && CC && DD || EE>");
		assertThat(ctn.toGraph().toDSLText()).isEqualTo("<<AA || BB> && CC && DD || EE>");
	}

	@Test
	public void moreSophisticatedScenarios_gh712_1d() {
		TaskNode ctn = parse("<<AC && AD || AE && AF> && AG || AB>");
		assertThat(ctn.toGraph().toDSLText()).isEqualTo("<<AC && AD || AE && AF> && AG || AB>");
		// Now include a transition
		ctn = parse("<<AC && AD || AE 'jumpOut'-> AH && AF> && AG || AB>");
		Graph graph = ctn.toGraph();
		assertThat(graph.toVerboseString()).isEqualTo("[0:START][1:AC][2:AD][3:AE][4:AH][5:AF][6:AG][7:AB][8:END]" +
		"[0-1][1-2][0-3][jumpOut:3-4][3-5][2-6][5-6][4-6][0-7][6-8][7-8]");
		// Key thing to observe above is the link from [4-6] which goes from
		// the transition target AH to the end of the split AG
		assertThat(graph.toDSLText()).isEqualTo("<<AC && AD || AE 'jumpOut'->AH && AF> && AG || AB>");
	}

	@Test
	public void moreSophisticatedScenarios_gh712_1e() {
		TaskNode ctn = parse("<<AA || BB> && CC && DD || <EE || FF> && GG || HH>");
		Graph graph = ctn.toGraph();
		assertThat(graph.toVerboseString()).isEqualTo("[0:START][1:AA][2:BB][3:CC][4:DD][5:EE][6:FF][7:GG][8:HH][9:END]" +
		"[0-1][0-2][1-3][2-3][3-4][0-5][0-6][5-7][6-7][0-8][4-9][7-9][8-9]");
		assertThat(graph.toDSLText()).isEqualTo("<<AA || BB> && CC && DD || <EE || FF> && GG || HH>");
	}

	@Test
	public void moreSophisticatedScenarios_gh712_1f() {
		// Multiple nested splits in parallel
		TaskNode ctn = parse("<<AA || BB> && CC || <DD || EE> && FF && GG || HH>");
		Graph graph = ctn.toGraph();
		assertThat(graph.toVerboseString()).isEqualTo("[0:START][1:AA][2:BB][3:CC][4:DD][5:EE][6:FF][7:GG][8:HH][9:END]" +
		"[0-1][0-2][1-3][2-3][0-4][0-5][4-6][5-6][6-7][0-8][3-9][7-9][8-9]");
		assertThat(graph.toDSLText()).isEqualTo("<<AA || BB> && CC || <DD || EE> && FF && GG || HH>");
	}

	// Case2: expecting a validation error on the parse because the second spark-cluster
	// isn't labeled
	@Test
	public void moreSophisticatedScenarios_gh712_2() {
		try {
			parse("<<jdbchdfs-local && spark-client || spark-cluster && spark-cluster> && timestamp || spark-yarn>");
			fail("");
		}
		catch (TaskValidationException tve) {
			List<TaskValidationProblem> validationProblems = tve.getValidationProblems();
			assertThat(validationProblems.size()).isEqualTo(1);
			TaskValidationProblem tvp = validationProblems.get(0);
			assertThat(tvp.getOffset()).isEqualTo(53);
			assertThat(tvp.getMessage()).isEqualTo(DSLMessage.TASK_VALIDATION_APP_NAME_ALREADY_IN_USE);
		}
	}

	// Case3: no graph when 1 label included?
	@Test
	public void moreSophisticatedScenarios_gh712_3() {
		try {
			parse("<1: jdbchdfs-local && spark-client && timestamp || spark-cluster && spark-cluster && timestamp || spark-yarn>");
			fail("");
		}
		catch (TaskValidationException tve) {
			System.out.println(tve);
			List<TaskValidationProblem> validationProblems = tve.getValidationProblems();
			assertThat(validationProblems.size()).isEqualTo(2);
			TaskValidationProblem tvp = validationProblems.get(0);
			assertThat(tvp.getOffset()).isEqualTo(68);
			assertThat(tvp.getMessage()).isEqualTo(DSLMessage.TASK_VALIDATION_APP_NAME_ALREADY_IN_USE);
			tvp = validationProblems.get(1);
			assertThat(tvp.getOffset()).isEqualTo(85);
			assertThat(tvp.getMessage()).isEqualTo(DSLMessage.TASK_VALIDATION_APP_NAME_ALREADY_IN_USE);
		}
	}

	@Test
	public void wildcardTransition() {
		ctn = parse("foo '*'->wibble");
		assertThat(ctn.toDSL()).isEqualTo("foo '*'->wibble");
		ctn = parse("foo \"*\"->wibble");
		assertThat(ctn.toDSL()).isEqualTo("foo \"*\"->wibble");
	}

	@Test
	public void splitWithTransition() {
		String spec = "<foo 'completed'->kill || bar>";
		ctn = parse(spec);
		assertThat(ctn.toDSL()).isEqualTo(spec);
	}

	@Test
	public void multiLine() {
		TaskNode ctn = parse("<foo\n" + "  'completed'->kill\n" + "  '*'->custard\n" + "  || bar>");
		assertThat(ctn.stringify()).isEqualTo("<foo 'completed'->kill '*'->custard || bar>");
	}

	@Test
	public void emptyInput() {
		checkForParseError("", DSLMessage.OOD, 0);
	}

	@Test
	public void toGraph$END() {
		TaskNode ctn = parse("foo 'oranges'->$END");
		assertThat(ctn.toDSL()).isEqualTo("foo 'oranges'->$END");
		assertGraph("[0:START][1:foo][2:$END][3:END][0-1][oranges:1-2][1-3]", "foo 'oranges'->$END");
		checkDSLToGraphAndBackToDSL("foo 'oranges'->$END");
	}

	@Test
	public void toGraph$FAIL() {
		String spec = "foo 'oranges'->$FAIL";
		assertThat(parse(spec).toDSL()).isEqualTo(spec);
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
		assertThat(graph.toDSLText()).isEqualTo("aaa --one=bar --two='b ar'");

		graph.nodes.add(new Node("3", "bbb"));
		graph.links.add(new Link("1", "3", "tname"));
		assertThat(graph.toDSLText()).isEqualTo("aaa --one=bar --two='b ar' 'tname'->bbb");

		graph.nodes.add(new Node("4", "ccc"));
		graph.links.add(new Link("1", "4", "*"));
		assertThat(graph.toDSLText()).isEqualTo("aaa --one=bar --two='b ar' 'tname'->bbb '*'->ccc");

		graph.nodes.add(new Node("5", "ddd"));
		graph.links.add(new Link("1", "5", "3"));
		assertThat(graph.toDSLText()).isEqualTo("aaa --one=bar --two='b ar' 'tname'->bbb '*'->ccc 3->ddd");

		// When going from DSL to graph, unquote property values and exit codes

		String dsl = "aaa --one=bar --two='b ar' 'tname'->bbb '*'->ccc 3->ddd";
		graph = parse(dsl).toGraph();
		n = graph.nodes.get(1);
		assertThat(n.properties.get("two")).isEqualTo("b ar");
		Link l = graph.links.get(1);
		assertThat(l.getTransitionName()).isEqualTo("tname");
		l = graph.links.get(2);
		assertThat(l.getTransitionName()).isEqualTo("*");
		l = graph.links.get(3);
		assertThat(l.getTransitionName()).isEqualTo("3");
		assertThat(graph.toDSLText()).isEqualTo(dsl);
	}

	@Test
	public void wildcardTransitions() {
		// When going from DSL to graph, unquote property values and exit codes
		String dsl = "aaa 'tname'->bbb '*'->ccc 3->ddd";
		assertGraph("[0:START][1:aaa][2:bbb][3:ccc][4:ddd][5:END][0-1][tname:1-2][*:1-3][3:1-4][1-5][2-5][3-5][4-5]",
				dsl);
		Graph graph = parse(dsl).toGraph();
		Link l = graph.links.get(1);
		assertThat(l.getTransitionName()).isEqualTo("tname");
		l = graph.links.get(2);
		assertThat(l.getTransitionName()).isEqualTo("*");
		l = graph.links.get(3);
		assertThat(l.getTransitionName()).isEqualTo("3");
		assertThat(graph.toDSLText()).isEqualTo(dsl);
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
		assertThat(t.kind).isEqualTo(kind);
		assertThat(t.getKind().hasPayload() ? t.stringValue() : new String(t.getKind().getTokenChars())).isEqualTo(string);
		assertThat(t.startPos).isEqualTo(start);
		assertThat(t.endPos).isEqualTo(end);
	}

	private void assertTokens(Tokens tokens, TokenKind... expectedKinds) {
		for (int i = 0; i < expectedKinds.length; i++) {
			assertThat(tokens.next().getKind()).isEqualTo(expectedKinds[i]);
		}
	}

	private void assertTaskApp(LabelledTaskNode node, String taskAppName) {
		assertThat(node.isTaskApp()).isTrue();
		assertThat(taskAppName).isEqualTo(((TaskAppNode) node).getName());
	}

	private void assertFlow(LabelledTaskNode node, String... expectedApps) {
		assertInstanceOf(FlowNode.class, node);
		FlowNode flow = (FlowNode) node;
		List<LabelledTaskNode> series = flow.getSeries();
		assertThat(series.size()).isEqualTo(expectedApps.length);
		assertThat(flow.getSeriesLength()).isEqualTo(expectedApps.length);
		for (int a = 0; a < expectedApps.length; a++) {
			assertTaskApp(series.get(a), expectedApps[a]);
		}
	}

	private void assertSplit(LabelledTaskNode node, String... expectedApps) {
		assertInstanceOf(SplitNode.class, node);
		SplitNode split = (SplitNode) node;
		List<LabelledTaskNode> series = split.getSeries();
		assertThat(series.size()).isEqualTo(expectedApps.length);
		assertThat(split.getSeriesLength()).isEqualTo(expectedApps.length);
		for (int a = 0; a < expectedApps.length; a++) {
			FlowNode f = (FlowNode) series.get(a);
			assertThat(f.getSeriesLength()).isEqualTo(1);
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
			assertThat(e.getMessageCode()).isEqualTo(msg);
			assertThat(e.getPosition()).isEqualTo(pos);
			if (inserts != null) {
				for (int i = 0; i < inserts.length; i++) {
					assertThat(e.getInserts()[i]).isEqualTo(inserts[i]);
				}
			}
			return e;
		}
	}

	private void assertApps(List<TaskApp> taskApps, String... expectedTaskAppNames) {

		assertThat(taskApps.size()).as("Expected " + expectedTaskAppNames.length + " but was " + taskApps.size() + ": " + taskApps).isEqualTo(expectedTaskAppNames.length);
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
		assertThat(graph.toDSLText()).isEqualTo(specification);
	}

	private void assertGraph(String expectedGraph, String dsl) {
		TaskNode ctn = parse(dsl);
		Graph graph = ctn.toGraph();
		assertThat(graph.toVerboseString()).isEqualTo(expectedGraph);
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
			assertThat(expectedTaskApp).isEqualTo(s.toString());
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
