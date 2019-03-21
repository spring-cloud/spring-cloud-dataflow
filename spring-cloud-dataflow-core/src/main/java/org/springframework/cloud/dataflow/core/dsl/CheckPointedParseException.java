/*
 * Copyright 2015 the original author or authors.
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

import java.util.List;

/**
 * An extension of {@link ParseException} that keeps track of the last position up to
 * which parsing was successful. Backtracking to that position and attempting a parse
 * again is guaranteed to be successful.
 *
 * @author Eric Bottard
 */
@SuppressWarnings("serial")
public class CheckPointedParseException extends ParseException {

	private int checkpointPointer = -1;

	private List<Token> tokens;

	private int tokenPointer;

	/**
	 * Construct a new {@code CheckPointedParseException}.
	 *
	 * @param expressionString the raw, untokenized text that was being parsed
	 * @param textPosition the text offset where the error occurs
	 * @param tokenPointer the token-index of token where the error occurred
	 * @param checkpointPointer the token-index of the last known good token
	 * @param tokens the list of tokens that make up expressionString
	 * @param message the error message
	 * @param inserts variables that may be inserted in the error message
	 */
	public CheckPointedParseException(String expressionString, int textPosition, int tokenPointer,
			int checkpointPointer, List<Token> tokens, DSLMessage message, Object... inserts) {
		super(expressionString, textPosition, message, inserts);
		this.tokenPointer = tokenPointer;
		this.checkpointPointer = checkpointPointer;
		this.tokens = tokens;
	}

	/**
	 * @return a formatted message with inserts applied.
	 */
	@Override
	public String getMessage() {
		StringBuilder s = new StringBuilder();
		if (message != null) {
			s.append(message.formatMessage(position, inserts));
		}
		else {
			s.append(super.getMessage());
		}
		int startOfLine = getStartOfLine(position);
		if (expressionString != null && expressionString.length() > 0) {
			s.append("\n").append(expressionString.substring(startOfLine)).append("\n");
		}
		int offset = position - startOfLine;
		if (checkpointPointer > 0 && offset >= 0 && (getStartOfLine(checkpointPointer) == startOfLine)) {
			int checkpointPosition = getCheckpointPosition();
			checkpointPosition = checkpointPosition - getStartOfLine(checkpointPosition);
			offset -= checkpointPosition;
			for (int i = 0; i < checkpointPosition; i++) {
				s.append(' ');
			}
			s.append("*");
			offset--; // account for the '*'
		}
		if (offset >= 0) {
			for (int i = 0; i < offset; i++) {
				s.append(' ');
			}
			s.append("^\n");
		}
		return s.toString();
	}

	/**
	 * If there are newlines, need to ensure the place where the ^ will go is adjusted.
	 *
	 * @param position the offset from the start of the string (ignoring newlines)
	 * @return the offset from the start of the line
	 */
	private int getStartOfLine(int position) {
		int lineStart = 0;
		for (int p = 0; p < position; p++) {
			if (expressionString.charAt(p) == '\n') {
				lineStart = p + 1;
			}
		}
		return lineStart;
	}

	public int getCheckpointPosition() {
		return checkpointPointer == 0 ? 0 : tokens.get(checkpointPointer - 1).endPos;
	}

	/**
	 * Return the parsed expression until the last known, well formed position. Attempting
	 * to re-parse that expression is guaranteed to not fail.
	 */
	public String getExpressionStringUntilCheckpoint() {
		return expressionString.substring(0, getCheckpointPosition());
	}

	public int getCheckpointPointer() {
		return checkpointPointer;
	}

	public List<Token> getTokens() {
		return tokens;
	}

	public int getTokenPointer() {
		return tokenPointer;
	}

}
