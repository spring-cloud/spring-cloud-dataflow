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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.dataflow.core.dsl.graph.Graph;
import org.springframework.util.Assert;

/**
 * The root AST node for any AST parsed from a composed task specification.
 *
 * @author Andy Clement
 */
public class ComposedTaskNode extends AstNode {

	/**
	 * The name of the composed task.
	 */
	private String name;
	
	/**
	 * The DSL text that was parsed to create this ComposedTaskNode.
	 */
	private String composedTaskText;

	/**
	 * The sequence of LabelledNodes parsed from the specification.
	 */
	private List<LabelledComposedTaskNode> sequences;

	/**
	 * All the apps mentioned in the composed task definition.
	 */
	private Set<String> taskApps;

	ComposedTaskNode(String name, String composedTaskSpecification, List<LabelledComposedTaskNode> sequences) {
		super((sequences.size()==0) ? 0 : sequences.get(0).getStartPos(), 
				(sequences.size()==0) ? 0 : sequences.get(sequences.size()-1).getEndPos());
		this.name = name;
		this.composedTaskText = composedTaskSpecification;
		this.sequences = sequences;
	}

	@Override
	public String stringify(boolean includePositionInfo) {
		StringBuilder s = new StringBuilder();
		for (int i=0;i<sequences.size();i++) {
			if (i>0) {
				s.append("\n");
			}
			s.append(sequences.get(i).stringify(includePositionInfo));
		}
		return s.toString();
	}

	/**
	 * @return all the apps referenced in the composed task.
	 */
	public Set<String> getTaskApps() {
		if (taskApps == null) {
			TaskAppsCollector collector = new TaskAppsCollector();
			accept(collector);
			taskApps = collector.getTaskApps();
		}
		return taskApps;
	}
	
	/**
	 * Walk the AST for the parsed composed task, calling the visitor for each element of interest.
	 * See the {@link ComposedTaskVisitor} for all the events that can be received, since that is an
	 * abstract class it can be extended and subclasses only need override methods to receive the
	 * events of interest.
	 * @param visitor a visitor to be called as the AST is walked
	 */
	public void accept(ComposedTaskVisitor visitor) {
		Assert.notNull(visitor,"visitor expected to be non-null");
		visitor.startVisit(this.composedTaskText);
		int sequenceNumber = 0;
		for (LabelledComposedTaskNode ctn: sequences) {
			if (visitor.preVisitSequence(ctn, sequenceNumber)) {
				ctn.accept(visitor);
				visitor.postVisitSequence(ctn, sequenceNumber);
			}
			sequenceNumber++;
		}
		visitor.endVisit();
	}
	
	/**
	 * @return this AST converted to a Graph form for display by Flo
	 */
	public Graph toGraph() {
		GraphGeneratorVisitor ggv = new GraphGeneratorVisitor();
		this.accept(ggv);
		return ggv.getGraph();
	}
	
	/**
	 * @return a list of problems found during validation, empty if no problems
	 */
	public List<ComposedTaskValidationProblem> validate() {
		ComposedTaskValidatorVisitor validator = new ComposedTaskValidatorVisitor();
		this.accept(validator);
		return validator.getProblems();
	}
	
	/**
	 * Simple visitor that discovers all the tasks in use in the composed task definition.
	 */
	static class TaskAppsCollector extends ComposedTaskVisitor {

		Set<String> taskApps = new HashSet<String>();

		@Override
		public void visit(TaskAppNode taskApp) {
			taskApps.add(taskApp.getName());
		}
		
		@Override
		public void visit(TransitionNode transition) {
			if (transition.isTargetApp()) {
				taskApps.add(transition.getTargetApp());
			}
		}
		
		public Set<String> getTaskApps() {
			return taskApps;
		}

	}
	
	public String getName() {
		return this.name;
	}
	
	public String getComposedTaskText() {
		return this.composedTaskText;
	}
	
	/**
	 * Shortcut to return the first node in the first sequence. Many composed tasks
	 * will have just one sequence so do not force the consumer to dig through that
	 * sequence.
	 * 
	 * @return the first node in the first sequence.
	 */
	public LabelledComposedTaskNode getStart() {
		if (sequences.size()==0) {
			return null;
		}
		else {
			return sequences.get(0);
		}
	}
	
	public List<LabelledComposedTaskNode> getSequences() {
		return sequences;
	}

	/**
	 * Find the sequence with the specified label and return it.
	 * @param label the label to search for
	 * @return the sequence with that label or null if there isn't one
	 */
	public LabelledComposedTaskNode getSequenceWithLabel(String label) {
		Assert.hasText(label,"label is required");
		for (LabelledComposedTaskNode ctn : sequences) {
			if (ctn.hasLabel() && ctn.getLabelString().equals(label)) {
				return ctn;
			}
		}
		return null;
	}

	/**
	 * @return the DSL representation of this composed task Ast
	 */
	public String toDSL() {
		return stringify(false);
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("ComposedTaskNode for ").append(composedTaskText.replaceAll("\n", ";"));
		s.append("\n").append(sequences);
		return s.toString();
	}

}
