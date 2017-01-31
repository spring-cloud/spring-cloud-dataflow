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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Visitor for a parsed composed task that verifies it is coherent. Rules being checked:
 * <ul>
 * <li>Any secondary sequences are labeled (otherwise they are unreachable).
 * <li>Cannot label two things with the same string.
 * <li>All target labels used on transitions must exist.
 * <li>TODO much more!
 * </ul>
 *
 * @author Andy Clement
 */
public class ComposedTaskValidatorVisitor extends ComposedTaskVisitor {

	// Text of the AST being validated
	private String composedTaskText;

	private List<ComposedTaskValidationProblem> problems = new ArrayList<ComposedTaskValidationProblem>();

	// At the end of the visit, verify any sequences that are never used
	private List<LabelledComposedTaskNode> recordedSequences = new ArrayList<LabelledComposedTaskNode>();
	
	private Set<Transition> transitionsTargetingLabels = new HashSet<>();
	
	private Set<String> labelsDefined = new HashSet<>();

	public List<ComposedTaskValidationProblem> getProblems() {
		return problems;
	}
	
	public boolean hasProblems() {
		return problems.size()!=0;
	}
	
	public void reset() {
		this.problems.clear();
	}
	
	public void startVisit(String composedTaskText) {
		this.composedTaskText = composedTaskText;
	}
	
	@Override
	public boolean preVisitSequence(LabelledComposedTaskNode firstNode, int sequenceNumber) {
		if (sequenceNumber > 0 && !firstNode.hasLabel()) {
			pushProblem(firstNode.getStartPos(), ComposedTaskValidationProblem.SECONDARY_SEQUENCES_MUST_BE_NAMED);
		}
		recordedSequences.add(firstNode);
		return true;
	}

	@Override
	public void postVisitSequence(LabelledComposedTaskNode firstNode, int sequenceNumber) {
	}

	@Override
	public boolean preVisit(Flow flow) {
		return true;
	}

	@Override
	public void visit(Flow flow) {
	}

	@Override
	public void postVisit(Flow flow) {
	}

	@Override
	public boolean preVisit(Split split) {
		if (split.hasLabel()) {
			String labelString = split.getLabelString();
			if (labelsDefined.contains(labelString)) {
				pushProblem(split.getLabel().startPos,ComposedTaskValidationProblem.DUPLICATE_LABEL);
			}
			labelsDefined.add(labelString);
		}
		return true;
	}

	@Override
	public void visit(Split split) {
		
	}

	@Override
	public void postVisit(Split split) {
	}

	@Override
	public boolean preVisit(TaskApp taskApp) {
		return true;
	}

	@Override
	public void visit(TaskApp taskApp) {
		if (taskApp.hasLabel()) {
			String labelString = taskApp.getLabelString();
			if (labelsDefined.contains(labelString)) {
				pushProblem(taskApp.getLabel().startPos,ComposedTaskValidationProblem.DUPLICATE_LABEL);
			}
			labelsDefined.add(labelString);
		}
	}

	@Override
	public void postVisit(TaskApp taskApp) {
	}

	@Override
	public boolean preVisit(Transition transition) {
		return true;
	}

	@Override
	public void visit(Transition transition) {
		if (!transition.isTargetApp()) {
			transitionsTargetingLabels.add(transition);
		}
	}
	
	@Override
	public void postVisit(Transition transition) {
	}

	@Override
	public void endVisit() {
		// Verify all targeted labels exist
		for (Transition transitionTargetingLabel: transitionsTargetingLabels) {
			if (!labelsDefined.contains(transitionTargetingLabel.getTargetLabel())) {
				pushProblem(transitionTargetingLabel.startPos,ComposedTaskValidationProblem.TRANSITION_TARGET_LABEL_UNDEFINED);
			}
		}
		// TODO Verify all secondary sequences are visited
	}
	
	private void pushProblem(int pos, String message) {
		problems.add(new ComposedTaskValidationProblem(composedTaskText, pos, message));
	}

}
