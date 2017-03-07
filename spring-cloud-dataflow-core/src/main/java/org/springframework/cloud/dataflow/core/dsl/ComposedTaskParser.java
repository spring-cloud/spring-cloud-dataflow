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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.util.Assert;

import static org.springframework.cloud.dataflow.core.dsl.TokenKind.*;

/**
 * Parse a composed task specification.
 *
 * @author Andy Clement
 */
public class ComposedTaskParser {

	private Tokens tokens;

	public ComposedTaskParser() {
	}

	/**
	 * Parse a composed task definition into an abstract syntax tree (AST).
	 *
	 * @param composedTaskName the name of the composed task
	 * @param composedTaskDefinition the textual composed task definition
	 * @return the AST for the parsed composed task
	 * @throws ComposedTaskValidationException if any problems occur during parsing
	 */
	public ComposedTaskNode parse(String composedTaskName, String composedTaskDefinition) {
		return parse(composedTaskName, composedTaskDefinition, true);
	}
	
	/**
	 * Parse a composed task definition into an abstract syntax tree (AST).
	 *
	 * @param composedTaskName the name of the composed task
	 * @param composedTaskDefinition the textual composed task definition
	 * @param validate if true then extra validation will be performed on the AST
	 * @return the AST for the parsed composed task
	 * @throws ComposedTaskValidationException if any problems occur during parsing
	 */
	public ComposedTaskNode parse(String composedTaskName, String composedTaskDefinition, boolean validate) {
		Assert.notNull(composedTaskName, "composedTaskName must be a non-null string");
		Assert.notNull(composedTaskDefinition, "composedTaskDefinition must be a non-null string");
		composedTaskDefinition = composedTaskDefinition.trim();
		if (composedTaskDefinition.length() == 0) {
			return new ComposedTaskNode(composedTaskName, composedTaskDefinition, Collections.emptyList());
		}
		tokens = new ComposedTaskTokenizer().getTokens(composedTaskDefinition);
		List<LabelledComposedTaskNode> sequence = eatSequence();
		ComposedTaskNode composedTaskNode = new ComposedTaskNode(composedTaskName, composedTaskDefinition, sequence);
		if (tokens.hasNext()) {
			tokens.raiseException(tokens.peek().startPos, DSLMessage.COMPOSED_TASK_MORE_INPUT,
					toString(tokens.next()));
		}
		if (validate) {
			ComposedTaskValidatorVisitor validator = new ComposedTaskValidatorVisitor();
			composedTaskNode.accept(validator);
			List<ComposedTaskValidationProblem> problems = validator.getProblems();
			if (!problems.isEmpty()) {
				throw new ComposedTaskValidationException(composedTaskNode, problems);
			}
		}
		return composedTaskNode;
	}

	/**
	 * Process a potential sequence of composed task nodes. Sequences are separated by
	 * newlines or semi-colons.
	 */
	private List<LabelledComposedTaskNode> eatSequence() {
		List<LabelledComposedTaskNode> sequence = new ArrayList<>();
		sequence.add(parseComposedTaskNode());
		while (tokens.hasNext() && (nextTokenIsOnNewline() || maybeEat(SEMICOLON))) {
			sequence.add(parseComposedTaskNode());
		}
		return sequence;
	}

	private boolean nextTokenIsOnNewline() {
		Token nextToken = peek();
		Token lastToken = peek(-1);
		int lastTokenLine = tokens.getLine(lastToken);
		int nextTokenLine = tokens.getLine(nextToken);
		return nextTokenLine>lastTokenLine;
	}
	
	private Token maybeEatLabel() {
		if (peek(IDENTIFIER) && (peek(1)!=null && peek(1).isKind(COLON))) {
			Token labelToken = eat();
			eat(COLON);
			return labelToken;
		}
		return null;
	}
	
	private LabelledComposedTaskNode parseComposedTaskNode() {
		// Handle (...)
		if (maybeEat(OPEN_PAREN)) {
			LabelledComposedTaskNode node = parseComposedTaskNode();
			eat(CLOSE_PAREN);
			return node;
		}
		Token label = maybeEatLabel();
		if (label != null) {
			Token secondLabel;
			if (peek(OPEN_PAREN)) {
				tokens.raiseException(peek().startPos, DSLMessage.COMPOSED_TASK_NO_LABELS_ON_PARENS);
			} 
			else if ((secondLabel=maybeEatLabel())!=null) {
				tokens.raiseException(secondLabel.startPos, DSLMessage.COMPOSED_TASK_NO_DOUBLE_LABELS);
			}
		}
		// Handle a split < ... >
		if (peek(LT)) {
			LabelledComposedTaskNode node = parseSplit();
			node.setLabel(label);
			// is the split part of a flow? "<..> && b"
			return parseFlow(node);
		}
		TaskAppNode app = eatTaskApp();
		app.setLabel(label);
		// Handle a potential flow "a && b"
		return parseFlow(app);
	}

	private FlowNode parseFlow(LabelledComposedTaskNode firstNodeInFlow) {
		List<LabelledComposedTaskNode> nodes = new ArrayList<>();
		nodes.add(firstNodeInFlow);
		while (maybeEat(ANDAND)) {
			LabelledComposedTaskNode nextNode = parseComposedTaskNode();
			// If nextNode is a Flow node, merge it with this one
			if (nextNode instanceof FlowNode) {
				nodes.addAll(nextNode.getSeries());
			}
			else {
				nodes.add(nextNode);
			}
		}
		FlowNode f = new FlowNode(nodes);
		f.setLabel(firstNodeInFlow.getLabel());
		return f;
	}

	// '<' jobs ['||' jobs]+ '>'
	private LabelledComposedTaskNode parseSplit() {
		List<LabelledComposedTaskNode> flows = new ArrayList<>();
		Token startSplit = eat(LT);
		flows.add(parseComposedTaskNode());
		while (maybeEat(OROR)) {
			flows.add(parseComposedTaskNode());
		}
		Token endSplit = eat(GT);
		return new SplitNode(startSplit.startPos, endSplit.endPos, flows);
	}

	// App1
	// App1 0 -> App2
	// App1 0 -> App2 1 -> App3
	// App1 'a' -> App2
	// App1 'a' -> App2 '*' -> App3
	private TaskAppNode eatTaskApp() {
		Token taskName = eat(IDENTIFIER);
		tokens.checkpoint();
		List<TransitionNode> transitions = maybeEatTransitions();
		return new TaskAppNode(taskName,transitions);
	}

	private List<TransitionNode> maybeEatTransitions() {
		List<TransitionNode> transitions = new ArrayList<>();
		Token transitionOn = null;
		while (true) {
			if (peek(ARROW)) {
				tokens.raiseException(peek().startPos,DSLMessage.COMPOSED_TASK_ARROW_SHOULD_BE_PRECEDED_BY_CODE);
				break;
			}
			Token possibleArrow = peek(+1);
			if (possibleArrow == null || possibleArrow.getKind()!=ARROW) {
				break;
			}
			transitionOn = peek();
			if (transitionOn == null || 
				(transitionOn.getKind()!=IDENTIFIER && 
				 transitionOn.getKind()!=LITERAL_STRING &&
				 transitionOn.getKind()!=STAR)) {
				break;
			}
			eat();
			if (!maybeEat(ARROW)) {
				tokens.raiseException(transitionOn.startPos, DSLMessage.COMPOSED_TASK_MISSING_TRANSITION_ARROW);
			}
			TransitionNode t = null;
			if (maybeEat(COLON)) {
				Token labelReference = eat(IDENTIFIER);
				t = TransitionNode.toLabelReference(transitionOn, labelReference);
			}
			else {
				Token taskName = eat(IDENTIFIER);
				t = TransitionNode.toAnotherTask(transitionOn, taskName);
			}
			if (t.isExitCodeCheck() && !t.getStatusToCheck().equals("*")) {
				// If an exit code check, must be a number
				try {
					Integer.parseInt(t.getStatusToCheck());
				} catch (NumberFormatException nfe) {
					tokens.raiseException(transitionOn.startPos, DSLMessage.COMPOSED_TASK_UNQUOTED_TRANSITION_CHECK_MUST_BE_NUMBER, t.getStatusToCheck());
				}
			}
			transitions.add(t);
		}		
		return transitions;
	}

	/**
	 * Show the parsing progress in the output string.
	 */
	@Override
	public String toString() {
		// Only state of the token processing is interesting:
		return tokens.toString();
	}

	private boolean maybeEat(TokenKind desiredTokenKind) {
		if (tokens.peek(desiredTokenKind)) {
			tokens.eat(desiredTokenKind);
			return true;
		}
		else {
			return false;
		}
	}
	
	private Token eat(TokenKind tokenKind) {
		return tokens.eat(tokenKind);
	}
	
	private Token eat() {
		return tokens.eat();
	}
	
	private Token peek() {
		return tokens.peek();
	}

	private Token peek(int howFarAhead) {
		return tokens.peek(howFarAhead);
	}

	private boolean peek(TokenKind desiredTokenKind) {
		return tokens.peek(desiredTokenKind);
	}
	
	/**
	 * Convert a token into its String representation.
	 *
	 * @param t the token
	 * @return a string representation of the supplied token
	 */
	private static String toString(Token t) {
		if (t.getKind().hasPayload()) {
			return t.stringValue();
		}
		else {
			return new String(t.kind.getTokenChars());
		}
	}

}
