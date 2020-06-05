/*
 * Copyright 2015-2020 the original author or authors.
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

/**
 * Parser for generating {@link AppNode AppNodes} from {@link Tokens}. This class may
 * serve as a base for higher level app processing, for instance streams or tasks.
 *
 * @author Andy Clement
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class AppParser {

	/**
	 * Tokens resulting from DSL parsing.
	 */
	protected final Tokens tokens;

	/**
	 * Construct a {@code AppParser} based on the provided {@link Tokens}.
	 *
	 * @param tokens tokens from which to construct this parser
	 */
	public AppParser(Tokens tokens) {
		this.tokens = tokens;
	}

	/**
	 * Return the {@link Tokens} this parser is using.
	 *
	 * @return tokens for this parser
	 */
	public Tokens getTokens() {
		return tokens;
	}

	/**
	 * Return a {@link AppNode} from the next token and advance the token position.
	 * <p>
	 * Expected format: {@code app: [label':']? identifier (appArguments)*}
	 * </p>
	 *
	 * @return an app node resulting from the next token
	 */
	protected AppNode eatApp() {
		Token label = null;
		if (tokens.peek(TokenKind.COLON)) {
			if (tokens.getTokenStream().size() == 1) {
				tokens.raiseException(tokens.peek().startPos, DSLMessage.EXPECTED_STREAM_NAME_AFTER_LABEL_COLON);
			}
		}
		Token name = tokens.next();
		if (!name.isKind(TokenKind.IDENTIFIER)) {
			tokens.raiseException(name.startPos, DSLMessage.EXPECTED_APPNAME,
					name.data != null ? name.data : new String(name.getKind().tokenChars));
		}
		if (tokens.peek(TokenKind.COLON) && tokens.isNextAdjacent()) {
			tokens.next(); // swallow colon
			if (tokens.isNextAdjacent()) {
				tokens.raiseException(tokens.peek().startPos, DSLMessage.EXPECTED_WHITESPACE_AFTER_LABEL_COLON);
			}
			label = name;
			name = tokens.eat(TokenKind.IDENTIFIER);
			if (tokens.peek(TokenKind.COLON) && tokens.isNextAdjacent()) {
				tokens.raiseException(name.startPos, DSLMessage.NO_DOUBLE_LABELS);
			}
		}
		Token appName = name;
		tokens.checkpoint();
		ArgumentNode[] args = eatAppArgs();
		int startPos = label != null ? label.startPos : appName.startPos;
		return makeAppNode(toLabelNode(label), appName.data, startPos, appName.endPos, args);
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
	protected ArgumentNode[] eatAppArgs() {
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
		return args == null ? null : args.toArray(new ArgumentNode[args.size()]);
	}

	/**
	 * Return the argument value from the next token and advance the token position.
	 * <p>
	 * Expected format: {@code argValue: identifier | literal_string}
	 *
	 * @return argument value
	 */
	protected String eatArgValue() {
		Token t = tokens.next();
		String argValue = null;
		if (t.getKind() == TokenKind.IDENTIFIER) {
			argValue = t.data;
		}
		else if (t.getKind() == TokenKind.LITERAL_STRING) {
			String quotesUsed = t.data.substring(0, 1);
			argValue = t.data.substring(1, t.data.length() - 1).replace(quotesUsed + quotesUsed, quotesUsed);
		}
		else {
			tokens.raiseException(t.startPos, DSLMessage.EXPECTED_ARGUMENT_VALUE, t.data);
		}
		return argValue;
	}

	/**
	 * Return an array of {@link Token} that are separated by a dot.
	 * <p>
	 * Expected format: {@code identifier [DOT identifier]*}
	 *
	 * @return array of tokens separated by a dot
	 */
	protected List<Token> eatDottedName() {
		List<Token> result = new ArrayList<Token>(3);
		Token name = tokens.next();
		if (!name.isKind(TokenKind.IDENTIFIER)) {
			tokens.raiseException(name.startPos, DSLMessage.NOT_EXPECTED_TOKEN,
					name.data != null ? name.data : new String(name.getKind().tokenChars));
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
	 * Create a new {@link LabelNode} based on the provided token.
	 *
	 * @param label token containing the label; may be {@code null}
	 * @return new {@code LabelNode} instance based on the provided token, or {@code null}
	 * if the provided token is {@code null}
	 */
	protected LabelNode toLabelNode(Token label) {
		return label == null ? null : new LabelNode(label.data, label.startPos, label.endPos);
	}

	/**
	 * Return the concatenation of the data of multiple tokens.
	 * @param iterable the tokens to concatenate into the result.
	 * @return string containing the data of multiple tokens
	 */
	protected String toData(Iterable<Token> iterable) {
		StringBuilder result = new StringBuilder();
		for (Token t : iterable) {
			if (t.getKind().hasPayload()) {
				result.append(t.data);
			}
			else {
				result.append(t.getKind().tokenChars);
			}
		}
		return result.toString();
	}

	/**
	 * Return a list of string representations of {@link Token Tokens}.
	 *
	 * @param tokens list of tokens to convert to a list of strings
	 * @return list of strings for the provided tokens
	 */
	protected List<String> tokenListToStringList(List<Token> tokens) {
		if (tokens.isEmpty()) {
			return Collections.<String>emptyList();
		}
		List<String> data = new ArrayList<String>();
		for (Token token : tokens) {
			data.add(token.data);
		}
		return data;
	}

	/**
	 * Return the string representation of a {@link Token}.
	 *
	 * @param t token
	 * @return string representation of the provided token
	 */
	protected String toString(Token t) {
		return t.getKind().hasPayload() ? t.stringValue() : new String(t.kind.getTokenChars());
	}

	/**
	 * Verify the supplied name is a valid name. Valid names must follow the same rules as
	 * Java identifiers, with the additional option to use a hyphen ('-') after the first
	 * character.
	 *
	 * @param name the name to validate
	 * @return true if name is valid
	 */
	protected boolean isValidName(String name) {
		if (name.length() == 0 || !Character.isJavaIdentifierStart(name.charAt(0))) {
			return false;
		}

		for (int i = 1, max = name.length(); i < max; i++) {
			char ch = name.charAt(i);
			if (!(Character.isJavaIdentifierPart(ch) || ch == '-')) {
				return false;
			}
		}
		return true;
	}
	
	protected AppNode makeAppNode(LabelNode label, String appName, int startPos, int endPos, ArgumentNode[] arguments) {
		return new AppNode(label, appName, startPos, endPos, arguments);
	}

}
