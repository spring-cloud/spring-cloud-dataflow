/*
 * Copyright 2017 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.util.Assert;

import static org.springframework.cloud.dataflow.core.dsl.TokenKind.ANDAND;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.ARROW;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.CLOSE_PAREN;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.COLON;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.DOUBLE_MINUS;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.GT;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.IDENTIFIER;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.LITERAL_STRING;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.LT;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.OPEN_PAREN;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.OROR;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.SEMICOLON;
import static org.springframework.cloud.dataflow.core.dsl.TokenKind.STAR;

/**
 * Parse a single or composed task specification.
 *
 * @author Andy Clement
 */
public class TaskParser extends AppParser {

	public final static ArgumentNode[] NO_ARGUMENTS = new ArgumentNode[0];

	/**
	 * Task name.
	 */
	private final String taskName;

	/**
	 * Task DSL text.
	 */
	private final String taskDefinition;

	/**
	 * Controls usage mode for the parser. If true then apps can take arguments, if false
	 * then app definitions are expected which take no arguments (currently).
	 */
	private final boolean inAppMode;

	/**
	 * If true, validation of the Ast will be performed after parsing.
	 */
	private final boolean validate;

	/**
	 * Parse a task definition into an abstract syntax tree (AST).
	 *
	 * @param taskName the name of the task
	 * @param taskDefinition the textual task definition
	 * @param inAppMode if true then expect apps with arguments otherwise expect app
	 * definition references
	 * @param validate if true then extra validation will be performed on the AST
	 */
	public TaskParser(String taskName, String taskDefinition, boolean inAppMode, boolean validate) {
		super(new TaskTokenizer().getTokens(taskDefinition));
		Assert.notNull(taskName, "taskName must be a non-null string");
		Assert.notNull(taskDefinition, "taskDefinition must be a non-null string");
		this.taskName = taskName;
		this.inAppMode = inAppMode;
		this.taskDefinition = taskDefinition.trim();
		this.validate = validate;
	}

	public TaskNode parse() {
		List<LabelledTaskNode> sequences = eatSequences();
		if (taskName != null && !isValidName(taskName)) {
			throw new ParseException(taskName, 0, DSLMessage.ILLEGAL_TASK_NAME, taskName);
		}
		TaskNode taskNode = new TaskNode(taskName, taskDefinition, sequences, inAppMode);
		if (getTokens().hasNext()) {
			getTokens().raiseException(getTokens().peek().startPos, DSLMessage.TASK_MORE_INPUT,
					toString(getTokens().next()));
		}
		if (validate) {
			TaskValidatorVisitor validator = new TaskValidatorVisitor();
			taskNode.accept(validator);
			List<TaskValidationProblem> problems = validator.getProblems();
			if (!problems.isEmpty()) {
				throw new TaskValidationException(taskNode, problems);
			}
		}
		return taskNode;
	}

	/**
	 * Process a potential sequence of task definitions. Sequences are separated by
	 * newlines or semi-colons.
	 */
	private List<LabelledTaskNode> eatSequences() {
		List<LabelledTaskNode> sequence = new ArrayList<>();
		sequence.add(parseTaskNode());
		while (getTokens().hasNext() && (nextTokenIsOnNewline() || maybeEat(SEMICOLON))) {
			sequence.add(parseTaskNode());
		}
		return sequence;
	}

	private boolean nextTokenIsOnNewline() {
		Token nextToken = peek();
		Token lastToken = peek(-1);
		int lastTokenLine = getTokens().getLine(lastToken);
		int nextTokenLine = getTokens().getLine(nextToken);
		return nextTokenLine > lastTokenLine;
	}

	private Token maybeEatLabel() {
		if (peek(IDENTIFIER) && (peek(1) != null && peek(1).isKind(COLON))) {
			Token labelToken = eat();
			eat(COLON);
			return labelToken;
		}
		return null;
	}

	private LabelledTaskNode parseTaskNode() {
		// Handle (...)
		if (maybeEat(OPEN_PAREN)) {
			LabelledTaskNode node = parseTaskNode();
			eat(CLOSE_PAREN);
			return node;
		}
		Token label = maybeEatLabel();
		if (label != null) {
			Token secondLabel;
			if (peek(OPEN_PAREN)) {
				getTokens().raiseException(peek().startPos, DSLMessage.TASK_NO_LABELS_ON_PARENS);
			}
			else if ((secondLabel = maybeEatLabel()) != null) {
				getTokens().raiseException(secondLabel.startPos, DSLMessage.TASK_NO_DOUBLE_LABELS);
			}
		}
		// Handle a split < ... >
		if (peek(LT)) {
			LabelledTaskNode node = parseSplit();
			node.setLabel(label);
			// is the split part of a flow? "<..> && b"
			return parseFlow(node);
		}
		TaskAppNode app = eatTaskApp(true);
		app.setLabel(label);
		// Handle a potential flow "a && b"
		return parseFlow(app);
	}

	private FlowNode parseFlow(LabelledTaskNode firstNodeInFlow) {
		List<LabelledTaskNode> nodes = new ArrayList<>();
		nodes.add(firstNodeInFlow);
		while (maybeEat(ANDAND)) {
			LabelledTaskNode nextNode = parseTaskNode();
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
	private LabelledTaskNode parseSplit() {
		List<LabelledTaskNode> flows = new ArrayList<>();
		Token startSplit = eat(LT);
		flows.add(parseTaskNode());
		while (maybeEat(OROR)) {
			flows.add(parseTaskNode());
		}
		Token endSplit = eat(GT);
		return new SplitNode(startSplit.startPos, endSplit.endPos, flows);
	}

	// App1
	// App1 0 -> App2
	// App1 0 -> App2 1 -> App3
	// App1 'a' -> App2
	// App1 'a' -> App2 '*' -> App3
	// App1 --p1=v1
	// App1 --p1=v1 'foo'-> Bar --p1=v2
	private TaskAppNode eatTaskApp(boolean transitionsAllowed) {
		Token name = eat();
		if (!name.isKind(TokenKind.IDENTIFIER)) {
			getTokens().raiseException(name.startPos, DSLMessage.EXPECTED_APPNAME,
					name.data != null ? name.data : new String(name.getKind().tokenChars));
		}
		getTokens().checkpoint();
		ArgumentNode[] arguments = (inAppMode ? maybeEatAppArgs() : null);
		if (!inAppMode && peek(DOUBLE_MINUS)) {
			getTokens().raiseException(peek().startPos, DSLMessage.TASK_ARGUMENTS_NOT_ALLOWED_UNLESS_IN_APP_MODE);
		}
		List<TransitionNode> transitions = transitionsAllowed ? maybeEatTransitions() : Collections.emptyList();
		return new TaskAppNode(name, arguments, transitions);
	}

	/**
	 * Return an array of {@link ArgumentNode} and advance the token position if the next
	 * token(s) contain arguments.
	 * <p>
	 * Expected format:
	 * {@code appArguments : DOUBLE_MINUS identifier(name) EQUALS identifier(value)}
	 *
	 * @return array of arguments or {@code null} if the next token(s) do not contain
	 * arguments
	 */
	private ArgumentNode[] maybeEatAppArgs() {
		Tokens tokens = getTokens();
		List<ArgumentNode> args = null;
		if (tokens.peek(TokenKind.DOUBLE_MINUS) && tokens.isNextAdjacent()) {
			tokens.raiseException(tokens.peek().startPos, DSLMessage.EXPECTED_WHITESPACE_AFTER_APP_BEFORE_ARGUMENT);
		}
		while (tokens.peek(TokenKind.DOUBLE_MINUS)) {
			Token dashDash = tokens.next(); // skip the '--'
			if (tokens.peek(TokenKind.IDENTIFIER) && !tokens.isNextAdjacent()) {
				tokens.raiseException(tokens.peek().startPos, DSLMessage.NO_WHITESPACE_BEFORE_ARG_NAME);
			}
			List<Token> argNameComponents = eatDottedName();
			if (tokens.peek(TokenKind.EQUALS) && !tokens.isNextAdjacent()) {
				tokens.raiseException(tokens.peek().startPos, DSLMessage.NO_WHITESPACE_BEFORE_ARG_EQUALS);
			}
			tokens.eat(TokenKind.EQUALS);
			if (tokens.peek(TokenKind.IDENTIFIER) && !tokens.isNextAdjacent()) {
				tokens.raiseException(tokens.peek().startPos, DSLMessage.NO_WHITESPACE_BEFORE_ARG_VALUE);
			}
			// Process argument value:
			Token t = tokens.peek();
			String argValue = eatArgValue();
			tokens.checkpoint();
			if (args == null) {
				args = new ArrayList<ArgumentNode>();
			}
			args.add(new ArgumentNode(toData(argNameComponents), argValue, dashDash.startPos, t.endPos));
		}
		return args == null ? NO_ARGUMENTS : args.toArray(new ArgumentNode[args.size()]);
	}

	private List<TransitionNode> maybeEatTransitions() {
		List<TransitionNode> transitions = new ArrayList<>();
		Token transitionOn = null;
		while (true) {
			if (peek(ARROW)) {
				getTokens().raiseException(peek().startPos, DSLMessage.TASK_ARROW_SHOULD_BE_PRECEDED_BY_CODE);
				break;
			}
			Token possibleArrow = peek(+1);
			if (possibleArrow == null || possibleArrow.getKind() != ARROW) {
				break;
			}
			transitionOn = peek();
			if (transitionOn == null || (transitionOn.getKind() != IDENTIFIER
					&& transitionOn.getKind() != LITERAL_STRING && transitionOn.getKind() != STAR)) {
				break;
			}
			eat();
			if (!maybeEat(ARROW)) {
				getTokens().raiseException(transitionOn.startPos, DSLMessage.TASK_MISSING_TRANSITION_ARROW);
			}
			TransitionNode t = null;
			if (maybeEat(COLON)) {
				Token labelReference = eat(IDENTIFIER);
				t = TransitionNode.toLabelReference(transitionOn, labelReference);
			}
			else {
				Token label = maybeEatLabel();
				TaskAppNode task = eatTaskApp(false);
				task.setLabel(label);
				t = TransitionNode.toAnotherTask(transitionOn, task);
			}
			if (t.isExitCodeCheck() && !t.getStatusToCheck().equals("*")) {
				// If an exit code check, must be a number
				try {
					Integer.parseInt(t.getStatusToCheck());
				}
				catch (NumberFormatException nfe) {
					getTokens().raiseException(transitionOn.startPos,
							DSLMessage.TASK_UNQUOTED_TRANSITION_CHECK_MUST_BE_NUMBER, t.getStatusToCheck());
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
		return getTokens().toString();
	}

	private boolean maybeEat(TokenKind desiredTokenKind) {
		if (getTokens().peek(desiredTokenKind)) {
			getTokens().eat(desiredTokenKind);
			return true;
		}
		else {
			return false;
		}
	}

	private Token eat(TokenKind tokenKind) {
		return getTokens().eat(tokenKind);
	}

	private Token eat() {
		return getTokens().eat();
	}

	private Token peek() {
		return getTokens().peek();
	}

	private Token peek(int howFarAhead) {
		return getTokens().peek(howFarAhead);
	}

	private boolean peek(TokenKind desiredTokenKind) {
		return getTokens().peek(desiredTokenKind);
	}

}
