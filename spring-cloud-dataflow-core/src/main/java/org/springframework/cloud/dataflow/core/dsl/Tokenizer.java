/*
 * Copyright 2015-2016 the original author or authors.
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

/**
 * Lex some input data into a stream of tokens that can then then be parsed.
 *
 * @author Andy Clement
 * @author Eric Bottard
 */
class Tokenizer extends AbstractTokenizer {

	public Tokenizer() {
	}

	protected void process() {
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

//	@Override
//	public TokenInfo getTokens(String inputData) {
//		this.expressionString = inputData;
//		this.toProcess = (inputData + "\0").toCharArray();
//		this.max = toProcess.length;
//		this.pos = 0;
//		this.tokens.clear();
//		process();
//		return tokens;
//	}

//	/**
//	 * Lex a string literal which uses single quotes as delimiters. To include
//	 * a single quote within the literal, use a pair ''
//	 */
//	private void lexQuotedStringLiteral() {
//		int start = pos;
//		boolean terminated = false;
//		while (!terminated) {
//			pos++;
//			char ch = toProcess[pos];
//			if (ch == '\'') {
//				// may not be the end if the char after is also a '
//				if (toProcess[pos + 1] == '\'') {
//					pos++; // skip over that too, and continue
//				}
//				else {
//					terminated = true;
//				}
//			}
//			if (ch == 0) {
//				throw new ParseException(expressionString, start,
//						DSLMessage.NON_TERMINATING_QUOTED_STRING);
//			}
//		}
//		pos++;
//		tokens.add(new Token(TokenKind.LITERAL_STRING,
//				subArray(start, pos), start, pos));
//	}
//
//	/**
//	 * Lex a string literal which uses double quotes as delimiters. To include
//	 * a single quote within the literal, use a pair ""
//	 */
//	private void lexDoubleQuotedStringLiteral() {
//		int start = pos;
//		boolean terminated = false;
//		while (!terminated) {
//			pos++;
//			char ch = toProcess[pos];
//			if (ch == '"') {
//				// may not be the end if the char after is also a "
//				if (toProcess[pos + 1] == '"') {
//					pos++; // skip over that too, and continue
//				}
//				else {
//					terminated = true;
//				}
//			}
//			if (ch == 0) {
//				throw new ParseException(expressionString, start,
//						DSLMessage.NON_TERMINATING_DOUBLE_QUOTED_STRING);
//			}
//		}
//		pos++;
//		tokens.add(new Token(TokenKind.LITERAL_STRING,
//				subArray(start, pos), start, pos));
//	}

//	/**
//	 * For the variant tokenizer (used following an '=' to parse an
//	 * argument value) we only terminate that identifier if
//	 * encountering a small set of characters. If the argument has
//	 * included a ' to put something in quotes, we remember
//	 * that and don't allow ' ' (space) and '\t' (tab) to terminate
//	 * the value.
//	 */
//	private boolean isArgValueIdentifierTerminator(char ch, boolean quoteOpen) {
//		return (ch == '|' && !quoteOpen) || (ch == ';' && !quoteOpen) || ch == '\0' || (ch == ' ' && !quoteOpen)
//				|| (ch == '\t' && !quoteOpen) || (ch == '>' && !quoteOpen)
//				|| ch == '\r' || ch == '\n';
//	}

//	/**
//	 * To prevent the need to quote all argument values, this identifier
//	 * lexing function is used just after an '=' when we are about to
//	 * digest an arg value. It is much more relaxed about what it will
//	 * include in the identifier.
//	 */
//	private void lexArgValueIdentifier() {
//		// Much of the complexity in here relates to supporting cases like these:
//		// 'hi'+payload
//		// 'hi'+'world'
//		// In these situations it looks like a quoted string and that perhaps the entire
//		// argument value is being quoted, but in fact half way through it is discovered that the
//		// entire value is not quoted, only the first part of the argument value is a string literal.
//
//		int start = pos;
//		boolean quoteOpen = false;
//		int quoteClosedCount = 0; // Enables identification of this pattern: 'hello'+'world'
//		Character quoteInUse = null; // If set, indicates this is being treated as a quoted string
//		if (isQuote(toProcess[pos])) {
//			quoteOpen = true;
//			quoteInUse = toProcess[pos++];
//		}
//		do {
//			char ch = toProcess[pos];
//			if ((quoteInUse != null && ch == quoteInUse) || (quoteInUse == null && isQuote(ch))) {
//				if (quoteInUse != null && quoteInUse == '\'' && ch == '\'' && toProcess[pos + 1] == '\'') {
//					pos++; // skip over that too, and continue
//				}
//				else {
//					quoteOpen = !quoteOpen;
//					if (!quoteOpen) {
//						quoteClosedCount++;
//					}
//				}
//			}
//			pos++;
//		}
//		while (!isArgValueIdentifierTerminator(toProcess[pos], quoteOpen));
//		char[] subarray = null;
//		if (quoteInUse != null && quoteInUse == '"' && quoteClosedCount == 0 ) {
//			throw new ParseException(expressionString, start,
//				DSLMessage.NON_TERMINATING_DOUBLE_QUOTED_STRING);
//		} else if (quoteInUse != null && quoteInUse == '\'' && quoteClosedCount == 0) {
//			throw new ParseException(expressionString, start,
//				DSLMessage.NON_TERMINATING_QUOTED_STRING);
//		} else if (quoteClosedCount == 1 && sameQuotes(start, pos - 1)) {
//			tokens.add(new Token(TokenKind.LITERAL_STRING,
//					subArray(start, pos), start, pos));
//		}
//		else {
//			subarray = subArray(start, pos);
//			tokens.add(new Token(TokenKind.IDENTIFIER, subarray, start, pos));
//		}
//	}

//	private boolean sameQuotes(int pos1, int pos2) {
//		if (toProcess[pos1] == '\'') {
//			return toProcess[pos2] == '\'';
//		}
//		else if (toProcess[pos1] == '"') {
//			return toProcess[pos2] == '"';
//		}
//		return false;
//	}
	
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
