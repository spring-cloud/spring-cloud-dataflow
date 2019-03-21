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

/**
 * Parser for task DSL that generates {@link AppNode}.
 *
 * @author Michael Minella
 * @author Patrick Peralta
 */
public class TaskParser extends AppParser {

	/**
	 * Task name (may be {@code null}).
	 */
	private final String name;

	/**
	 * Construct a {@code TaskParser} without supplying the task name up front.
	 * The task name may be embedded in the definition; for example:
	 * {@code myTask = filejdbc}
	 *
	 * @param dsl the task definition DSL text
	 */
	public TaskParser(String dsl) {
		this(null, dsl);
	}

	/**
	 * Construct a {@code TaskParser} for a task with the provided name.
	 *
	 * @param name task name
	 * @param dsl  task dsl text
	 */
	public TaskParser(String name, String dsl) {
		super(new Tokens(dsl));
		this.name = name;
	}

	/**
	 * Parse a task definition.
	 *
	 * @return the AST for the parsed task
	 * @throws ParseException
	 */
	public AppNode parse() {
		AppNode ast = eatApp();

		// Check the task name, however it was specified
		if (ast.getName() != null && !isValidName(ast.getName())) {
			throw new ParseException(ast.getName(), 0, DSLMessage.ILLEGAL_TASK_NAME, ast.getName());
		}
		if (name != null && !isValidName(name)) {
			throw new ParseException(name, 0, DSLMessage.ILLEGAL_TASK_NAME, name);
		}
		Tokens tokens = getTokens();
		if (tokens.hasNext()) {
			tokens.raiseException(tokens.peek().startPos, DSLMessage.MORE_INPUT, toString(tokens.next()));
		}

		return ast;
	}

}
