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

/**
 * Lex some input data into a stream of tokens that can then then be parsed. This
 * tokenizer recognizes the task DSL - both the simple (single task) and composed task
 * cases.
 *
 * @author Andy Clement
 */
class TaskTokenizer extends AbstractTokenizer {

	@Override
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

			// Difference between stream and task here, $ is allowed identifier prefix to
			// allow
			// for $END/$FAIL
			if (isAlphabetic(ch) || isDigit(ch) || ch == '_' || ch == '$') {
				lexIdentifier();
			}
			else {
				switch (ch) {
				case '-':
					if (isTwoCharToken(TokenKind.DOUBLE_MINUS)) {
						pushPairToken(TokenKind.DOUBLE_MINUS);
					}
					else if (isTwoCharToken(TokenKind.ARROW)) {
						pushPairToken(TokenKind.ARROW);
					}
					else {
						throw new ParseException(expressionString, pos, DSLMessage.MISSING_CHARACTER, "-");
					}
					break;
				case '&':
					if (isTwoCharToken(TokenKind.ANDAND)) {
						pushPairToken(TokenKind.ANDAND);
					}
					else {
						raiseException(DSLMessage.TASK_DOUBLE_AND_REQUIRED);
					}
					break;
				case '|':
					if (isTwoCharToken(TokenKind.DOUBLEPIPE)) {
						pushPairToken(TokenKind.DOUBLEPIPE);
					}
					else {
						raiseException(DSLMessage.TASK_DOUBLE_OR_REQUIRED);
					}
					break;
				case ' ':
				case '\t':
				case '\r':
					// drift over white space
					pos++;
					break;
				case '.':
					pushCharToken(TokenKind.DOT);
					break;
				case '\n':
					addLinebreak();
					break;
				case '<':
					pushCharToken(TokenKind.LT);
					break;
				case '>':
					pushCharToken(TokenKind.GT);
					break;
				case '(':
					pushCharToken(TokenKind.OPEN_PAREN);
					break;
				case ')':
					pushCharToken(TokenKind.CLOSE_PAREN);
					break;
				case '\'':
					lexQuotedStringLiteral();
					break;
				case '"':
					lexDoubleQuotedStringLiteral();
					break;
				case ':':
					pushCharToken(TokenKind.COLON);
					break;
				case '=':
					justProcessedEquals = true;
					pushCharToken(TokenKind.EQUALS);
					break;
				case '*':
					pushCharToken(TokenKind.STAR);
					break;
				case ';':
					pushCharToken(TokenKind.SEMICOLON);
					break;
				case 0:
					// hit sentinel at end of char data
					pos++; // will take us to the end
					break;
				case '\\':
					raiseException(DSLMessage.UNEXPECTED_ESCAPE_CHAR);
				default:
					raiseException(DSLMessage.TASK_UNEXPECTED_DATA, Character.valueOf(ch).toString());
				}
			}
		}
	}

	protected void lexIdentifier() {
		int start = pos;
		do {
			pos++;
			if (toProcess[pos] == '-' && toProcess[pos + 1] == '>') {
				// When hitting '0->' treat '->' as an arrow and end marker of the
				// identifier '0'
				break;
			}
		}
		while (isIdentifier(toProcess[pos]));
		char[] subarray = subArray(start, pos);
		tokens.add(new Token(TokenKind.IDENTIFIER, subarray, start, pos));
	}

}
