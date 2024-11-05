/*
 * Copyright 2015-2017 the original author or authors.
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

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Class that converts an expression into a list of {@link Token tokens}. Furthermore,
 * this class provides methods to process the tokens and keeps track of the current token
 * being processed.
 *
 * @author Andy Clement
 * @author Patrick Peralta
 */
public class Tokens {

	/**
	 * Expression string to be parsed.
	 */
	private final String expression;

	/**
	 * List of tokens created from {@link #expression}.
	 */
	private final List<Token> tokenStream;

	/**
	 * Position of linebreaks in the parsed string. Token positions are absolute from
	 * start of string, so this array can be used to compute the line the token is on.
	 */
	private int[] linebreaks;

	/**
	 * Index of stream token currently being processed.
	 */
	private int position = 0;

	/**
	 * Index of last stream token that was successfully processed.
	 */
	private int lastGoodPosition = 0;

	/**
	 * Create a new tokens holder that can be iterated over. Created by the particular
	 * tokenizer instance (some concrete subclass of {@link AbstractTokenizer}).
	 *
	 * @param expression string expression to convert into {@link Token tokens}.
	 * @param tokens the stream of tokens
	 * @param linebreaks the offsets within the expression where newlines occur
	 */
	Tokens(String expression, List<Token> tokens, int[] linebreaks) {
		this.expression = expression;
		this.linebreaks = linebreaks;
		this.tokenStream = Collections.unmodifiableList(tokens);
	}

	/**
	 * Return the expression string converted to tokens.
	 *
	 * @return expression string
	 */
	public String getExpression() {
		return expression;
	}

	/**
	 * Decrement the current token position and return the new position.
	 *
	 * @return new token position
	 */
	public int decrementPosition() {
		return --position;
	}

	/**
	 * Return the current token position.
	 *
	 * @return current token position
	 */
	public int position() {
		return position;
	}

	/**
	 * Return an immutable list of {@link Token tokens}
	 *
	 * @return list of tokens
	 */
	public List<Token> getTokenStream() {
		return tokenStream;
	}

	/**
	 * Return {@code true} if the token in the position indicated by {@link #position} +
	 * {@code distance} matches the token indicated by {@code desiredTokenKind}.
	 *
	 * @param distance number of token positions past the current position
	 * @param desiredTokenKind the token to check for
	 * @return true if the token at the indicated position matches
	 * {@code desiredTokenKind}
	 */
	protected boolean lookAhead(int distance, TokenKind desiredTokenKind) {
		if ((position + distance) >= tokenStream.size()) {
			return false;
		}
		Token t = tokenStream.get(position + distance);
		return t.kind == desiredTokenKind;
	}

	/**
	 * Return {@code true} if there are more tokens to process.
	 *
	 * @return {@code true} if there are more tokens to process
	 */
	public boolean hasNext() {
		return position < tokenStream.size();
	}

	/**
	 * Return the token at the current position. If there are no more tokens, return
	 * {@code null}.
	 *
	 * @return token at current position or {@code null} if there are no more tokens
	 */
	public Token peek() {
		return hasNext() ? tokenStream.get(position) : null;
	}

	/**
	 * Return the token at a specified distance beyond current position. If that is off
	 * the end of the list of known tokens, return {@code null}.
	 *
	 * @param howFarAhead how many character positions to peek ahead.
	 * @return token at specified distance beyond current position or {@code null} if
	 * there are no more tokens
	 */
	protected Token peek(int howFarAhead) {
		if ((position + howFarAhead) >= 0 && (position + howFarAhead) < tokenStream.size()) {
			return tokenStream.get(position + howFarAhead);
		}
		else {
			return null;
		}
	}

	/**
	 * Return {@code true} if the indicated token matches the current token position.
	 *
	 * @param desiredTokenKind token to match
	 * @return true if the current token kind matches the provided token kind
	 */
	public boolean peek(TokenKind desiredTokenKind) {
		return peek(desiredTokenKind, false);
	}

	/**
	 * Return {@code true} if the indicated token matches the current token position.
	 *
	 * @param desiredTokenKind token to match
	 * @param consumeIfMatched if {@code true}, advance the current token position
	 * @return true if the current token kind matches the provided token kind
	 */
	private boolean peek(TokenKind desiredTokenKind, boolean consumeIfMatched) {
		if (!hasNext()) {
			return false;
		}
		Token t = peek();
		if (t.kind == desiredTokenKind) {
			if (consumeIfMatched) {
				position++;
			}
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Return the next {@link Token} and advance the current token position.
	 *
	 * @return next {@code Token}
	 */
	public Token next() {
		if (!hasNext()) {
			raiseException(expression.length(), DSLMessage.OOD);
		}
		return tokenStream.get(position++);
	}

	/**
	 * Consume the next token if it matches the indicated token kind; otherwise throw
	 * {@link CheckPointedParseException}.
	 *
	 * @param expectedKind the expected token kind
	 * @return the next token
	 * @throws CheckPointedParseException if the next token does not match the expected
	 * token kind
	 */
	public Token eat(TokenKind expectedKind) {
		Token t = next();
		if (t == null) {
			raiseException(expression.length(), DSLMessage.OOD);
		}
		if (t.kind != expectedKind) {
			raiseException(t.startPos, DSLMessage.NOT_EXPECTED_TOKEN, expectedKind.toString().toLowerCase(Locale.ROOT),
					(t.data == null) ? new String(t.getKind().tokenChars).toLowerCase(Locale.ROOT) : t.data);
		}
		return t;
	}

	/**
	 * Consume the next token. Throw {@link CheckPointedParseException} if there is
	 * nothing to consume.
	 *
	 * @return the next token
	 * @throws CheckPointedParseException if there are no more tokens
	 */
	protected Token eat() {
		Token t = next();
		if (t == null) {
			raiseException(expression.length(), DSLMessage.OOD);
		}
		return t;
	}

	/**
	 * Return {@code true} if the first character of the token at the current position is
	 * the same as the last character of the token at the previous position.
	 *
	 * @return true if the first character of the current token matches the last character
	 * of the previous token
	 */
	public boolean isNextAdjacent() {
		if (!hasNext()) {
			return false;
		}

		Token last = tokenStream.get(position - 1);
		Token next = tokenStream.get(position);
		return next.startPos == last.endPos;
	}

	/**
	 * Indicate that the current token has been successfully processed.
	 *
	 * @see #lastGoodPosition
	 */
	protected void checkpoint() {
		lastGoodPosition = position;
	}

	/**
	 * Throw a new {@link CheckPointedParseException} based on the current and last
	 * successfully processed token position.
	 *
	 * @param position position where parse error occurred
	 * @param message parse exception message
	 * @param inserts variables that may be inserted in the error message
	 */
	public void raiseException(int position, DSLMessage message, Object... inserts) {
		throw new CheckPointedParseException(expression, position, this.position, lastGoodPosition, tokenStream,
				message, inserts);
	}

	/**
	 * @param token the token for which to determine the line
	 * @return which line the token is on, starting from 0
	 */
	public int getLine(Token token) {
		int tokenStart = token.startPos;
		int lb = 0;
		while (lb < linebreaks.length && linebreaks[lb] < tokenStart) {
			lb++;
		}
		return lb;
	}

}
