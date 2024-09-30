/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.cloud.dataflow.core.dsl.tck;

import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.dsl.AppNode;
import org.springframework.cloud.dataflow.core.dsl.ArgumentNode;
import org.springframework.cloud.dataflow.core.dsl.DSLMessage;
import org.springframework.cloud.dataflow.core.dsl.ParseException;
import org.springframework.cloud.dataflow.core.dsl.SourceDestinationNode;
import org.springframework.cloud.dataflow.core.dsl.StreamNode;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractStreamDslTests {

	protected abstract StreamNode parse(String streamDefinition);

	protected abstract StreamNode parse(String streamName, String streamDefinition);

	@Test
	public void oneApp() {
		StreamNode sn = parse("foo");
		assertThat(sn.getAppNodes()).hasSize(1);
		AppNode appNode = sn.getApp("foo");
		assertThat(appNode.getName()).isEqualTo("foo");
		assertThat(appNode.getArguments()).hasSize(0);
		assertThat(appNode.getStartPos()).isEqualTo(0);
		assertThat(appNode.getEndPos()).isEqualTo(3);
	}

	@Test
	public void hyphenatedAppName() {
		StreamNode sn = parse("gemfire-cq");
		sn = parse("gemfire-cq");
		assertThat(sn.stringify(true)).isEqualTo("[(AppNode:gemfire-cq:0>10)]");
	}

	@Test
	public void listApps() {
		checkForParseError(":aaa > fff||bbb", DSLMessage.DONT_USE_DOUBLEPIPE_WITH_CHANNELS, 10);
		checkForParseError("fff||bbb > :zzz", DSLMessage.DONT_USE_DOUBLEPIPE_WITH_CHANNELS, 3);
		checkForParseError("aaa | bbb|| ccc", DSLMessage.DONT_MIX_PIPE_AND_DOUBLEPIPE, 9);
		checkForParseError("aaa || bbb| ccc", DSLMessage.DONT_MIX_PIPE_AND_DOUBLEPIPE, 10);
		StreamNode sn = parse("aaa | filter --expression=#jsonPath(payload,'$.lang')=='en'");
		assertThat("--expression=#jsonPath(payload,'$.lang')=='en'")
				.isEqualTo(sn.getAppNodes().get(1).getArguments()[0].toString());
	}

	@Test
	public void doublePipeEndingArgs() {
		checkForParseError("aaa --bbb=ccc||", DSLMessage.OOD, 15);
		StreamNode sn = parse("aaa --bbb=ccc,");
		assertThat("[(AppNode:aaa --bbb=ccc,)]").isEqualTo(sn.stringify(false));
		checkForParseError("aaa --bbb='ccc'||", DSLMessage.OOD, 17);
		sn = parse("aaa --bbb='ccc'|| bbb");
		assertThat("[(AppNode:aaa --bbb=ccc:0>15)(AppNode:bbb:18>21)]").isEqualTo(sn.stringify(true));
		ArgumentNode argumentNode = sn.getAppNodes().get(0).getArguments()[0];
		assertThat("ccc").isEqualTo(argumentNode.getValue());
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
		checkForParseError("aaa --bbb=\n --ccc=ddd", DSLMessage.EXPECTED_ARGUMENT_VALUE, 10);
	}

	// Just to make the testing easier the parser supports stream naming easier.
	@Test
	public void streamNaming() {
		StreamNode sn = parse("mystream = foo");
		assertThat("[mystream = (AppNode:foo:11>14)]").isEqualTo(sn.stringify(true));
		assertThat("mystream").isEqualTo(sn.getName());
	}

	@Test
	public void streamNameAsAppName() {
		String streamName = "bar";
		String stream = "bar = foo | bar";
		StreamNode sn = parse(stream);
		assertThat(streamName).isEqualTo(sn.getName());
	}

	// Pipes are used to connect apps
	@Test
	public void twoApps() {
		StreamNode ast = parse("foo | bar");
		assertThat("[(AppNode:foo:0>3)(AppNode:bar:6>9)]").isEqualTo(ast.stringify(true));
	}

	// Apps can be labeled
	@Test
	public void appLabels() {
		StreamNode ast = parse("label: http");
		assertThat("[((Label:label:0>5) AppNode:http:0>11)]").isEqualTo(ast.stringify(true));
	}

	@Test
	public void appLabels3() {
		StreamNode ast = parse("food = http | label3: foo");
		assertThat("[food = (AppNode:http:7>11)((Label:label3:14>20) AppNode:foo:14>25)]").isEqualTo(ast.stringify(true));

		StreamNode sn = parse("http | foo: bar | file");
		assertThat("[(AppNode:http)((Label:foo) AppNode:bar)(AppNode:file)]").isEqualTo(sn.stringify());

		checkForParseError("http | foo: goggle: bar | file", DSLMessage.NO_DOUBLE_LABELS, 12);
		checkForParseError("http | foo :bar | file", DSLMessage.UNEXPECTED_DATA_AFTER_STREAMDEF, 11);
	}

	// Apps can take parameters
	@Test
	public void oneAppWithParam() {
		StreamNode ast = parse("foo --name=value");
		assertThat("[(AppNode:foo --name=value:0>16)]").isEqualTo(ast.stringify(true));
	}

	// Apps can take two parameters
	@Test
	public void oneAppWithTwoParams() {
		StreamNode sn = parse("foo --name=value --x=y");
		List<AppNode> appNodes = sn.getAppNodes();
		assertThat(1).isEqualTo(appNodes.size());

		AppNode mn = appNodes.get(0);
		assertThat("foo").isEqualTo(mn.getName());
		ArgumentNode[] args = mn.getArguments();
		assertThat(args).isNotNull();
		assertThat(2).isEqualTo(args.length);
		assertThat("name").isEqualTo(args[0].getName());
		assertThat("value").isEqualTo(args[0].getValue());
		assertThat("x").isEqualTo(args[1].getName());
		assertThat("y").isEqualTo(args[1].getValue());
		assertThat("[(AppNode:foo --name=value --x=y:0>22)]").isEqualTo(sn.stringify(true));
	}

	@Test
	public void parameters() {
		String app = "gemfire-cq --query='Select * from /Stocks where symbol=''VMW''' --regionName=foo --foo=bar";
		StreamNode ast = parse(app);
		AppNode gemfireApp = ast.getApp("gemfire-cq");
		Properties parameters = gemfireApp.getArgumentsAsProperties();
		assertThat(3).isEqualTo(parameters.size());
		assertThat("Select * from /Stocks where symbol='VMW'").isEqualTo(parameters.get("query"));
		assertThat("foo").isEqualTo(parameters.get("regionName"));
		assertThat("bar").isEqualTo(parameters.get("foo"));

		app = "test";
		parameters = parse(app).getApp("test").getArgumentsAsProperties();
		assertThat(0).isEqualTo(parameters.size());

		app = "foo --x=1 --y=two ";
		parameters = parse(app).getApp("foo").getArgumentsAsProperties();
		assertThat(2).isEqualTo(parameters.size());
		assertThat("1").isEqualTo(parameters.get("x"));
		assertThat("two").isEqualTo(parameters.get("y"));

		app = "foo --x=1a2b --y=two ";
		parameters = parse(app).getApp("foo").getArgumentsAsProperties();
		assertThat(2).isEqualTo(parameters.size());
		assertThat("1a2b").isEqualTo(parameters.get("x"));
		assertThat("two").isEqualTo(parameters.get("y"));

		app = "foo --x=2";
		parameters = parse(app).getApp("foo").getArgumentsAsProperties();
		assertThat(1).isEqualTo(parameters.size());
		assertThat("2").isEqualTo(parameters.get("x"));

		app = "--foo = bar";
		try {
			parse(app);
			throw new AssertionError(app + " is invalid. Should throw exception");
		}
		catch (Exception e) {
			// success
		}
	}

	@Test
	public void invalidApps() {
		String config = "test | foo--x=13";
		try {
			parse("t", config);
			throw new AssertionError(config + " is invalid. Should throw exception");
		}
		catch (Exception e) {
			// success
		}
	}

	@Test
	public void tapWithLabelReference() {
		parse("mystream = http | filter | group1: transform | group2: transform | file");
		StreamNode ast = parse(":mystream.group1 > file");

		assertThat("[(mystream.group1)>(AppNode:file)]").isEqualTo(ast.stringify());
		ast = parse(":mystream.group2 > file");
		assertThat("[(mystream.group2)>(AppNode:file)]").isEqualTo(ast.stringify());
	}

	@Test
	public void tapWithQualifiedAppReference() {
		parse("mystream = http | foobar | file");
		StreamNode sn = parse(":mystream.foobar > file");
		assertThat("[(mystream.foobar:1>16)>(AppNode:file:19>23)]").isEqualTo(sn.stringify(true));
	}

	@Test
	public void expressions_xd159() {
		StreamNode ast = parse("foo | transform --expression=--payload | bar");
		AppNode mn = ast.getApp("transform");
		Properties props = mn.getArgumentsAsProperties();
		assertThat("--payload").isEqualTo(props.get("expression"));
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
	public void parametersContainingNewlineCarriageReturn() {
		StreamNode ast = parse(":producer > foobar --expression='aaa=bbb \n ccc=ddd' > :consumer");
		assertThat("aaa=bbb \n ccc=ddd").isEqualTo(ast.getApp("foobar").getArguments()[0].getValue());
		ast = parse(":producer > foobar --expression='aaa=bbb \r ccc=ddd' > :consumer");
		assertThat("aaa=bbb \r ccc=ddd").isEqualTo(ast.getApp("foobar").getArguments()[0].getValue());
	}

	@Test
	public void expressions_xd159_3() {
		StreamNode ast = parse("foo |  transform --expression='new StringBuilder(payload).reverse()' | bar");
		AppNode mn = ast.getApp("transform");
		Properties props = mn.getArgumentsAsProperties();
		assertThat("new StringBuilder(payload).reverse()").isEqualTo(props.get("expression"));
	}

	@Test
	public void unbalancedSingleQuotes() {
		checkForParseError("foo | bar --expression='select foo", DSLMessage.NON_TERMINATING_QUOTED_STRING, 23);
	}

	@Test
	public void unbalancedDoubleQuotes() {
		checkForParseError("foo | bar --expression=\"select foo", DSLMessage.NON_TERMINATING_DOUBLE_QUOTED_STRING, 23);
	}

	@Test
	public void appArguments_xd1613() {
		StreamNode ast = null;

		// notice no space between the ' and final >
		ast = parse(":producer > transform --expression='payload.toUpperCase()' | filter --expression='payload.length"
				+ "() > 4'> :consumer");
		assertThat("payload.toUpperCase()").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());
		assertThat("payload.length() > 4").isEqualTo(ast.getApp("filter").getArguments()[0].getValue());

		ast = parse("time | transform --expression='T(org.joda.time.format.DateTimeFormat).forPattern(\"yyyy-MM-dd "
				+ "HH:mm:ss\").parseDateTime(payload)'");
		assertThat(
				"T(org.joda.time.format.DateTimeFormat).forPattern(\"yyyy-MM-dd HH:mm:ss\").parseDateTime(payload)")
				.isEqualTo(ast.getApp("transform").getArguments()[0].getValue());

		// allow for pipe/semicolon if quoted
		ast = parse("http | transform --outputType='text/plain|charset=UTF-8'  | log");
		assertThat("text/plain|charset=UTF-8").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());

		ast = parse("http | transform --outputType='text/plain;charset=UTF-8'  | log");
		assertThat("text/plain;charset=UTF-8").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());

		// Want to treat all of 'hi'+payload as the argument value
		ast = parse("http | transform --expression='hi'+payload | log");
		assertThat("'hi'+payload").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());

		// Want to treat all of payload+'hi' as the argument value
		ast = parse("http | transform --expression=payload+'hi' | log");
		assertThat("payload+'hi'").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());

		// Alternatively, can quote all around it to achieve the same thing
		ast = parse("http | transform --expression='payload+''hi''' | log");
		assertThat("payload+'hi'").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());
		ast = parse("http | transform --expression='''hi''+payload' | log");
		assertThat("'hi'+payload").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());

		ast = parse("http | transform --expression=\"payload+'hi'\" | log");
		assertThat("payload+'hi'").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());
		ast = parse("http | transform --expression=\"'hi'+payload\" | log");
		assertThat("'hi'+payload").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());

		ast = parse("http | transform --expression=payload+'hi'--param2='foobar' | log");
		assertThat("payload+'hi'--param2='foobar'").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());

		ast = parse("http | transform --expression='hi'+payload--param2='foobar' | log");
		assertThat("'hi'+payload--param2='foobar'").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());

		// This also works, which is cool
		ast = parse("http | transform --expression='hi'+'world' | log");
		assertThat("'hi'+'world'").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());
		ast = parse("http | transform --expression=\"'hi'+'world'\" | log");
		assertThat("'hi'+'world'").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());

		ast = parse("http | filter --expression=payload.matches('hello world') | log");
		assertThat("payload.matches('hello world')").isEqualTo(ast.getApp("filter").getArguments()[0].getValue());

		ast = parse("http | transform --expression='''hi''' | log");
		assertThat("'hi'").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());

		ast = parse("http | transform --expression=\"''''hi''''\" | log");
		assertThat("''''hi''''").isEqualTo(ast.getApp("transform").getArguments()[0].getValue());
	}

	@Test
	public void expressions_xd159_4() {
		StreamNode ast = parse("foo |  transform --expression=\"'Hello, world!'\" | bar");
		AppNode mn = ast.getApp("transform");
		Properties props = mn.getArgumentsAsProperties();
		assertThat("'Hello, world!'").isEqualTo(props.get("expression"));
		ast = parse("foo |  transform --expression='''Hello, world!''' | bar");
		mn = ast.getApp("transform");
		props = mn.getArgumentsAsProperties();
		assertThat("'Hello, world!'").isEqualTo(props.get("expression"));
		// Prior to the change for XD-1613, this error should point to the comma:
		// checkForParseError("foo | transform --expression=''Hello, world!'' | bar",
		// DSLMessage.UNEXPECTED_DATA,
		// 37);
		// but now it points to the !
		checkForParseError("foo |  transform --expression=''Hello, world!'' | bar", DSLMessage.UNEXPECTED_DATA, 44);
	}

	@Test
	public void expressions_gh1() {
		StreamNode ast = parse("http --port=9014 | filter --expression=\"payload == 'foo'\" | log");
		AppNode mn = ast.getApp("filter");
		Properties props = mn.getArgumentsAsProperties();
		assertThat("payload == 'foo'").isEqualTo(props.get("expression"));
	}

	@Test
	public void expressions_gh1_2() {
		StreamNode ast = parse("http --port=9014 | filter --expression='new Foo()' | log");
		AppNode mn = ast.getApp("filter");
		Properties props = mn.getArgumentsAsProperties();
		assertThat("new Foo()").isEqualTo(props.get("expression"));
	}

	@Test
	public void sourceDestination() {
		StreamNode sn = parse(":foobar > file");
		assertThat("[(foobar:1>7)>(AppNode:file:10>14)]").isEqualTo(sn.stringify(true));
    }

    @Test
	public void sourceDestinationsWithExtraWildcards() {
		StreamNode sn = parse(":a/ > file");
		assertThat("[(a/:1>3)>(AppNode:file:6>10)]").isEqualTo(sn.stringify(true));
		sn = parse(":a/*# > file");
		assertThat("[(a/*#:1>5)>(AppNode:file:8>12)]").isEqualTo(sn.stringify(true));
		sn = parse(":foo.* > file");
		assertThat("[(foo.*:1>6)>(AppNode:file:9>13)]").isEqualTo(sn.stringify(true));
		sn = parse(":*foo > file");
		assertThat("[(*foo:1>5)>(AppNode:file:8>12)]").isEqualTo(sn.stringify(true));
	}

	@Test
	public void sinkDestination() {
		StreamNode sn = parse("http > :foo");
		assertThat("[(AppNode:http:0>4)>(foo:8>11)]").isEqualTo(sn.stringify(true));
    }

    @Test
    public void sinkDestinationsWithExtraWildcards() {
		StreamNode sn = parse("http > :foo/");
		assertThat("[(AppNode:http:0>4)>(foo/:8>12)]").isEqualTo(sn.stringify(true));
		sn = parse("http > :foo/*#");
		assertThat("[(AppNode:http:0>4)>(foo/*#:8>14)]").isEqualTo(sn.stringify(true));
		sn = parse("http > :foo.*");
		assertThat("[(AppNode:http:0>4)>(foo.*:8>13)]").isEqualTo(sn.stringify(true));
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

		StreamNode sn = parse("wibble: http > :bar");
		assertThat("[((Label:wibble) AppNode:http)>(bar)]").isEqualTo(sn.stringify());
	}

	@Test
	public void sourceDestination2() {
		parse("foo = http | bar | file");
		StreamNode ast = parse(":foo.bar > file");
		assertThat("[(foo.bar:1>8)>(AppNode:file:11>15)]").isEqualTo(ast.stringify(true));
		assertThat("foo.bar").isEqualTo(ast.getSourceDestinationNode().getDestinationName());
	}

	@Test
	public void sourceTapDestination() {
		parse("mystream = http | file");
		StreamNode ast = parse(":mystream.http > file");
		assertThat("[(mystream.http:1>14)>(AppNode:file:17>21)]").isEqualTo(ast.stringify(true));
		SourceDestinationNode sourceDestinationNode = ast.getSourceDestinationNode();
		assertThat("mystream.http").isEqualTo(sourceDestinationNode.getDestinationName());
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
		checkForParseError(":foo > (", DSLMessage.UNEXPECTED_DATA, 7, "(");
		checkForParseError(":foo > *", DSLMessage.EXPECTED_APPNAME, 7, "*");
		checkForParseError("::foo > *", DSLMessage.UNEXPECTED_DATA_IN_DESTINATION_NAME, 1, ":");
		checkForParseError(":foo > :", DSLMessage.OOD, 7);
	}

	@Test
	public void errorCases08() {
		checkForParseError(":foo | bar", DSLMessage.EXPECTED_APPNAME, 0, ":");
	}

	@Test
	public void errorCases09() {
		checkForParseError("( = http | file", DSLMessage.UNEXPECTED_DATA, 0, "(");
		checkForParseError("* = http | file", DSLMessage.ILLEGAL_STREAM_NAME, 0, "*");
		checkForParseError(": = http | file", DSLMessage.ILLEGAL_STREAM_NAME, 0, ":");
	}

	@Test
	public void duplicateExplicitLabels() {
		checkForParseError("xxx: http | xxx: file", DSLMessage.DUPLICATE_LABEL, 12, "xxx", "http", 0, "file", 1);
		checkForParseError("xxx: http | yyy: filter | transform | xxx: transform | file", DSLMessage.DUPLICATE_LABEL,
				38, "xxx", "http", 0, "transform", 3);
		checkForParseError("xxx: http | yyy: filter | transform | xxx: transform | xxx: file",
				DSLMessage.DUPLICATE_LABEL, 38, "xxx", "http", 0, "transform", 3);
	}

	@Test
	public void addingALabelLiftsAmbiguity() {
		StreamNode ast = parse("file | out: file");
		assertThat("file").isEqualTo(ast.getAppNodes().get(0).getLabelName());
		assertThat("out").isEqualTo(ast.getAppNodes().get(1).getLabelName());
	}

	@Test
	public void duplicateImplicitLabels() {
		checkForParseError("http | filter | transform | transform | file", DSLMessage.DUPLICATE_LABEL, 28, "transform",
				"transform", 2, "transform", 3);
	}

	@Test
	public void tapWithLabels() {
		parse("mystream = http | flibble: transform | file");
		StreamNode sn = parse(":mystream.flibble > file");
		assertThat("mystream.flibble").isEqualTo(sn.getSourceDestinationNode().getDestinationName());
	}

	@Test
	public void bridge01() {
		StreamNode sn = parse(":bar > :boo");
		assertThat("[(bar:1>4)>(AppNode:bridge:5>6)>(boo:8>11)]").isEqualTo(sn.stringify(true));
	}

	@Test
	public void sourceDestinationArgs() {
		StreamNode sn = parse(":test --group=test > file");
		assertThat("[(test:1>5 --group=test)>(AppNode:file:21>25)]").isEqualTo(sn.stringify(true));
	}

	// Parameters must be constructed via adjacent tokens
	@Test
	public void needAdjacentTokensForParameters() {
		checkForParseError("foo -- name=value", DSLMessage.NO_WHITESPACE_BEFORE_ARG_NAME, 7);
		checkForParseError("foo --name =value", DSLMessage.NO_WHITESPACE_BEFORE_ARG_EQUALS, 11);
		checkForParseError("foo --name= value", DSLMessage.NO_WHITESPACE_BEFORE_ARG_VALUE, 12);
	}

	@Test
	public void composedOptionNameErros() {
		checkForParseError("foo --name.=value", DSLMessage.NOT_EXPECTED_TOKEN, 11);
		checkForParseError("foo --name .sub=value", DSLMessage.NO_WHITESPACE_IN_DOTTED_NAME, 11);
		checkForParseError("foo --name. sub=value", DSLMessage.NO_WHITESPACE_IN_DOTTED_NAME, 12);
	}

	@Test
	public void xd2416() {
		StreamNode ast = parse("http | transform --expression='payload.replace(\"abc\", \"\")' | log");
		assertThat((String) ast.getAppNodes().get(1).getArgumentsAsProperties().get("expression"))
			.isEqualTo("payload" + ".replace(\"abc\", \"\")");

		ast = parse("http | transform --expression='payload.replace(\"abc\", '''')' | log");
		assertThat((String) ast.getAppNodes().get(1).getArgumentsAsProperties().get("expression"))
			.isEqualTo("payload" + ".replace(\"abc\", '')");
	}

	@Test
	public void parseUnboundStreamApp() {
		StreamNode sn = parse("foo");
		List<AppNode> appNodes = sn.getAppNodes();
		assertThat(appNodes.get(0).isUnboundStreamApp()).isTrue();
	}

	@Test
	public void parseUnboundStreamApps() {
		StreamNode sn = parse("foo|| bar|| baz");
		List<AppNode> appNodes = sn.getAppNodes();
		assertThat(3).isEqualTo(appNodes.size());
		assertThat("foo").isEqualTo(appNodes.get(0).getName());
		assertThat("baz").isEqualTo(appNodes.get(2).getName());
		assertThat(appNodes.get(0).isUnboundStreamApp()).isTrue();

		sn = parse("foo | bar");
		appNodes = sn.getAppNodes();
		assertThat(2).isEqualTo(appNodes.size());
		assertThat("foo").isEqualTo(appNodes.get(0).getName());
		assertThat("bar").isEqualTo(appNodes.get(1).getName());
		assertThat(appNodes.get(0).isUnboundStreamApp()).isFalse();

		checkForParseError("foo||",DSLMessage.OOD, 5);

		sn = parse("foo --aaa=,|| bar");
		appNodes = sn.getAppNodes();
		assertThat(2).isEqualTo(appNodes.size());
		assertThat("foo --aaa=,").isEqualTo(appNodes.get(0).toString());
		assertThat("bar").isEqualTo(appNodes.get(1).toString());
	}

	@Test
	public void parseUnboundStreamAppsWithParams() {
		StreamNode sn = parse("foo --aaa=bbb || bar");
		List<AppNode> appNodes = sn.getAppNodes();
		assertThat(2).isEqualTo(appNodes.size());
		assertThat("foo --aaa=bbb").isEqualTo(appNodes.get(0).toString());
		assertThat("bar").isEqualTo(appNodes.get(1).toString());

		// No space after bbb argument
		sn = parse("foo --aaa=bbb|| bar");
		appNodes = sn.getAppNodes();
		assertThat(2).isEqualTo(appNodes.size());
		assertThat("foo --aaa=bbb").isEqualTo(appNodes.get(0).toString());
		assertThat("bar").isEqualTo(appNodes.get(1).toString());

		sn = parse("foo --aaa=\"bbb\"|| bar");
		appNodes = sn.getAppNodes();
		assertThat(2).isEqualTo(appNodes.size());
		assertThat("foo --aaa=bbb").isEqualTo(appNodes.get(0).toString());
		assertThat("bar").isEqualTo(appNodes.get(1).toString());

		sn = parse("foo --aaa=\"bbb\" || bar");
		appNodes = sn.getAppNodes();
		assertThat(2).isEqualTo(appNodes.size());
		assertThat("foo --aaa=bbb").isEqualTo(appNodes.get(0).toString());
		assertThat("bar").isEqualTo(appNodes.get(1).toString());

		checkForParseError("foo --aaa=\"bbb\"||",DSLMessage.OOD, 17);
		checkForParseError("foo --aaa=\"bbb\" ||",DSLMessage.OOD, 18);
	}

	protected void checkForIllegalStreamName(String streamName, String streamDef) {
		try {
			StreamNode sn = parse(streamName, streamDef);
			throw new AssertionError("expected to fail but parsed " + sn.stringify());
		}
		catch (ParseException e) {
			assertThat(e.getMessageCode()).isEqualTo(DSLMessage.ILLEGAL_STREAM_NAME);
			assertThat(e.getPosition()).isEqualTo(0);
			assertThat(streamName).isEqualTo(e.getInserts()[0]);
		}
	}

	protected void checkForParseError(String stream, DSLMessage msg, int pos, Object... inserts) {
		try {
			StreamNode sn = parse(stream);
			throw new AssertionError("expected to fail but parsed " + sn.stringify());
		}
		catch (ParseException e) {
			assertThat(msg).isEqualTo(e.getMessageCode());
			assertThat(pos).isEqualTo(e.getPosition());
			if (inserts != null) {
				for (int i = 0; i < inserts.length; i++) {
					assertThat(inserts[i]).isEqualTo(e.getInserts()[i]);
				}
			}
		}
	}
}
