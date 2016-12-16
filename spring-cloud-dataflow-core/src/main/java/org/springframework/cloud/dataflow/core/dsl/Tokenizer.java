/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.core.dsl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Lex some input data into a stream of tokens that can then then be parsed.
 *
 * @author Andy Clement
 * @author Eric Bottard
 */
class Tokenizer {

	/**
	 * The string to be tokenized.
	 */
	private String expressionString;

	/**
	 * The expressionString as a char array.
	 */
	private char[] toProcess;

	/**
	 * Length of input data.
	 */
	private int max;

	/**
	 * Current lexing position in the input data.
	 */
	private int pos;

	/**
	 * Output stream of tokens.
	 */
	private List<Token> tokens = new ArrayList<Token>();

	public Tokenizer(String inputData) {
		this.expressionString = inputData;
		this.toProcess = (inputData + "\0").toCharArray();
		this.max = toProcess.length;
		this.pos = 0;
		process();
	}

	private void process() {
		boolean justProcessedEquals = false;
		while (pos < max) {
			char ch = toProcess[pos];

			if (justProcessedEquals) {
				if (!isWhitespace(ch) && ch != 0) {
					// following an '=' we commence a variant of regular tokenization,
					// here we consume everything up to the next special char.
					// This allows SpEL expressions to be used without quoting in many
					// situations.
					lexArgValueIdentifier();
				}
				justProcessedEquals = false;
				continue;
			}

			if (isAlphabetic(ch) || isDigit(ch) || ch == '_') {
				lexIdentifier();
			}
			else {
				switch (ch) {
					case '-':
						if (!isTwoCharToken(TokenKind.DOUBLE_MINUS)) {
							throw new ParseException(
									expressionString, pos,
									DSLMessage.MISSING_CHARACTER, "-");
						}
						pushPairToken(TokenKind.DOUBLE_MINUS);
						break;
					case '=':
						justProcessedEquals = true;
						pushCharToken(TokenKind.EQUALS);
						break;
					case '&':
						pushCharToken(TokenKind.AND);
						break;
					case '|':
						pushCharToken(TokenKind.PIPE);
						break;
					case ' ':
					case '\t':
					case '\r':
						// drift over white space
						pos++;
						break;
					case '\n':
						pushCharToken(TokenKind.NEWLINE);
						break;
					case '.':
						pushCharToken(TokenKind.DOT);
						break;
					case '>':
						pushCharToken(TokenKind.GT);
						break;
					case ':':
						pushCharToken(TokenKind.COLON);
						break;
					case ';':
						pushCharToken(TokenKind.SEMICOLON);
						break;
					case '\'':
						lexQuotedStringLiteral();
						break;
					case '"':
						lexDoubleQuotedStringLiteral();
						break;
					case '@':
						pushCharToken(TokenKind.REFERENCE);
						break;
					case 0:
						// hit sentinel at end of char data
						pos++; // will take us to the end
						break;
					case '\\':
						throw new ParseException(
								expressionString, pos, DSLMessage.UNEXPECTED_ESCAPE_CHAR);
					default:
						throw new ParseException(
								expressionString, pos, DSLMessage.UNEXPECTED_DATA,
								Character.valueOf(ch).toString());
				}
			}
		}
	}

	public List<Token> getTokens() {
		return tokens;
	}

	/**
	 * Lex a string literal which uses single quotes as delimiters. To include
	 * a single quote within the literal, use a pair ''
	 */
	private void lexQuotedStringLiteral() {
		int start = pos;
		boolean terminated = false;
		while (!terminated) {
			pos++;
			char ch = toProcess[pos];
			if (ch == '\'') {
				// may not be the end if the char after is also a '
				if (toProcess[pos + 1] == '\'') {
					pos++; // skip over that too, and continue
				}
				else {
					terminated = true;
				}
			}
			if (ch == 0) {
				throw new ParseException(expressionString, start,
						DSLMessage.NON_TERMINATING_QUOTED_STRING);
			}
		}
		pos++;
		tokens.add(new Token(TokenKind.LITERAL_STRING,
				subArray(start, pos), start, pos));
	}

	/**
	 * Lex a string literal which uses double quotes as delimiters. To include
	 * a single quote within the literal, use a pair ""
	 */
	private void lexDoubleQuotedStringLiteral() {
		int start = pos;
		boolean terminated = false;
		while (!terminated) {
			pos++;
			char ch = toProcess[pos];
			if (ch == '"') {
				// may not be the end if the char after is also a "
				if (toProcess[pos + 1] == '"') {
					pos++; // skip over that too, and continue
				}
				else {
					terminated = true;
				}
			}
			if (ch == 0) {
				throw new ParseException(expressionString, start,
						DSLMessage.NON_TERMINATING_DOUBLE_QUOTED_STRING);
			}
		}
		pos++;
		tokens.add(new Token(TokenKind.LITERAL_STRING,
				subArray(start, pos), start, pos));
	}

	private void lexIdentifier() {
		int start = pos;
		do {
			pos++;
		}
		while (isIdentifier(toProcess[pos]));
		char[] subarray = subArray(start, pos);
		tokens.add(new Token(TokenKind.IDENTIFIER, subarray, start, pos));
	}

	/**
	 * For the variant tokenizer (used following an '=' to parse an
	 * argument value) we only terminate that identifier if
	 * encountering a small set of characters. If the argument has
	 * included a ' to put something in quotes, we remember
	 * that and don't allow ' ' (space) and '\t' (tab) to terminate
	 * the value.
	 */
	private boolean isArgValueIdentifierTerminator(char ch, boolean quoteOpen) {
		return (ch == '|' && !quoteOpen) || (ch == ';' && !quoteOpen) || ch == '\0' || (ch == ' ' && !quoteOpen)
				|| (ch == '\t' && !quoteOpen) || (ch == '>' && !quoteOpen)
				|| ch == '\r' || ch == '\n';
	}

	/**
	 * To prevent the need to quote all argument values, this identifier
	 * lexing function is used just after an '=' when we are about to
	 * digest an arg value. It is much more relaxed about what it will
	 * include in the identifier.
	 */
	private void lexArgValueIdentifier() {
		// Much of the complexity in here relates to supporting cases like these:
		// 'hi'+payload
		// 'hi'+'world'
		// In these situations it looks like a quoted string and that perhaps the entire
		// argument value is being quoted, but in fact half way through it is discovered that the
		// entire value is not quoted, only the first part of the argument value is a string literal.

		int start = pos;
		boolean quoteOpen = false;
		int quoteClosedCount = 0; // Enables identification of this pattern: 'hello'+'world'
		Character quoteInUse = null; // If set, indicates this is being treated as a quoted string
		if (isQuote(toProcess[pos])) {
			quoteOpen = true;
			quoteInUse = toProcess[pos++];
		}
		do {
			char ch = toProcess[pos];
			if ((quoteInUse != null && ch == quoteInUse) || (quoteInUse == null && isQuote(ch))) {
				if (quoteInUse != null && quoteInUse == '\'' && ch == '\'' && toProcess[pos + 1] == '\'') {
					pos++; // skip over that too, and continue
				}
				else {
					quoteOpen = !quoteOpen;
					if (!quoteOpen) {
						quoteClosedCount++;
					}
				}
			}
			pos++;
		}
		while (!isArgValueIdentifierTerminator(toProcess[pos], quoteOpen));
		char[] subarray = null;
		if (quoteInUse != null && quoteInUse == '"' && quoteClosedCount == 0 ) {
			throw new ParseException(expressionString, start,
				DSLMessage.NON_TERMINATING_DOUBLE_QUOTED_STRING);
		} else if (quoteInUse != null && quoteInUse == '\'' && quoteClosedCount == 0) {
			throw new ParseException(expressionString, start,
				DSLMessage.NON_TERMINATING_QUOTED_STRING);
		} else if (quoteClosedCount == 1 && sameQuotes(start, pos - 1)) {
			tokens.add(new Token(TokenKind.LITERAL_STRING,
					subArray(start, pos), start, pos));
		}
		else {
			subarray = subArray(start, pos);
			tokens.add(new Token(TokenKind.IDENTIFIER, subarray, start, pos));
		}
	}

	private boolean sameQuotes(int pos1, int pos2) {
		if (toProcess[pos1] == '\'') {
			return toProcess[pos2] == '\'';
		}
		else if (toProcess[pos1] == '"') {
			return toProcess[pos2] == '"';
		}
		return false;
	}

	private char[] subArray(int start, int end) {
		char[] result = new char[end - start];
		System.arraycopy(toProcess, start, result, 0, end - start);
		return result;
	}

	/**
	 * Check if this might be a two character token.
	 */
	private boolean isTwoCharToken(TokenKind kind) {
		Assert.isTrue(kind.tokenChars.length == 2);
		Assert.isTrue(toProcess[pos] == kind.tokenChars[0]);
		return toProcess[pos + 1] == kind.tokenChars[1];
	}

	/**
	 * Push a token of just one character in length.
	 */
	private void pushCharToken(TokenKind kind) {
		tokens.add(new Token(kind, pos, pos + 1));
		pos++;
	}

	/**
	 * Push a token of two characters in length.
	 */
	private void pushPairToken(TokenKind kind) {
		tokens.add(new Token(kind, pos, pos + 2));
		pos += 2;
	}

	// ID: ('a'..'z'|'A'..'Z'|'_'|'$') ('a'..'z'|'A'..'Z'|'_'|'$'|'0'..'9'|DOT_ESCAPED|'-')*;
	private boolean isIdentifier(char ch) {
		return isAlphabetic(ch) || isDigit(ch) || ch == '_' || ch == '$' || ch == '-';
	}

	private boolean isQuote(char ch) {
		return ch == '\'' || ch == '"';
	}

	private boolean isWhitespace(char ch) {
		return ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n';
	}

	private boolean isDigit(char ch) {
		if (ch > 255) {
			return false;
		}
		return (flags[ch] & IS_DIGIT) != 0;
	}

	private boolean isAlphabetic(char ch) {
		if (ch > 255) {
			return false;
		}
		return (flags[ch] & IS_ALPHA) != 0;
	}

	private static final byte flags[] = new byte[256];

	private static final byte IS_DIGIT = 0x01;

	private static final byte IS_HEXDIGIT = 0x02;

	private static final byte IS_ALPHA = 0x04;

	static {
		for (int ch = '0'; ch <= '9'; ch++) {
			flags[ch] |= IS_DIGIT | IS_HEXDIGIT;
		}
		for (int ch = 'A'; ch <= 'F'; ch++) {
			flags[ch] |= IS_HEXDIGIT;
		}
		for (int ch = 'a'; ch <= 'f'; ch++) {
			flags[ch] |= IS_HEXDIGIT;
		}
		for (int ch = 'A'; ch <= 'Z'; ch++) {
			flags[ch] |= IS_ALPHA;
		}
		for (int ch = 'a'; ch <= 'z'; ch++) {
			flags[ch] |= IS_ALPHA;
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(this.expressionString).append("\n");
		for (int i = 0; i < this.pos; i++) {
			s.append(" ");
		}
		s.append("^\n");
		s.append(tokens).append("\n");
		return s.toString();
	}

}
