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

import java.util.List;

/**
 * The main task supertype AST node. Other nodes like {@link FlowNode} and
 * {@link SplitNode} extend this one, relying on this one for common functionality.
 *
 * @author Andy Clement
 */
public abstract class LabelledTaskNode extends AstNode {

	private Token label;

	LabelledTaskNode(int startPos, int endPos) {
		super(startPos, endPos);
	}

	/**
	 * @return true if this node represents a flow (a number of jobs to run in sequence)
	 */
	public boolean isFlow() {
		return false;
	}

	/**
	 * @return true if this node represents a split (a number of jobs to run in parallel)
	 */
	public boolean isSplit() {
		return false;
	}

	/**
	 * @return true if this node represents a inlined job definition or simple job
	 * reference, otherwise false
	 */
	public boolean isTaskApp() {
		return false;
	}

	/**
	 * For nodes representing a flow or split, return how many are in the series of things
	 * to run (so the number to run in sequence or the number to run in parallel).
	 *
	 * @return the length of the series for flow or split nodes, otherwise -1
	 */
	public int getSeriesLength() {
		return -1;
	}

	/**
	 * For nodes representing a flow or a split, return a specific element in the series
	 * of things that will run either sequentially or in parallel.
	 *
	 * @param index the element of interest
	 * @return the indicated series or null if this is not a flow/split node
	 */
	public LabelledTaskNode getSeriesElement(int index) {
		return null;
	}

	/**
	 * For nodes representing a flow or a split, return the series of elements to run in
	 * sequence/parallel.
	 *
	 * @return the series of elements for this flow/split, or null if not a flow/split
	 * node
	 */
	public List<LabelledTaskNode> getSeries() {
		return null;
	}

	protected boolean hasLabel() {
		return this.label != null;
	}

	protected String getLabelString() {
		return hasLabel() ? this.label.stringValue() : null;
	}

	public Token getLabel() {
		return this.label;
	}

	public void setLabel(Token label) {
		this.label = label;
	}

	public abstract void accept(TaskVisitor visitor);

}
