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

import java.util.List;

/**
 * If a parsed composed task AST is validated, if there are any validation errors
 * this exception will be thrown, it contains a list of the validation errors
 * found.
 *
 * @author Andy Clement
 */
@SuppressWarnings("serial")
public class ComposedTaskValidationException extends RuntimeException {

	private ComposedTaskNode composedTaskNode;

	private List<ComposedTaskValidationProblem> validationProblems;

	ComposedTaskValidationException(ComposedTaskNode composedTaskNode, List<ComposedTaskValidationProblem> validationProblems) {
		this.composedTaskNode = composedTaskNode;
		this.validationProblems = validationProblems;
	}

	public List<ComposedTaskValidationProblem> getValidationProblems() {
		return validationProblems;
	}

	public ComposedTaskNode getComposedTaskNode() {
		return composedTaskNode;
	}

	/**
	 * @return a formatted message with inserts applied
	 */
	@Override
	public String getMessage() {
		StringBuilder s = new StringBuilder();
		s.append("Problems found when validating '").append(composedTaskNode.getComposedTaskText()).append("': ");
		s.append(validationProblems);
		return s.toString();
	}

}
