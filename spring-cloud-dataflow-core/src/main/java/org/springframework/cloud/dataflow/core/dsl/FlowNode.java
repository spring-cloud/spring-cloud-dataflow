/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.core.dsl;

import java.util.Collections;
import java.util.List;

/**
 * The AST node representing a flow. A flow is a series of things to execute sequentially.
 * Those things can themselves be individual task applications or splits. In DSL form a
 * flow is expressed like this:
 *
 * <pre>
 * {@code
 * aa && bb
 * }
 * </pre>
 *
 * @author Andy Clement
 */
public class FlowNode extends LabelledTaskNode {

	private List<LabelledTaskNode> series;

	FlowNode(List<LabelledTaskNode> nodes) {
		super(nodes.get(0).getStartPos(), nodes.get(nodes.size() - 1).getEndPos());
		this.series = Collections.unmodifiableList(nodes);
	}

	@Override
	public String stringify(boolean includePositionInfo) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < series.size(); i++) {
			if (i > 0) {
				s.append(" ").append(TokenKind.ANDAND.tokenChars).append(" ");
			}
			s.append(series.get(i).stringify(includePositionInfo));
		}
		return s.toString();
	}

	@Override
	public int getSeriesLength() {
		return series.size();
	}

	@Override
	public List<LabelledTaskNode> getSeries() {
		return series;
	}

	@Override
	public LabelledTaskNode getSeriesElement(int index) {
		return series.get(index);
	}

	@Override
	public boolean isFlow() {
		return true;
	}

	@Override
	public String toString() {
		return "[Flow:" + stringify(true) + "]";
	}

	@Override
	public void accept(TaskVisitor visitor) {
		boolean cont = visitor.preVisit(this);
		if (!cont) {
			return;
		}
		visitor.visit(this);
		for (LabelledTaskNode labelledTaskNode : series) {
			labelledTaskNode.accept(visitor);
		}
		visitor.postVisit(this);
	}
}
