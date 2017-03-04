/*
 * Copyright 2017 the original author or authors.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.ANDAND;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.ARROW;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.CLOSE_PAREN;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.COLON;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.GT;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.IDENTIFIER;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.LITERAL_STRING;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.LT;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.OPEN_PAREN;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.cloud.dataflow.core.dsl.graph.Graph;

/**
 * Test the parser and visitor infrastructure. Check it accepts expected data and 
 * correctly handles bad data. Check that the parsed out
 * converts to/from the graph format that the UI can use.
 *
 * @author Andy Clement
 */
public class ComposedTaskParserTests {

	private ComposedTaskNode ctn;

	@Test
	public void basics() {
		Tokens tokens = new ComposedTaskTokenizer().getTokens("App1");
		assertToken(IDENTIFIER, "App1", 0, 4, tokens.next());
		tokens = new ComposedTaskTokenizer().getTokens("App1 && App2");
		assertToken(IDENTIFIER, "App1", 0, 4, tokens.next());
		assertToken(ANDAND, "&&", 5, 7, tokens.next());
		assertToken(IDENTIFIER, "App2", 8, 12, tokens.next());
		tokens = new ComposedTaskTokenizer().getTokens("< > -> ( )");
		assertToken(LT,"<",0,1,tokens.next());
		assertToken(GT,">",2,3,tokens.next());
		assertToken(ARROW,"->",4,6,tokens.next());
		assertToken(OPEN_PAREN,"(",7,8,tokens.next());
		assertToken(CLOSE_PAREN,")",9,10,tokens.next());
	}
	
	@Test
	public void tokenStreams() {
		Tokens tokens = new ComposedTaskTokenizer().getTokens("App1 0->App2 1->:Bar");
		assertTokens(tokens, IDENTIFIER, IDENTIFIER, ARROW, IDENTIFIER, IDENTIFIER, ARROW, COLON, IDENTIFIER);
		tokens = new ComposedTaskTokenizer().getTokens("App1 0->App2 'abc' ->   App3");
		assertTokens(tokens, IDENTIFIER, IDENTIFIER, ARROW, IDENTIFIER, LITERAL_STRING, ARROW, IDENTIFIER);
	}

	@Test
	public void singleApp() {
		ctn = parse("FooApp");
		assertEquals("FooApp", ctn.getComposedTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(6, ctn.getEndPos());
		assertEquals("FooApp", ctn.stringify());
		LabelledComposedTaskNode node = ctn.getStart();
		assertFalse(node.isSplit());
		assertTrue(node.isFlow());
		assertFlow(node,"FooApp");
		assertTrue(((FlowNode)node).getSeriesElement(0).isTaskApp());
	}
	
	@Test
	public void twoAppFlow() {
		ctn = parse("FooApp  &&  BarApp");

		assertEquals("FooApp  &&  BarApp", ctn.getComposedTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(18, ctn.getEndPos());
		assertEquals("FooApp && BarApp", ctn.stringify());

		LabelledComposedTaskNode node = ctn.getStart();
		assertFalse(node.isSplit());
		assertTrue(node.isFlow());
		assertFalse(node.isTaskApp());
		
		FlowNode flow = (FlowNode)node;
		List<LabelledComposedTaskNode> series = flow.getSeries();
		assertEquals(2,series.size());
		assertEquals(2,flow.getSeriesLength());
		assertTaskApp(series.get(0),"FooApp");
		assertTaskApp(flow.getSeriesElement(0),"FooApp");
		assertTaskApp(series.get(1),"BarApp");
		assertTaskApp(flow.getSeriesElement(1),"BarApp");
	}
	
	@Test
	public void twoAppSplit() {
		ctn = parse("< FooApp  ||    BarApp>");

		assertEquals("< FooApp  ||    BarApp>", ctn.getComposedTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(23, ctn.getEndPos());
		assertEquals("<FooApp || BarApp>", ctn.stringify());

		LabelledComposedTaskNode node = ctn.getStart();
		assertTrue(node.isFlow());
		node = ((FlowNode)node).getSeriesElement(0);
		assertTrue(node.isSplit());
		assertFalse(node.isTaskApp());
		
		SplitNode split = (SplitNode)node;
		List<LabelledComposedTaskNode> series = split.getSeries();
		assertEquals(2,series.size());
		assertEquals(2,split.getSeriesLength());
		assertFlow(series.get(0), "FooApp");
		assertFlow(split.getSeriesElement(0), "FooApp");
		assertFlow(series.get(1), "BarApp");
		assertFlow(split.getSeriesElement(1), "BarApp");
	}
	
	@Test
	public void appWithOneTransition() {
		ctn = parse("App1 0->App2");
		assertEquals("test",ctn.getName());
		assertEquals("App1 0->App2", ctn.getComposedTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(12, ctn.getEndPos());
		assertEquals("App1 0->App2", ctn.stringify());
		LabelledComposedTaskNode firstNode = ctn.getStart();
		assertTrue(firstNode.isFlow());
		List<TransitionNode> transitions = ((TaskAppNode)((FlowNode)firstNode).getSeriesElement(0)).getTransitions();
		assertEquals(1,transitions.size());
		TransitionNode transition = transitions.get(0);
		assertEquals("0",transition.getStatusToCheck());
		assertEquals("App2",transition.getTargetDslText());
		assertEquals(5,transition.getStartPos());
		assertEquals(12,transition.getEndPos());
	}

	@Test
	public void appWithTwoTransitions() {
		ctn = parse("App1 0->App2 'abc' ->   App3");
		assertEquals("App1 0->App2 'abc' ->   App3", ctn.getComposedTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(28, ctn.getEndPos());
		assertEquals("App1 0->App2 'abc'->App3", ctn.stringify());
		LabelledComposedTaskNode node = ctn.getStart();
		assertTrue(node.isFlow());
		node = ((FlowNode)node).getSeriesElement(0);
		List<TransitionNode> transitions = ((TaskAppNode)node).getTransitions();
		assertEquals(2,transitions.size());
		TransitionNode transition = transitions.get(0);
		assertEquals("0",transition.getStatusToCheck());
		assertTrue(transition.isExitCodeCheck());
		assertEquals("App2",transition.getTargetDslText());
		assertEquals(5,transition.getStartPos());
		assertEquals(12,transition.getEndPos());
		transition = transitions.get(1);
		assertEquals("abc",transition.getStatusToCheck());
		assertFalse(transition.isExitCodeCheck());
		assertEquals("App3",transition.getTargetDslText());
		assertEquals(13,transition.getStartPos());
		assertEquals(28,transition.getEndPos());
	}
	
	@Test
	public void appWithWildcardTransitions() {
		ctn = parse("App1 *->App2 '*'->App3");
		assertEquals("App1 *->App2 '*'->App3", ctn.getComposedTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(22, ctn.getEndPos());
		assertEquals("App1 *->App2 '*'->App3", ctn.stringify());
		LabelledComposedTaskNode node = ctn.getStart();
		node = ((FlowNode)node).getSeriesElement(0);
		assertTrue(node.isTaskApp());
		List<TransitionNode> transitions = ((TaskAppNode)node).getTransitions();
		assertEquals(2,transitions.size());
		
		TransitionNode transition = transitions.get(0);
		assertEquals("*",transition.getStatusToCheck());
		assertTrue(transition.isExitCodeCheck());
		assertEquals("App2",transition.getTargetDslText());
		assertEquals(5,transition.getStartPos());
		assertEquals(12,transition.getEndPos());
		transition = transitions.get(1);
		assertEquals("*",transition.getStatusToCheck());
		assertFalse(transition.isExitCodeCheck());
		assertEquals("App3",transition.getTargetDslText());
		assertEquals(13,transition.getStartPos());
		assertEquals(22,transition.getEndPos());
	}
	
	@Test
	public void appWithLabelReferenceTransition() {
		ctn = parse("App1 'foo'->:something", false);
		assertEquals("App1 'foo'->:something", ctn.getComposedTaskText());
		assertEquals(0, ctn.getStartPos());
		assertEquals(22, ctn.getEndPos());
		assertEquals("App1 'foo'->:something", ctn.stringify());
		LabelledComposedTaskNode firstNode = ctn.getStart();
		assertFlow(firstNode,"App1");
		List<TransitionNode> transitions = ((TaskAppNode)((FlowNode)firstNode).getSeriesElement(0)).getTransitions();
		assertEquals(1,transitions.size());
		TransitionNode transition = transitions.get(0);
		assertEquals("foo",transition.getStatusToCheck());
		assertFalse(transition.isExitCodeCheck());
		assertEquals(":something",transition.getTargetDslText());
		assertEquals("something",transition.getTargetLabel());
		assertEquals(5,transition.getStartPos());
		assertEquals(22,transition.getEndPos());
	}
	
	@Test
	public void splitMainComposedTaskOverMultipleLines() {
		ctn = parse("FooApp &&\nBarApp");
		assertFlow(ctn.getStart(),"FooApp","BarApp");
		ctn = parse("FooApp\n&& BarApp");
		assertFlow(ctn.getStart(),"FooApp","BarApp");
		ctn = parse("FooApp\n&&\nBarApp");
		assertFlow(ctn.getStart(),"FooApp","BarApp");
		ctn = parse("FooApp 0->:a 1->:b &&\nBarApp 2->:c 3->:d", false);
		assertFlow(ctn.getStart(),"FooApp","BarApp");
		ctn = parse("FooApp\n 0\n->:a\n 1->:b\n &&\nBarApp 2->:c 3->:d", false);
		assertFlow(ctn.getStart(),"FooApp","BarApp");
		ctn = parse("<FooApp ||\nBarApp>");
		assertSplit(((FlowNode)ctn.getStart()).getSeriesElement(0),"FooApp","BarApp");
		ctn = parse("<\nFooApp ||\nBarApp\n>");
		assertSplit(((FlowNode)ctn.getStart()).getSeriesElement(0),"FooApp","BarApp");
	}

	@Test
	public void labelledElement() {
		ctn = parse("foo: appA");
		LabelledComposedTaskNode start = ctn.getStart();
		assertEquals("foo", start.getLabelString());
		FlowNode f = (FlowNode)start;
		assertEquals("foo",f.getLabelString());
		assertEquals("appA",((TaskAppNode)f.getSeriesElement(0)).getName());

		ctn = parse("foo: <appA || appB>");
		start = ctn.getStart();
		assertEquals("foo", start.getLabelString());
		SplitNode s = (SplitNode) ((FlowNode)start).getSeriesElement(0);
		assertSplit(s, "appA", "appB");
		
		ctn = parse("foo: appA && appB");
		start = ctn.getStart();
		assertEquals("foo", start.getLabelString());
		assertFlow(start, "appA","appB");
	}
	
	@Test
	public void taskCollectorVisitor() {
		assertApps(parse("appA").getTaskApps(),"appA");
		assertApps(parse("appA && appB && appC").getTaskApps(),"appA","appB","appC");
		assertApps(parse("<appA || appB> && appC").getTaskApps(),"appA","appB","appC");
		assertApps(parse("<appA || appB> && appC && appC").getTaskApps(),"appA","appB","appC");
	}
	
	@Test
	public void transitionsToLaterInFlow() {
		String spec ="appA 'foo'->:bar && appB && bar: appC";
		assertGraph("[0:START][1:appA][2:appB][3:appC][4:END][0-1][1-2]['foo':1-3][2-3][3-4]",spec);
		checkDSLToGraphAndBackToDSL(spec);
		spec ="appA 'foo'->:bar && appB && bar: appC && appD";
		assertGraph("[0:START][1:appA][2:appB][3:appC][4:appD][5:END][0-1][1-2]['foo':1-3][2-3][3-4][4-5]",spec);
		checkDSLToGraphAndBackToDSL(spec);
	}
	
	@Test
	public void transitionToOtherSequence() {
		String spec =" appA 'fail'->:two && appB && appC;two: appD && appE";
		assertGraph("[0:START][1:appA][2:appB][3:appC][4:END][9:appD][10:appE]"+
		            "[0-1][1-2][2-3][3-4]['fail':1-9][9-10][10-4]",spec);
	}

	@Test
	public void secondarySequencesHaveFurtherTransitions() {
		String spec =" appA 'fail'->:two && appB;two: appD 'fail2'->:three && appE;three: appF && appG";
		assertGraph("[0:START][1:appA][2:appB][3:END][12:appD][13:appE][14:appF][15:appG]"+
		            "[0-1][1-2][2-3]['fail':1-12][12-13][13-3]['fail2':12-14][14-15][15-3]",spec);
	}

	@Test
	public void twoReferencesToSecondarySequence() {
		String spec = "appA 'fail'->:two && appB 'fail2'->:two && appC;two: appD && appE";
		assertGraph("[0:START][1:appA][2:appB][3:appC][4:END][9:appD][10:appE]"+
		            "[0-1][1-2][2-3][3-4]['fail':1-9]['fail2':2-9][9-10][10-4]",spec);
	}
	
	@Ignore
	@Test
	public void transitionToSplit() {
		String spec = "aa 'foo'->:split && bb && split: <cc || dd> && ee";
		// TODO lets consider this a limitation for now.
		assertGraph("[0:START][1:aa][2:bb][3:cc][4:dd][5:ee][6:END]"+
		            "[0-1][1-2]['foo':1-3][2-3][2-4][3-5][4-5][5-6]",spec);
	}

	@Test
	public void jumpingAround() {
		String spec = "one: aa && bb 'retry'->:one";
		assertGraph("[0:START][1:aa][2:bb][3:END][0-1][1-2]['retry':2-1][2-3]",spec);
		spec = "aa && one: bb && cc 'retry'->:one && dd";
		assertGraph("[0:START][1:aa][2:bb][3:cc][4:dd][5:END]"+
		            "[0-1][1-2][2-3]['retry':3-2][3-4][4-5]",spec);
		spec = "aa 0->:two && bb 0->:three && cc;two: dd;three:ee";
		assertGraph("[0:START][1:aa][2:bb][3:cc][4:END][11:dd][12:ee]"+
		            "[0-1][1-2][2-3][3-4][0:1-11][11-4][0:2-12][12-4]",spec);
		spec = "aa 'foo'->:two && bb && three: cc && dd;two: ee && ff '*'->:three";
		assertGraph("[0:START][1:aa][2:bb][3:cc][4:dd][5:END][10:ee][11:ff]"+
		            "[0-1][1-2][2-3][3-4][4-5]['foo':1-10][10-11][11-5]['*':11-3]",spec);
		spec = "aa 'foo'->:two && bb && four: cc && dd;two: ee && ff '*'->:three;three: gg '*'->:four";
		ComposedTaskNode parse = parse(spec);
		System.out.println(parse);
		assertGraph("[0:START][1:aa][2:bb][3:cc][4:dd][5:END][13:ee][14:ff][15:gg]" +
				    "[0-1][1-2][2-3][3-4][4-5]['foo':1-13][13-14][14-5]['*':14-15][15-5]['*':15-3]",spec);
	}
	
	@Test
	public void transitionToNonResolvedLabel() {
		String spec = "aa 'foo'->:split && bb && cc";
		ComposedTaskNode ctn = parse(spec,false);
		List<ComposedTaskValidationProblem> validationProblems = ctn.validate();
		assertEquals(1, validationProblems.size());
		assertEquals(DSLMessage.CT_VALIDATION_TRANSITION_TARGET_LABEL_UNDEFINED, validationProblems.get(0).getMessage());
		assertEquals(3, validationProblems.get(0).getOffset());
		
		spec = "<aa 'foo'->:split && bb && cc || dd>";
		ctn = parse(spec,false);
		validationProblems = ctn.validate();
		assertEquals(1, validationProblems.size());
		assertEquals(DSLMessage.CT_VALIDATION_TRANSITION_TARGET_LABEL_UNDEFINED, validationProblems.get(0).getMessage());
		assertEquals(4, validationProblems.get(0).getOffset());
	}
	
	@Test
	public void visitors() {
		ctn = parse("appA");
		TestVisitor tv = new TestVisitor();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >TA =TA[appA] <TA <F <SN[0]",tv.getString());

		ctn = parse("foo: appA");
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[foo: 0] >F =F[foo:] >TA =TA[foo: appA] <TA <F <SN[0]",tv.getString());

		ctn = parse("appA && appB");
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >TA =TA[appA] <TA >TA =TA[appB] <TA <F <SN[0]",tv.getString());
		
		ctn = parse("<appA || appB>");
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >S =S >F =F >TA =TA[appA] <TA <F >F =F >TA =TA[appB] <TA <F <S <F <SN[0]",tv.getString());

		ctn = parse("<appA && appB|| appC>");
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >S =S >F =F >TA =TA[appA] <TA >TA =TA[appB] <TA <F >F =F >TA =TA[appC] <TA <F <S <F <SN[0]",tv.getString());

		ctn = parse("appA 0->:foo", false);
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >TA =TA[appA] >T =T[0->:foo] <T <TA <F <SN[0]",tv.getString());

		ctn = parse("appA 0->appB");
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >TA =TA[appA] >T =T[0->appB] <T <TA <F <SN[0]",tv.getString());

		ctn = parse("appA;appB", false);
		tv.reset();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >TA =TA[appA] <TA <F <SN[0] >SN[1] >F =F >TA =TA[appB] <TA <F <SN[1]",tv.getString());
		
		ctn = parse("appA && appB 0->:foo *->appC;foo: appD && appE", false);
		assertApps(ctn.getTaskApps(),"appA","appB","appC","appD","appE");
		tv.reset();
		ctn.accept(tv);
		// TODO slight nuance here. foo: above is considered the label for the second flow and for the app appD, is that a problem? Is the distinction necessary?
		assertEquals(">SN[0] >F =F >TA =TA[appA] <TA >TA =TA[appB] >T =T[0->:foo] <T >T =T[*->appC] <T <TA <F <SN[0] >SN[foo: 1] >F =F[foo:] >TA =TA[foo: appD] <TA >TA =TA[appE] <TA <F <SN[1]",tv.getString());
	}
	

	@Test
	public void multiline() {
		ctn = parse("appA 0->:label1 && appB\nlabel1: appC");
	}
	
	@Test
	public void multiSequence() {
		ComposedTaskNode ctn = parse("appA\n  0->:foo\n  *->appB\n  && appE;foo: appC && appD");
		LabelledComposedTaskNode start = ctn.getStart(); // get the root of the AST starting appA
		assertNotNull(start);
		List<LabelledComposedTaskNode> sequences = ctn.getSequences();
		LabelledComposedTaskNode labelledComposedTaskNode = sequences.get(1);
		assertEquals("foo",labelledComposedTaskNode.getLabelString());
		LabelledComposedTaskNode fooSequence = ctn.getSequenceWithLabel("foo"); // get the AST for foo: ...
		assertNotNull(fooSequence);
		TestVisitor tv = new TestVisitor();
		ctn.accept(tv);
		assertEquals(">SN[0] >F =F >TA =TA[appA] >T =T[0->:foo] <T >T =T[*->appB] <T <TA >TA =TA[appE] <TA <F <SN[0] >SN[foo: 1] >F =F[foo:] >TA =TA[foo: appC] <TA >TA =TA[appD] <TA <F <SN[1]",tv.getString());
	}
	
	@Test
	public void validator() {
		ComposedTaskValidatorVisitor validator = new ComposedTaskValidatorVisitor();
		ctn = parse("appA");
		ctn.accept(validator);
		assertFalse(validator.hasProblems());
		
		validator.reset();
		ctn = parse("appA;appB",false);
		ctn.accept(validator);
		List<ComposedTaskValidationProblem> problems = validator.getProblems();
		assertEquals(1,problems.size());
		assertEquals(DSLMessage.CT_VALIDATION_SECONDARY_SEQUENCES_MUST_BE_NAMED,problems.get(0).getMessage());
		assertEquals(5,problems.get(0).getOffset());
		assertEquals("158E:(pos 5): Secondary sequences must have labels or are unreachable",problems.get(0).toString());
		assertEquals("158E:(pos 5): Secondary sequences must have labels or are unreachable\nappA;appB\n     ^\n",problems.get(0).toStringWithContext());

		validator.reset();
		ctn = parse("appA;foo: appB");
		ctn.accept(validator);
		assertFalse(validator.hasProblems());

		validator.reset();
		ctn = parse("appA;foo: appB\nappC",false);
		ctn.accept(validator);
		problems = validator.getProblems();
		assertEquals(1,problems.size());
		assertEquals(DSLMessage.CT_VALIDATION_SECONDARY_SEQUENCES_MUST_BE_NAMED,problems.get(0).getMessage());
		assertEquals(15,problems.get(0).getOffset());
		assertEquals("158E:(pos 15): Secondary sequences must have labels or are unreachable",problems.get(0).toString());
		assertEquals("158E:(pos 15): Secondary sequences must have labels or are unreachable\nappC\n^\n",problems.get(0).toStringWithContext());
	}
	
	@Test
	public void labels() {
		// basic task
		ctn = parse("aaa: appA");
		LabelledComposedTaskNode flow = ctn.getStart();
		assertEquals("aaa",flow.getLabelString());
		TaskAppNode taskApp = (TaskAppNode)((FlowNode)flow).getSeriesElement(0);
		assertEquals("aaa",taskApp.getLabelString());
		
		// flows
		ctn = parse("aaa: appA && bbb: appB");
		taskApp = (TaskAppNode)((FlowNode) ctn.getStart()).getSeriesElement(1);
		assertEquals("bbb",taskApp.getLabelString());
		
		// splits
		ctn = parse("outer:<aaa: appA || bbb: appB>");
		flow = (FlowNode)ctn.getStart();
		assertEquals("outer",flow.getLabelString());
		SplitNode s = (SplitNode)flow.getSeriesElement(0);
		assertEquals("outer",s.getLabelString());
		taskApp = (TaskAppNode)(((FlowNode)s.getSeriesElement(0)).getSeriesElement(0));
		assertEquals("aaa",taskApp.getLabelString());
		taskApp = (TaskAppNode)(((FlowNode)s.getSeriesElement(1)).getSeriesElement(0));
		assertEquals("bbb",taskApp.getLabelString());
		
		// parentheses
		ctn = parse("(aaa: appA && appB)");
		taskApp = (TaskAppNode)((FlowNode) ctn.getStart()).getSeriesElement(0);
		assertEquals("aaa", taskApp.getLabelString());
		
		checkForParseError("aaa: (appA)", DSLMessage.COMPOSED_TASK_NO_LABELS_ON_PARENS, 5);
		checkForParseError("aaa: bbb: appA", DSLMessage.COMPOSED_TASK_NO_DOUBLE_LABELS, 5);
		checkForParseError("aaa: >", DSLMessage.NOT_EXPECTED_TOKEN, 5);
		checkForParseError("aaa: &&", DSLMessage.NOT_EXPECTED_TOKEN, 5);
		checkForParseError("aaa:: appA", DSLMessage.NOT_EXPECTED_TOKEN, 4);
	}
	
	@Test
	public void badTransitions() {
		checkForParseError("App1 ->",DSLMessage.COMPOSED_TASK_ARROW_SHOULD_BE_PRECEDED_BY_CODE,5);
		checkForParseError("App1 0->x ->",DSLMessage.COMPOSED_TASK_ARROW_SHOULD_BE_PRECEDED_BY_CODE,10);
		checkForParseError("App1 ->xx",DSLMessage.COMPOSED_TASK_ARROW_SHOULD_BE_PRECEDED_BY_CODE,5);
		checkForParseError("App1 xx->",DSLMessage.OOD,9);
	}

	@Test
	public void graphToText() {
		assertGraph("[0:START][1:AppA][2:END][0-1][1-2]","AppA");
		checkDSLToGraphAndBackToDSL("AppA");
		assertGraph("[0:START][1:AppA][2:AppB][3:END][0-1][1-2][2-3]","AppA && AppB");
		checkDSLToGraphAndBackToDSL("AppA && AppB");
		assertGraph("[0:START][1:AppA][2:AppB][3:END][0-1][0-2][1-3][2-3]","<AppA || AppB>");
		checkDSLToGraphAndBackToDSL("<AppA || AppB>");
		assertGraph("[0:START][1:AppA][2:AppB][3:AppC][4:END][0-1][1-2][0-3][2-4][3-4]","<AppA && AppB || AppC>");
		checkDSLToGraphAndBackToDSL("<AppA && AppB || AppC>");
		assertGraph("[0:START][1:AppA][2:AppB][3:AppC][4:END][0-1][0-2][2-3][1-4][3-4]","<AppA || AppB && AppC>");
		checkDSLToGraphAndBackToDSL("<AppA || AppB && AppC>");
		assertGraph("[0:START][1:AppA][2:AppB][3:AppC][4:AppD][5:END][0-1][1-2][0-3][3-4][2-5][4-5]","<AppA && AppB || AppC && AppD>");
		checkDSLToGraphAndBackToDSL("<AppA && AppB || AppB && AppC>");
		assertGraph("[0:START][1:AppA][2:AppB][3:AppC][4:END][0-1][1-2][1-3][2-4][3-4]","AppA && <AppB || AppC>");
		checkDSLToGraphAndBackToDSL("AppA && <AppB || AppC>");
		assertGraph("[0:START][1:AppA][2:AppB][3:AppC][4:AppD][5:END][0-1][1-2][1-3][2-4][3-4][4-5]",
				   "AppA && <AppB || AppC> && AppD");
		checkDSLToGraphAndBackToDSL("AppA && <AppB || AppC> && AppD");
		assertGraph("[0:START][1:AppA][2:AppB][3:SYNC][4:AppC][5:AppD][6:END][0-1][0-2][1-3][2-3][3-4][3-5][4-6][5-6]","<AppA || AppB> && <AppC || AppD>");
		checkDSLToGraphAndBackToDSL("<AppA || AppB> && <AppC || AppD>");
		assertGraph("[0:START][1:AppA][2:AppB][3:AppC][4:SYNC][5:AppD][6:AppE][7:AppF][8:END][0-1][1-2][0-3][2-4][3-4][4-5][4-6][6-7][5-8][7-8]","<AppA && AppB || AppC> && <AppD || AppE && AppF>");
		checkDSLToGraphAndBackToDSL("<AppA && AppB || AppC> && <AppD || AppE && AppF>");
		checkDSLToGraphAndBackToDSL("<AppA && AppB || AppC> && <AppD || AppE && AppF>");
		checkDSLToGraphAndBackToDSL("<foojob || bbb && ccc>");
		checkDSLToGraphAndBackToDSL("<a || b> && c");
		checkDSLToGraphAndBackToDSL("a && <b || c>");
	}
	
	@Test
	public void textToGraphWithTransitions() {
		assertGraph("[0:START][1:AppA][2:AppE][3:AppB][4:END][0-1][0:1-2][1-3][3-4][2-4]","AppA 0->AppE && AppB");
		checkDSLToGraphAndBackToDSL("AppA 0->AppE && AppB");
		assertGraph("[0:START][1:AppA][2:AppE][3:AppB][4:AppC][5:END][0-1][0:1-2][1-3][3-4][4-5][2-5]","AppA 0->AppE && AppB && AppC");
		checkDSLToGraphAndBackToDSL("AppA 0->AppE && AppB && AppC");
		assertGraph("[0:START][1:AppA][2:AppE][3:AppB][4:AppC][5:AppD][6:END][0-1][0:1-2][1-3][3-4][3-5][4-6][5-6][2-6]",
		          "AppA 0->AppE && AppB && <AppC || AppD>");
		checkDSLToGraphAndBackToDSL("AppA 0->AppE && AppB && <AppC || AppD>");
		checkDSLToGraphAndBackToDSL("aaa 'FOO'->XXX 'B'->bbb '*'->ccc && bbb && ccc");
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
		checkForParseError("<aa | bb>", DSLMessage.COMPOSED_TASK_DOUBLE_OR_REQUIRED, 4);
		checkForParseError("<aa ||| bb>", DSLMessage.COMPOSED_TASK_DOUBLE_OR_REQUIRED, 6);
	}

	@Test
	public void unexpectedDoubleAnd() {
		checkForParseError("aa  &&&& bb", DSLMessage.NOT_EXPECTED_TOKEN, 6, "identifier","&&");
	}

	@Test
	public void transitionsWithSameTarget() {
		// Not quoted transition names
		checkForParseError("<foojob completed->killjob || barjob 'completed'->killjob>", DSLMessage.COMPOSED_TASK_UNQUOTED_TRANSITION_CHECK_MUST_BE_NUMBER,8);
		// Rule: In the same flow, references to the same transition target are the same node
		assertGraph("[0:START][1:foojob][2:killjob][3:barjob][4:END][0-1]['completed':1-2][1-3]['completed':3-2][3-4][2-4]",
				    "foojob 'completed'->killjob && barjob 'completed'->killjob");
		// Rule: Across splits, references to the same transition target are different instances
		assertGraph("[0:START][1:foojob][2:killjob][3:barjob][4:killjob][5:END]"+
		            "[0-1]['completed':1-2][0-3]['completed':3-4][1-5][2-5][3-5][4-5]",
				    "<foojob 'completed'->killjob || barjob 'completed'->killjob>");
		checkDSLToGraphAndBackToDSL("<foojob 'completed'->killjob || barjob 'completed'->killjob>");
	}

	@Test
	public void toDSLTextTransitions() {
		// [SHOULD-VALIDATE] There is no real route to bbb
		String spec = "aaa '*'->$END && bbb";
		assertEquals(spec, parse(spec).toDSL());
		assertGraph("[0:START][1:aaa][2:$END][3:bbb][4:END]"+
		            "[0-1]['*':1-2][1-3][3-4]", spec);
		checkDSLToGraphAndBackToDSL(spec);
	}

	@Test
	// You can't draw this on the graph, it would end up looking like "aaa | '*' = $END || bbb || ccc
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
		checkDSLToGraphAndBackToDSL("aaa 'COMPLETED'->kill && bbb && ccc");
		checkDSLToGraphAndBackToDSL("aaa 'COMPLETED'->kill && bbb 'COMPLETED'->kill && ccc");
		checkDSLToGraphAndBackToDSL("aaa 'COMPLETED'->kill 'FOO'->bar && bbb 'COMPLETED'->kill && ccc");
	}

	@Test
	public void toDSLTextSplitTransitions() {
		checkDSLToGraphAndBackToDSL("<aaa 'COMPLETED'->kill || bbb> && ccc");
		checkDSLToGraphAndBackToDSL("<aaa 'COMPLETED'->kill || bbb 'COMPLETED'->kill> && ccc");
		//		checkDSLToGraphAndBackToDSL("<aaa | COMPLETED = kill | '*' = kill2 & bbb | COMPLETED = kill> || ccc");
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
		assertGraph("[0:START][1:a][2:b][3:SYNC][4:c][5:d][6:END]"+
		            "[0-1][0-2][1-3][2-3][3-4][3-5][4-6][5-6]", spec);
	}

	@Test
	public void toDSLTextSqoop() {
		String spec = "<(sqoop-6e44 'FAILED'->kill1" +
				"  && sqoop-e07a 'FAILED'->kill1) & " +
				" (sqoop-035f 'FAILED'->kill2" +
				"  && sqoop-9408 'FAILED'->kill2" +
				"  && sqoop-a6e0 'FAILED'->kill2" +
				"  && sqoop-e522 'FAILED'->kill2" +
				"  && shell-b521 'FAILED'->kill2) || " +
				" (sqoop-6420 'FAILED'->kill3)>";
		// DSL text right now doesn't include parentheses (they aren't necessary)
		spec = "<sqoop-6e44 'FAILED'->kill1" +
				" && sqoop-e07a 'FAILED'->kill1 ||" +
				" sqoop-035f 'FAILED'->kill2" +
				" && sqoop-9408 'FAILED'->kill2" +
				" && sqoop-a6e0 'FAILED'->kill2" +
				" && sqoop-e522 'FAILED'->kill2" +
				" && shell-b521 'FAILED'->kill2 ||" +
				" sqoop-6420 'FAILED'->kill3>";
		checkDSLToGraphAndBackToDSL(spec);
	}

	@Test
	public void toDSLTextManualSync() {
		// Here foo is effectively acting as a SYNC node
		String spec = "<a || b> && foo && <c || d>";
		checkDSLToGraphAndBackToDSL(spec);
	}


	@Test
	public void whitespace() {
		assertEquals("A && B",parse("A&&B").stringify());
		assertEquals("<A || B>",parse("<A||B>").stringify());
		assertEquals("<A && B || C>",parse("<A&&B||C>").stringify());
	}

	@Test
	public void endTransition() {
		String spec = "aaa 'broken'->$END";
		assertGraph("[0:START][1:aaa][2:$END][3:END][0-1]['broken':1-2][1-3]",spec);
		checkDSLToGraphAndBackToDSL(spec);
	}

	// TODO not quoted state transition names
	@Test
	public void missingQuotes() {
		checkForParseError("appA BROKEN->$FAIL", DSLMessage.COMPOSED_TASK_UNQUOTED_TRANSITION_CHECK_MUST_BE_NUMBER, 5, "BROKEN");
		checkForParseError("appA\n BROKEN->$FAIL", DSLMessage.COMPOSED_TASK_UNQUOTED_TRANSITION_CHECK_MUST_BE_NUMBER, 6, "BROKEN");
	}
	
	@Test
	public void parentheses2() {
		ComposedTaskNode ctn = parse("<(jobA && jobB && jobC) || jobC>");
		assertEquals("<jobA && jobB && jobC || jobC>", ctn.stringify());
	}

	@Test
	public void funnyJobNames() {
		ctn = parse("a.b.c");
		assertFlow(ctn.getStart(),"a.b.c");
		ctn = parse("a.b.c && d.e.f");
		checkDSLToGraphAndBackToDSL("a.b.c && d.e.f");
		assertGraph("[0:START][1:a.b.c][2:d.e.f][3:END][0-1][1-2][2-3]","a.b.c && d.e.f");
	}

	@Test
	public void nestedSplit1() {
		ComposedTaskNode ctn = parse("<<jobA || jobB> || jobC>");
		assertEquals("<<jobA || jobB> || jobC>", ctn.stringify());
		LabelledComposedTaskNode start = ctn.getStart();
		assertTrue(start instanceof FlowNode);
		SplitNode split = (SplitNode)((FlowNode)start).getSeriesElement(0);
		LabelledComposedTaskNode seriesElement = ((FlowNode)split.getSeriesElement(0)).getSeriesElement(0);
		assertTrue(seriesElement instanceof SplitNode);
		SplitNode split2 = (SplitNode)seriesElement;
		assertEquals(2,split2.getSeriesLength());
	}

	@Test
	public void nestedSplit2() {
		ComposedTaskNode ctn = parse("<jobA || <jobB || jobC> || jobD>");
		assertEquals("<jobA || <jobB || jobC> || jobD>", ctn.stringify());
		LabelledComposedTaskNode start = ctn.getStart();
		assertTrue(start.isFlow());
		SplitNode split = (SplitNode)((FlowNode)start).getSeriesElement(0);
		assertEquals(3,split.getSeriesLength());
		LabelledComposedTaskNode seriesElement = split.getSeriesElement(1);
		SplitNode splitSeriesElement = (SplitNode)((FlowNode)seriesElement).getSeriesElement(0);
		assertTrue(splitSeriesElement.isSplit());
		assertEquals(2,splitSeriesElement.getSeriesLength());
		assertEquals("<jobB || jobC>",splitSeriesElement.stringify());
		assertEquals("jobB",((TaskAppNode)((FlowNode)splitSeriesElement.getSeriesElement(0)).getSeriesElement(0)).getName());
	}

	@Test
	public void singleTransition() {
		ComposedTaskNode ctn = parse("foo 'completed'->bar");
		LabelledComposedTaskNode start = ctn.getStart();
		start = ((FlowNode)start).getSeriesElement(0);
		assertTrue(start instanceof TaskAppNode);
		TaskAppNode ta = (TaskAppNode) start;
		List<TransitionNode> transitions = ta.getTransitions();
		assertEquals(1, transitions.size());
		assertEquals("completed", transitions.get(0).getStatusToCheck());
		assertEquals("bar", transitions.get(0).getTargetApp());
	}

	@Test
	public void doubleTransition() {
		ComposedTaskNode ctn = parse("foo 'completed'->bar 'wibble'->wobble");
		LabelledComposedTaskNode start = ctn.getStart();
		assertFlow(start,"foo");
		TaskAppNode ta = (TaskAppNode) ((FlowNode)start).getSeriesElement(0);
		List<TransitionNode> transitions = ta.getTransitions();
		assertEquals(2, transitions.size());
		assertEquals("completed", transitions.get(0).getStatusToCheck());
		assertEquals("bar", transitions.get(0).getTargetApp());
		assertEquals("wibble", transitions.get(1).getStatusToCheck());
		assertEquals("wobble", transitions.get(1).getTargetApp());
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
		ComposedTaskNode ctn = 
				parse("<foo\n"
				    + "  'completed'->kill\n"
				    + "  '*'->custard\n"
				    + "  || bar>");
		assertEquals("<foo 'completed'->kill '*'->custard || bar>", ctn.stringify());
	}

	@Test
	public void toGraphBlank() {
		ctn = parse("");
		ctn.toGraph();
		assertGraph("[0:START][1:END][0-1]","");
		checkDSLToGraphAndBackToDSL("");
	}

	@Test
	public void complexToGraphAndBack() {
		ctn = parse(
				"aaa 'FAILED'->iii && <bbb 'FAILED'->iii '*'->$END || ccc 'FAILED'->jjj '*'->$END> &&ddd 'FAILED'->iii&&eee 'FAILED'->iii&&fff 'FAILED'->iii && <ggg 'FAILED'->kkk '*'->$END|| hhh 'FAILED'->kkk '*'->$END>    ");
		checkDSLToGraphAndBackToDSL(
				"aaa 'FAILED'->iii && <bbb 'FAILED'->iii '*'->$END || ccc 'FAILED'->jjj '*'->$END> && ddd 'FAILED'->iii && eee 'FAILED'->iii && fff 'FAILED'->iii && <ggg 'FAILED'->kkk '*'->$END || hhh 'FAILED'->kkk '*'->$END>");
	}

	@Test
	public void toGraph$END() {
		ComposedTaskNode ctn = parse("foo 'oranges'->$END");
		assertEquals("foo 'oranges'->$END", ctn.toDSL());

		// Graph creation
		// Note: node '2' does not exist, it was an $END node for the transition, when we discovered that was
		// going to be joined to the final $END node we re-routed the links targeting '2' to the real end
		// node and removed it
		assertGraph("[0:START][1:foo][2:$END][3:END][0-1]['oranges':1-2][1-3]", "foo 'oranges'->$END");
//		assertEquals(toExpectedGraph("n:0:START,n:1:foo,n:3:END,l:0:1,l:1:3:transitionName=oranges"),
//				js.toGraph().toJSON());

		// Graph to DSL
		checkDSLToGraphAndBackToDSL("foo 'oranges'->$END");
	}
	
	@Test
	public void toGraph$END2() {
		String definition = "aaa 'foo'->$END 'B'->bbb '*'->ccc && bbb && ccc";
		assertParseAndBackToDSL(definition);
		// {"nodes":[{"id":"0","name":"START"},{"id":"1","name":"aaa"},{"id":"3","name":"bbb"},{"id":"4","name":"ccc"},
		//           {"id":"5","name":"END"}],
		//  "links":[{"from":"0","to":"1"},{"from":"1","to":"5","properties":{"transitionName":"foo"}},
		//           {"from":"1","to":"3","properties":{"transitionName":"B"}},
		//           {"from":"1","to":"4","properties":{"transitionName":"'*'"}},
		//           {"from":"3","to":"4"},{"from":"4","to":"5"}]}
		assertGraph("[0:START][1:aaa][2:$END][3:bbb][4:ccc][5:bbb][6:ccc][7:END]"+
		            "[0-1]['foo':1-2]['B':1-3]['*':1-4][1-5][5-6][6-7][3-7][4-7]",definition);
		checkDSLToGraphAndBackToDSL("aaa 'foo'->$END 'B'->bbb '*'->ccc && bbb && ccc");
	}

	@Test
	public void toGraph$END3() {
		// The trailing 'bbb' is redundant here...
		String spec = "aaa 'foo'->$END 'B'->bbb '*'->$END && bbb";

		assertEquals(spec,parse(spec).toDSL());

		// Graph creation
		//		assertEquals(
		//				toExpectedGraph("n:0:START,n:1:aaa,n:2:bbb,n:3:ccc,n:4:END," +
		//						"l:0:1,l:2:3,l:1:4:transitionName=foo,l:1:2:transitionName=B,l:1:3:transitionName='*',l:3:4"),
		//				js.toGraph().toJSON());

		// TODO should the $ENDs just be joined to END?
		assertGraph("[0:START][1:aaa][2:$END][3:bbb][4:bbb][5:END]"+
		            "[0-1]['foo':1-2]['B':1-3]['*':1-2][1-4][4-5][3-5]",spec);

		checkDSLToGraphAndBackToDSL(spec);
	}


	@Test
	public void toGraph$FAIL() {
		String spec = "foo 'oranges'->$FAIL";
		assertEquals(spec,parse(spec).toDSL());
		assertGraph("[0:START][1:foo][2:$FAIL][3:END][0-1]['oranges':1-2][1-3]",spec);
		checkDSLToGraphAndBackToDSL(spec);
	}

	// TODO should & end the boo job name? Don't think it does right now
	// 	js = parse("<foo | completed=boo& bar> || boo");

	@Test
	public void toGraphWithTransition() throws Exception {
		// Should be two different goo nodes
		String spec = "<foo 'completed'->goo || bar> && boo && goo";
		assertGraph("[0:START][1:foo][2:goo][3:bar][4:boo][5:goo][6:END]"+
		            "[0-1]['completed':1-2][0-3][1-4][2-4][3-4][4-5][5-6]",spec);
	}

	@Test
	public void toGraphWithTransition2() {
		// The target transition node hoo is not elsewhere on the list
		String definition = "<foo 'completed'->hoo || bar> && boo && goo";
		assertGraph("[0:START][1:foo][2:hoo][3:bar][4:boo][5:goo][6:END]"+
		            "[0-1]['completed':1-2][0-3][1-4][2-4][3-4][4-5][5-6]",definition);
		checkDSLToGraphAndBackToDSL(definition);
	}

	@Test
	public void sqoopExample() {
		String spec =
			    "<(sqoop-6e44 'FAILED'->kill1\n" +
				"  && sqoop-e07a 'FAILED'->kill1) || \n" +
				" (sqoop-035f 'FAILED'->kill2\n" +
				"  && sqoop-9408 'FAILED'->kill2\n" +
				"  && sqoop-a6e0 'FAILED'->kill2\n" +
				"  && sqoop-e522 'FAILED' ->kill2\n" +
				"  && shell-b521 'FAILED'->kill2) || \n" +
				" (sqoop-6420 'FAILED'->kill3)>";
		// TODO note parentheses and newlines removed
		assertEquals("<sqoop-6e44 'FAILED'->kill1 && sqoop-e07a 'FAILED'->kill1 || sqoop-035f 'FAILED'->kill2 && sqoop-9408 'FAILED'->kill2 && sqoop-a6e0 'FAILED'->kill2 && sqoop-e522 'FAILED'->kill2 && shell-b521 'FAILED'->kill2 || sqoop-6420 'FAILED'->kill3>",parse(spec).toDSL());
	}

	@Test
	public void transitionsAcrossSplit() {
		// As the boo references are across a split, should be different Boo instances
		String spec = "<foo 'failed'->boo || bar 'failed'->boo>";
		// There should be only one 'boo' here, that the two things in the same flow will map to.
		assertGraph("[0:START][1:foo][2:boo][3:bar][4:boo][5:END]"+
		            "[0-1]['failed':1-2][0-3]['failed':3-4][1-5][2-5][3-5][4-5]",spec);
	}

	@Test
	public void forwardReferenceInFlow() {
		String spec = "aaa 'failed'->:foo && bbb && foo: ccc && ddd";
		assertGraph("[0:START][1:aaa][2:bbb][3:ccc][4:ddd][5:END]"+
				    "[0-1][1-2]['failed':1-3][2-3][3-4][4-5]", spec);
		checkDSLToGraphAndBackToDSL(spec);
	}

	@Test
	public void splitWithSimilarTransitions() {
		// Two instances of boo in the graph, one for each 'branch'
		String spec = "<foo 'failed'->boo 'error'->boo || bar 'failed'->boo>";
		assertGraph("[0:START][1:foo][2:boo][3:bar][4:boo][5:END]"+
				    "[0-1]['failed':1-2]['error':1-2][0-3]['failed':3-4][1-5][2-5][3-5][4-5]", spec);
		checkDSLToGraphAndBackToDSL(spec);
	}

	@Test
	public void complexSplitWithTransitionsToCommonApps() {
		String spec = "<foo 'failed'->boo 'error'->boo && goo || bar 'failed'->boo>";
		assertGraph("[0:START][1:foo][2:boo][3:goo][4:bar][5:boo][6:END]"+
		            "[0-1]['failed':1-2]['error':1-2][1-3][0-4]['failed':4-5][3-6][2-6][4-6][5-6]", spec);
		checkDSLToGraphAndBackToDSL(spec);
	}

	@Test
	public void multiTransitionToSameTarget() {
		String spec = "foo 'failed'->bbb && bar 'failed'->bbb";
		assertGraph("[0:START][1:foo][2:bbb][3:bar][4:END][0-1]['failed':1-2][1-3]['failed':3-2][3-4][2-4]", spec);
		checkDSLToGraphAndBackToDSL(spec);
	}

	@Test
	public void multiTransitionOnSameJobToSameTarget() {
		String spec = "foo 'failed'->bbb 'error'->bbb && bar 'failed'->bbb";
		assertGraph("[0:START][1:foo][2:bbb][3:bar][4:END][0-1]['failed':1-2]['error':1-2][1-3]['failed':3-2][3-4][2-4]", spec);
		checkDSLToGraphAndBackToDSL(spec);
	}
	
	@Test
	public void extraneousDataError() {
		String jobSpecification = "<a || b> rubbish";
		checkForParseError(jobSpecification, DSLMessage.MORE_INPUT, 9, "rubbish");
	}

	@Test
	public void incorrectTransition() {
		checkForParseError("foo ||->bar", DSLMessage.MORE_INPUT, 4, "||");
	}
	
	// --

	private ComposedTaskParser getParser() {
		return new ComposedTaskParser();
	}

	private ComposedTaskNode parse(String dsltext) {
		ComposedTaskNode ctn = getParser().parse("test", dsltext, true);
		return ctn;
	}

	private ComposedTaskNode parse(String dsltext, boolean validate) {
		ComposedTaskNode ctn = getParser().parse("test", dsltext,validate);
		return ctn;
	}

	private void assertToken(TokenKind kind, String string, int start, int end, Token t) {
		assertEquals(kind, t.kind);
		assertEquals(string, t.getKind().hasPayload() ? t.stringValue() : new String(t.getKind().getTokenChars()));
		assertEquals(start, t.startPos);
		assertEquals(end, t.endPos);
	}

	private void assertTokens(Tokens tokens, TokenKind... expectedKinds) {
		for (int i=0;i<expectedKinds.length;i++) {
			assertEquals(expectedKinds[i],tokens.next().getKind());
		}
	}

	private void assertTaskApp(LabelledComposedTaskNode node, String taskAppName) {
		assertTrue(node.isTaskApp());
		assertEquals(((TaskAppNode)node).getName(),taskAppName);
	}
	
	private void assertFlow(LabelledComposedTaskNode node, String... expectedApps) {
		assertTrue(node instanceof FlowNode);
		FlowNode flow = (FlowNode)node;
		List<LabelledComposedTaskNode> series = flow.getSeries();
		assertEquals(expectedApps.length,series.size());
		assertEquals(expectedApps.length,flow.getSeriesLength());
		for (int a=0;a<expectedApps.length;a++) {
			assertTaskApp(series.get(a),expectedApps[a]);
		}
	}

	private void assertSplit(LabelledComposedTaskNode node, String... expectedApps) {
		assertTrue(node instanceof SplitNode);
		SplitNode split = (SplitNode)node;
		List<LabelledComposedTaskNode> series = split.getSeries();
		assertEquals(expectedApps.length,series.size());
		assertEquals(expectedApps.length,split.getSeriesLength());
		for (int a=0;a<expectedApps.length;a++) {
			FlowNode f = (FlowNode)series.get(a);
			assertEquals(1,f.getSeriesLength());
			assertTaskApp(f.getSeriesElement(0),expectedApps[a]);
		}
	}


	private ParseException checkForParseError(String dsl, DSLMessage msg, int pos, Object... inserts) {
		try {
			ComposedTaskNode ctn = parse(dsl);
			fail("expected to fail but parsed " + ctn.stringify());
			return null;
		}
		catch (ParseException e) {
			System.out.println(e);
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

	private void assertApps(Set<String> taskApps, String... expectedTaskAppNames) {
		assertEquals("Expected "+expectedTaskAppNames.length+" but was "+taskApps.size()+": "+taskApps,expectedTaskAppNames.length,taskApps.size());
		Set<String> set2 = new HashSet<String>(taskApps);
		for (String expectedTaskAppName: expectedTaskAppNames) {
			if (!set2.contains(expectedTaskAppName)) {
				fail("Expected set "+taskApps+" does not contain app '"+expectedTaskAppName+"'");
			}
			set2.remove(expectedTaskAppName);
		}
		if (set2.size()!=0) {
			fail("Unexpected app"+(set2.size()>1?"s":"")+" :"+set2);
		}
	}
	
	private void checkDSLToGraphAndBackToDSL(String specification) {
		ComposedTaskNode ctn = parse(specification);
		Graph graph = ctn.toGraph();
		assertEquals(specification, graph.toDSLText());
	}

	private void assertGraph(String expectedGraph, String dsl) {
		ComposedTaskNode ctn = parse(dsl);
		Graph graph = ctn.toGraph();
		assertEquals(expectedGraph,graph.toVerboseString());
	}

	private void assertParseAndBackToDSL(String definition) {
		assertEquals(definition,parse(definition).toDSL());
	}

	static class TestVisitor extends ComposedTaskVisitor {
		private StringBuilder s = new StringBuilder();
		
		public void reset() {
			s = new StringBuilder();
		}
		
		public String getString() {
			return s.toString().trim();
		}
		
		@Override
		public boolean preVisitSequence(LabelledComposedTaskNode firstNode, int sequenceNumber) {
			s.append(">SN["+(firstNode.hasLabel()?firstNode.getLabelString()+": ":"")+sequenceNumber+"] ");
			return true;
		}
		
		@Override
		public void postVisitSequence(LabelledComposedTaskNode firstNode, int sequenceNumber) {
			s.append("<SN["+sequenceNumber+"] ");
		}
		
		@Override
		public boolean preVisit(FlowNode flow) {
			s.append(">F ");
			return true;
		}

		@Override
		public void visit(FlowNode flow) {
			s.append("=F"+(flow.hasLabel()?"["+flow.getLabelString()+":]":"")+" ");
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
			s.append("=S"+(split.hasLabel()?"["+split.getLabelString()+":]":"")+" ");
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
			s.append("=TA["+(taskApp.hasLabel()?taskApp.getLabelString()+": ":"")+taskApp.getName()+"] ");
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
			s.append("=T["+
					(transition.isExitCodeCheck()?transition.getStatusToCheck():"'"+transition.getStatusToCheck()+"'")+
					"->"+
					(transition.isTargetApp()?transition.getTargetApp():":"+transition.getTargetLabel())+"] ");
		}
		
		@Override
		public void postVisit(TransitionNode transition) {
			s.append("<T ");
		}
		
	}
}
