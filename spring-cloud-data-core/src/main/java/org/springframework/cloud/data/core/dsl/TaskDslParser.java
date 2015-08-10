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
package org.springframework.cloud.data.core.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Minella
 */
public class TaskDslParser {

	private String expressionString;

	private List<Token> tokenStream;

	private int tokenStreamLength;

	private int tokenStreamPointer; // Current location in the token stream when

	// processing tokens

	private int lastGoodPoint;

	/**
	 * Parse a task definition without supplying the task name up front.
	 * The task name may be embedded in the definition.
	 * For example: <code>myTask = filejdbc</code>
	 *
	 * @return the AST for the parsed stream
	 */
	public ModuleNode parse(String stream) {
		return parse(null, stream);
	}

	/**
	 * Parse a task definition.
	 *
	 * @return the AST for the parsed task
	 * @throws ParseException
	 */
	public ModuleNode parse(String name, String task) {
		this.expressionString = task;
		Tokenizer tokenizer = new Tokenizer(expressionString);

		tokenStream = tokenizer.getTokens();
		tokenStreamLength = tokenStream.size();
		tokenStreamPointer = 0;
		ModuleNode ast = eatModule();

		// Check the task name, however it was specified
		if (ast.getName() != null && !isValidTaskName(ast.getName())) {
			throw new ParseException(ast.getName(), 0, DSLMessage.ILLEGAL_TASK_NAME, ast.getName());
		}
		if (name != null && !isValidTaskName(name)) {
			throw new ParseException(name, 0, DSLMessage.ILLEGAL_TASK_NAME, name);
		}
		if (moreTokens()) {
			throw new ParseException(this.expressionString, peekToken().startPos, DSLMessage.MORE_INPUT,
					toString(nextToken()));
		}

		return ast;
	}

	// module: [label':']? identifier (moduleArguments)*
	private ModuleNode eatModule() {
		Token label = null;
		Token name = nextToken();
		if (!name.isKind(TokenKind.IDENTIFIER)) {
			raiseException(name.startPos, DSLMessage.EXPECTED_MODULENAME, name.data != null ? name.data
					: new String(name.getKind().tokenChars));
		}
		if (peekToken(TokenKind.COLON)) {
			if (!isNextTokenAdjacent()) {
				raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_BETWEEN_LABEL_NAME_AND_COLON);
			}
			nextToken(); // swallow colon
			label = name;
			name = eatToken(TokenKind.IDENTIFIER);
		}
		Token moduleName = name;
		checkpoint();
		ArgumentNode[] args = maybeEatModuleArgs();
		int startpos = label != null ? label.startPos : moduleName.startPos;
		return new ModuleNode(toLabelNode(label), moduleName.data, startpos, moduleName.endPos, args);
	}

	private LabelNode toLabelNode(Token label) {
		if (label == null) {
			return null;
		}
		return new LabelNode(label.data, label.startPos, label.endPos);
	}

	// moduleArguments : DOUBLE_MINUS identifier(name) EQUALS identifier(value)
	private ArgumentNode[] maybeEatModuleArgs() {
		List<ArgumentNode> args = null;
		if (peekToken(TokenKind.DOUBLE_MINUS) && isNextTokenAdjacent()) {
			raiseException(peekToken().startPos, DSLMessage.EXPECTED_WHITESPACE_AFTER_MODULE_BEFORE_ARGUMENT);
		}
		while (peekToken(TokenKind.DOUBLE_MINUS)) {
			Token dashDash = nextToken(); // skip the '--'
			if (peekToken(TokenKind.IDENTIFIER) && !isNextTokenAdjacent()) {
				raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_BEFORE_ARG_NAME);
			}
			List<Token> argNameComponents = eatDottedName();
			if (peekToken(TokenKind.EQUALS) && !isNextTokenAdjacent()) {
				raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_BEFORE_ARG_EQUALS);
			}
			eatToken(TokenKind.EQUALS);
			if (peekToken(TokenKind.IDENTIFIER) && !isNextTokenAdjacent()) {
				raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_BEFORE_ARG_VALUE);
			}
			// Process argument value:
			Token t = peekToken();
			String argValue = eatArgValue();
			checkpoint();
			if (args == null) {
				args = new ArrayList<ArgumentNode>();
			}
			args.add(new ArgumentNode(data(argNameComponents), argValue, dashDash.startPos, t.endPos));
		}
		return args == null ? null : args.toArray(new ArgumentNode[args.size()]);
	}

	// argValue: identifier | literal_string
	private String eatArgValue() {
		Token t = nextToken();
		String argValue = null;
		if (t.getKind() == TokenKind.IDENTIFIER) {
			argValue = t.data;
		}
		else if (t.getKind() == TokenKind.LITERAL_STRING) {
			String quotesUsed = t.data.substring(0, 1);
			argValue = t.data.substring(1, t.data.length() - 1).replace(quotesUsed+quotesUsed, quotesUsed);
		}
		else {
			raiseException(t.startPos, DSLMessage.EXPECTED_ARGUMENT_VALUE, t.data);
		}
		return argValue;
	}

	private Token eatToken(TokenKind expectedKind) {
		Token t = nextToken();
		if (t == null) {
			raiseException(expressionString.length(), DSLMessage.OOD);
		}
		if (t.kind != expectedKind) {
			raiseException(t.startPos, DSLMessage.NOT_EXPECTED_TOKEN, expectedKind.toString().toLowerCase(),
					t.getKind().toString().toLowerCase() + (t.data == null ? "" : "(" + t.data + ")"));
		}
		return t;
	}

	private boolean peekToken(TokenKind desiredTokenKind) {
		return peekToken(desiredTokenKind, false);
	}

	private boolean lookAhead(int distance, TokenKind desiredTokenKind) {
		if ((tokenStreamPointer + distance) >= tokenStream.size()) {
			return false;
		}
		Token t = tokenStream.get(tokenStreamPointer + distance);
		if (t.kind == desiredTokenKind) {
			return true;
		}
		return false;
	}

	private List<Token> eatDottedName() {
		return eatDottedName(DSLMessage.NOT_EXPECTED_TOKEN);
	}

	/**
	 * Consumes and returns (identifier [DOT identifier]*) as long as they're adjacent.
	 *
	 * @param error the kind of error to report if input is ill-formed
	 */
	private List<Token> eatDottedName(DSLMessage error) {
		List<Token> result = new ArrayList<Token>(3);
		Token name = nextToken();
		if (!name.isKind(TokenKind.IDENTIFIER)) {
			raiseException(name.startPos, error, name.data != null ? name.data
					: new String(name.getKind().tokenChars));
		}
		result.add(name);
		while (peekToken(TokenKind.DOT)) {
			if (!isNextTokenAdjacent()) {
				raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_IN_DOTTED_NAME);
			}
			result.add(nextToken()); // consume dot
			if (peekToken(TokenKind.IDENTIFIER) && !isNextTokenAdjacent()) {
				raiseException(peekToken().startPos, DSLMessage.NO_WHITESPACE_IN_DOTTED_NAME);
			}
			result.add(eatToken(TokenKind.IDENTIFIER));
		}
		return result;
	}

	/**
	 * Verify the supplied name is a valid task name. Valid stream names must follow the same rules as java
	 * identifiers, with the additional option to use a hyphen ('-') after the first character.
	 *
	 * @param taskname the name to validate
	 * @return true if name is valid
	 */
	public static boolean isValidTaskName(String taskname) {
		if (taskname.length() == 0) {
			return false;
		}
		if (!Character.isJavaIdentifierStart(taskname.charAt(0))) {
			return false;
		}
		for (int i = 1, max = taskname.length(); i < max; i++) {
			char ch = taskname.charAt(i);
			if (!(Character.isJavaIdentifierPart(ch) || ch == '-')) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return the concatenation of the data of many tokens.
	 */
	private String data(Iterable<Token> many) {
		StringBuilder result = new StringBuilder();
		for (Token t : many) {
			if (t.getKind().hasPayload()) {
				result.append(t.data);
			}
			else {
				result.append(t.getKind().tokenChars);
			}
		}
		return result.toString();
	}

	private boolean peekToken(TokenKind desiredTokenKind, boolean consumeIfMatched) {
		if (!moreTokens()) {
			return false;
		}
		Token t = peekToken();
		if (t.kind == desiredTokenKind) {
			if (consumeIfMatched) {
				tokenStreamPointer++;
			}
			return true;
		}
		else {
			return false;
		}
	}

	private boolean moreTokens() {
		return tokenStreamPointer < tokenStream.size();
	}

	private Token nextToken() {
		if (tokenStreamPointer >= tokenStreamLength) {
			raiseException(expressionString.length(), DSLMessage.OOD);
		}
		return tokenStream.get(tokenStreamPointer++);
	}

	private boolean isNextTokenAdjacent() {
		if (tokenStreamPointer >= tokenStreamLength) {
			return false;
		}
		Token last = tokenStream.get(tokenStreamPointer - 1);
		Token next = tokenStream.get(tokenStreamPointer);
		return next.startPos == last.endPos;
	}

	private Token peekToken() {
		if (tokenStreamPointer >= tokenStreamLength) {
			return null;
		}
		return tokenStream.get(tokenStreamPointer);
	}

	private void raiseException(int pos, DSLMessage message, Object... inserts) {
		throw new CheckPointedParseException(expressionString, pos, tokenStreamPointer, lastGoodPoint,
				tokenStream, message, inserts);
	}

	private void checkpoint() {
		lastGoodPoint = tokenStreamPointer;
	}

	private String toString(Token t) {
		if (t.getKind().hasPayload()) {
			return t.stringValue();
		}
		else {
			return new String(t.kind.getTokenChars());
		}
	}

}
