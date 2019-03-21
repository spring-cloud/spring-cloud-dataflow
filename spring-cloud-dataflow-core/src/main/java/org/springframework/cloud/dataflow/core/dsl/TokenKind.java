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

/**
 * Enumeration of all the token types that may be found in a DSL definition stream. DSL
 * variants may use some subset of these tokens. For example stream DSL doesn't use the LT
 * token, it is used by the task DSL. The tokenizer in use will decide which subset are
 * built for a particular DSL.
 *
 * @author Andy Clement
 */
public enum TokenKind {
	IDENTIFIER,
	DOUBLE_MINUS("--"),
	EQUALS("="),
	AND("&"),
	ANDAND("&&"),
	OROR("||"),
	ARROW("->"),
	PIPE("|"),
	OPEN_PAREN("("),
	CLOSE_PAREN(")"),
	NEWLINE("\n"),
	STAR("*"),
	COLON(":"),
	GT(">"),
	LT("<"),
	SEMICOLON(";"),
	REFERENCE("@"),
	DOT("."),
	LITERAL_STRING,;

	char[] tokenChars;

	private boolean hasPayload; // is there more to this token than simply the kind

	TokenKind(String tokenString) {
		tokenChars = tokenString.toCharArray();
		hasPayload = tokenChars.length == 0;
	}

	TokenKind() {
		this("");
	}

	@Override
	public String toString() {
		return this.name() + (tokenChars.length != 0 ? "(" + new String(tokenChars) + ")" : "");
	}

	public boolean hasPayload() {
		return hasPayload;
	}

	public int getLength() {
		return tokenChars.length;
	}

	/**
	 * @return the chars representing simple fixed token (eg. : > --)
	 */
	public char[] getTokenChars() {
		return tokenChars;
	}
}
