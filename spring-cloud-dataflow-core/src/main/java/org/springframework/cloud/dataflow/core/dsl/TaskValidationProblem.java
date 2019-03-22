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

import java.util.HashMap;
import java.util.Map;

/**
 * After parsing a task definition from a DSL string, the validation visitor may
 * optionally run. Even though it parses successfully there may be issues with how the
 * definition is constructed. The {@link TaskValidatorVisitor} will find those problems
 * and report them as instances of {@link TaskValidationProblem}.
 *
 * @author Andy Clement
 */
public class TaskValidationProblem {

	private final String taskDsl;

	private final int offset;

	private final DSLMessage message;

	public TaskValidationProblem(String taskDsl, int offset, DSLMessage message) {
		this.taskDsl = taskDsl;
		this.offset = offset;
		this.message = message;
	}

	public String toString() {
		return message.formatMessage(offset);
	}

	public String toStringWithContext() {
		StringBuilder s = new StringBuilder();
		s.append(message.formatMessage(offset));
		int startOfLine = getStartOfLine(offset);
		if (taskDsl != null && taskDsl.length() > 0) {
			s.append("\n").append(taskDsl.substring(startOfLine)).append("\n");
		}
		int offsetOnLine = offset - startOfLine;
		if (offsetOnLine >= 0) {
			for (int i = 0; i < offsetOnLine; i++) {
				s.append(' ');
			}
			s.append("^\n");
		}
		return s.toString();
	}

	public DSLMessage getMessage() {
		return message;
	}

	public int getOffset() {
		return offset;
	}

	private int getStartOfLine(int position) {
		for (int p = 0; p < position; p++) {
			if (taskDsl.charAt(p) == '\n') {
				return p + 1;
			}
		}
		return 0;
	}

	/**
	 * Produce a simple map of information about the exception that can be sent to the
	 * client for display.
	 *
	 * @return map of simple information including message and position
	 */
	public Map<String, Object> toExceptionDescriptor() {
		Map<String, Object> descriptor = new HashMap<>();
		String text = message.formatMessage(offset);
		descriptor.put("message", text);
		descriptor.put("position", offset);
		return descriptor;
	}

}
