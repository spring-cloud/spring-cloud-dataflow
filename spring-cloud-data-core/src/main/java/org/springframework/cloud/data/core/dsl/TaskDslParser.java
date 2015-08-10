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

	/**
	 * Tokens resulting from task definition parsing.
	 */
	private Tokens tokens;

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
		tokens = new Tokens(task);
		ModuleNode ast = eatModule();

		// Check the task name, however it was specified
		if (ast.getName() != null && !isValidTaskName(ast.getName())) {
			throw new ParseException(ast.getName(), 0, DSLMessage.ILLEGAL_TASK_NAME, ast.getName());
		}
		if (name != null && !isValidTaskName(name)) {
			throw new ParseException(name, 0, DSLMessage.ILLEGAL_TASK_NAME, name);
		}
		if (tokens.more()) {
			tokens.raiseException(tokens.peek().startPos, DSLMessage.MORE_INPUT, toString(tokens.next()));
		}

		return ast;
	}

	// module: [label':']? identifier (moduleArguments)*
	private ModuleNode eatModule() {
		Token label = null;
		Token name = tokens.next();
		if (!name.isKind(TokenKind.IDENTIFIER)) {
			tokens.raiseException(name.startPos, DSLMessage.EXPECTED_MODULENAME, name.data != null ? name.data
					: new String(name.getKind().tokenChars));
		}
		if (tokens.peek(TokenKind.COLON)) {
			if (!tokens.isNextAdjacent()) {
				tokens.raiseException(tokens.peek().startPos, DSLMessage.NO_WHITESPACE_BETWEEN_LABEL_NAME_AND_COLON);
			}
			tokens.next(); // swallow colon
			label = name;
			name = tokens.eat(TokenKind.IDENTIFIER);
		}
		Token moduleName = name;
		tokens.checkpoint();
		ArgumentNode[] args = maybeEatModuleArgs();
		int startPos = label != null ? label.startPos : moduleName.startPos;
		return new ModuleNode(toLabelNode(label), moduleName.data, startPos, moduleName.endPos, args);
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
		if (tokens.peek(TokenKind.DOUBLE_MINUS) && tokens.isNextAdjacent()) {
			tokens.raiseException(tokens.peek().startPos, DSLMessage.EXPECTED_WHITESPACE_AFTER_MODULE_BEFORE_ARGUMENT);
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
			args.add(new ArgumentNode(data(argNameComponents), argValue, dashDash.startPos, t.endPos));
		}
		return args == null ? null : args.toArray(new ArgumentNode[args.size()]);
	}

	// argValue: identifier | literal_string
	private String eatArgValue() {
		Token t = tokens.next();
		String argValue = null;
		if (t.getKind() == TokenKind.IDENTIFIER) {
			argValue = t.data;
		}
		else if (t.getKind() == TokenKind.LITERAL_STRING) {
			String quotesUsed = t.data.substring(0, 1);
			argValue = t.data.substring(1, t.data.length() - 1).replace(quotesUsed+quotesUsed, quotesUsed);
		}
		else {
			tokens.raiseException(t.startPos, DSLMessage.EXPECTED_ARGUMENT_VALUE, t.data);
		}
		return argValue;
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
		Token name = tokens.next();
		if (!name.isKind(TokenKind.IDENTIFIER)) {
			tokens.raiseException(name.startPos, error, name.data != null ? name.data
					: new String(name.getKind().tokenChars));
		}
		result.add(name);
		while (tokens.peek(TokenKind.DOT)) {
			if (!tokens.isNextAdjacent()) {
				tokens.raiseException(tokens.peek().startPos, DSLMessage.NO_WHITESPACE_IN_DOTTED_NAME);
			}
			result.add(tokens.next()); // consume dot
			if (tokens.peek(TokenKind.IDENTIFIER) && !tokens.isNextAdjacent()) {
				tokens.raiseException(tokens.peek().startPos, DSLMessage.NO_WHITESPACE_IN_DOTTED_NAME);
			}
			result.add(tokens.eat(TokenKind.IDENTIFIER));
		}
		return result;
	}

	/**
	 * Verify the supplied name is a valid task name. Valid stream names must follow the same rules as java
	 * identifiers, with the additional option to use a hyphen ('-') after the first character.
	 *
	 * @param taskName the name to validate
	 * @return true if name is valid
	 */
	public static boolean isValidTaskName(String taskName) {
		if (taskName.length() == 0) {
			return false;
		}
		if (!Character.isJavaIdentifierStart(taskName.charAt(0))) {
			return false;
		}
		for (int i = 1, max = taskName.length(); i < max; i++) {
			char ch = taskName.charAt(i);
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

	private String toString(Token t) {
		if (t.getKind().hasPayload()) {
			return t.stringValue();
		}
		else {
			return new String(t.kind.getTokenChars());
		}
	}

}
