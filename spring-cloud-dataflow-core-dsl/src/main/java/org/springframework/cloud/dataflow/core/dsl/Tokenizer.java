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

/**
 * Lex some input data into a stream of tokens that can then then be parsed.
 *
 * @author Andy Clement
 * @author Eric Bottard
 */
public class Tokenizer extends AbstractTokenizer {

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
					// If the next char is a special char then the argument value is missing
					if (isArgValueIdentifierTerminator(ch, false)) {
						raiseException(DSLMessage.EXPECTED_ARGUMENT_VALUE, ch);
					}
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
						throw new ParseException(expressionString, pos, DSLMessage.MISSING_CHARACTER, "-");
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
					if (isTwoCharToken(TokenKind.DOUBLEPIPE)) {
						pushPairToken(TokenKind.DOUBLEPIPE);
					}
					else {
						pushCharToken(TokenKind.PIPE);
					}
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
				case '<':
					pushCharToken(TokenKind.LT);
					break;
				case '>':
					pushCharToken(TokenKind.GT);
					break;
				case ':':
					pushCharToken(TokenKind.COLON);
					break;
				case '/':
					pushCharToken(TokenKind.SLASH);
					break;
				case '#':
					pushCharToken(TokenKind.HASH);
					break;
				case '*':
					pushCharToken(TokenKind.STAR);
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
					throw new ParseException(expressionString, pos, DSLMessage.UNEXPECTED_ESCAPE_CHAR);
				default:
					throw new ParseException(expressionString, pos, DSLMessage.UNEXPECTED_DATA,
							Character.valueOf(ch).toString());
				}
			}
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
