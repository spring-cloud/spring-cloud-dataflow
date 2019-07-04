/*
 * Copyright 2017-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Visitor for a parsed task that verifies it is coherent. Rules being checked:
 * <ul>
 * <li>Any secondary sequences are labeled (otherwise they are unreachable).
 * <li>Cannot label two things with the same string.
 * <li>All target labels used on transitions must exist.
 * <li>Two references to the same app must be labeled to differentiate them
 * <li>Do not use split construct with only one flow inside
 * </ul>
 *
 * @author Andy Clement
 */
public class TaskValidatorVisitor extends TaskVisitor {

	// Text of the AST being validated
	private String taskDsl;

	private List<TaskValidationProblem> problems = new ArrayList<>();

	// At the end of the visit, verify any sequences that are never used
	private List<LabelledTaskNode> recordedSequences = new ArrayList<>();

	private Set<TransitionNode> transitionsTargetingLabels = new HashSet<>();

	private Set<String> labelsDefined = new HashSet<>();

	private Set<String> taskAppNamesWithoutLabels = new HashSet<>();

	public List<TaskValidationProblem> getProblems() {
		return problems;
	}

	public boolean hasProblems() {
		return problems.size() != 0;
	}

	public void reset() {
		this.problems.clear();
		this.recordedSequences.clear();
		this.transitionsTargetingLabels.clear();
		this.labelsDefined.clear();
		this.taskAppNamesWithoutLabels.clear();
	}

	@Override
	public void startVisit(String taskName, String taskDsl) {
		this.taskDsl = taskDsl;
	}

	@Override
	public boolean preVisitSequence(LabelledTaskNode firstNode, int sequenceNumber) {
		if (sequenceNumber > 0 && !firstNode.hasLabel()) {
			pushProblem(firstNode.getStartPos(), DSLMessage.TASK_VALIDATION_SECONDARY_SEQUENCES_MUST_BE_NAMED);
		}
		recordedSequences.add(firstNode);
		return true;
	}

	@Override
	public boolean preVisit(SplitNode split) {
		// This used to check there were multiple entries in the split otherwise it threw an error.
		// It could conditionally still throw that error if there is one entry in the split
		// and it has no transitions (since that is the only scenario that needs a single entry split)
		if (split.hasLabel()) {
			String labelString = split.getLabelString();
			if (labelsDefined.contains(labelString)) {
				pushProblem(split.getLabel().startPos, DSLMessage.TASK_VALIDATION_DUPLICATE_LABEL);
			}
			labelsDefined.add(labelString);
		}
		return true;
	}

	@Override
	public void visit(TaskAppNode taskApp) {
		if (taskApp.hasLabel()) {
			String labelString = taskApp.getLabelString();
			if (labelsDefined.contains(labelString)) {
				pushProblem(taskApp.getLabel().startPos, DSLMessage.TASK_VALIDATION_DUPLICATE_LABEL);
			}
			labelsDefined.add(labelString);
			if (taskAppNamesWithoutLabels.contains(labelString)) {
				pushProblem(taskApp.getLabel().startPos, DSLMessage.TASK_VALIDATION_LABEL_CLASHES_WITH_TASKAPP_NAME);
			}
		}
		else {
			String name = taskApp.getName();
			if (labelsDefined.contains(name)) {
				pushProblem(taskApp.startPos, DSLMessage.TASK_VALIDATION_APP_NAME_CLASHES_WITH_LABEL);
			}
			if (taskAppNamesWithoutLabels.contains(name)) {
				pushProblem(taskApp.startPos, DSLMessage.TASK_VALIDATION_APP_NAME_ALREADY_IN_USE);
			}
			taskAppNamesWithoutLabels.add(taskApp.getName());
		}
	}

	@Override
	public void visit(TransitionNode transition) {
		if (!transition.isTargetApp()) {
			transitionsTargetingLabels.add(transition);
		}
		if (transition.isTargetApp()) {
			TaskAppNode taskApp = transition.getTargetApp();
			if (taskApp.hasLabel()) {
				String labelString = taskApp.getLabelString();
				if (labelsDefined.contains(labelString)) {
					pushProblem(taskApp.getLabel().startPos, DSLMessage.TASK_VALIDATION_DUPLICATE_LABEL);
				}
				labelsDefined.add(labelString);
				if (taskAppNamesWithoutLabels.contains(labelString)) {
					pushProblem(taskApp.getLabel().startPos,
							DSLMessage.TASK_VALIDATION_LABEL_CLASHES_WITH_TASKAPP_NAME);
				}
			}
			else {
				String name = taskApp.getName();
				if (labelsDefined.contains(name)) {
					pushProblem(taskApp.startPos, DSLMessage.TASK_VALIDATION_APP_NAME_CLASHES_WITH_LABEL);
				}
				if (taskAppNamesWithoutLabels.contains(name)) {
					pushProblem(taskApp.startPos, DSLMessage.TASK_VALIDATION_APP_NAME_ALREADY_IN_USE);
				}
				taskAppNamesWithoutLabels.add(taskApp.getName());
			}
		}
	}

	@Override
	public void endVisit() {
		// Verify all targeted labels exist
		for (TransitionNode transitionTargetingLabel : transitionsTargetingLabels) {
			if (!labelsDefined.contains(transitionTargetingLabel.getTargetLabel())) {
				pushProblem(transitionTargetingLabel.startPos,
						DSLMessage.TASK_VALIDATION_TRANSITION_TARGET_LABEL_UNDEFINED);
			}
		}
	}

	private void pushProblem(int pos, DSLMessage message) {
		problems.add(new TaskValidationProblem(taskDsl, pos, message));
	}

}
