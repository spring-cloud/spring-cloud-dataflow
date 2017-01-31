/*
 * Copyright 2017 the original author or authors.
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
 * After parsing a composed task definition from a DSL string, the validation visitor may optionally run.
 * Even though it parses successfully there may be issues with how the definition is constructed. The 
 * {@link ComposedTaskValidatorVisitor} will find those problems and report them as instances of
 * {@link ComposedTaskValidationProblem}.
 * 
 * @author Andy Clement
 */
public class ComposedTaskValidationProblem {
	public final static String SECONDARY_SEQUENCES_MUST_BE_NAMED = "Secondary sequences must have labels or are unreachable";
	public final static String DUPLICATE_LABEL = "This label has already been defined";
	public final static String TRANSITION_TARGET_LABEL_UNDEFINED = "Transition specifies an undefined label";
	
	final String composedTaskText;
	final int offset;
	final String message;
	
	public ComposedTaskValidationProblem(String composedTaskText, int offset, String message) {
		this.composedTaskText = composedTaskText;
		this.offset = offset;
		this.message = message;
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(message).append(" @ ").append(offset);
		return s.toString();
	}
}