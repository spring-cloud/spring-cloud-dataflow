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

import java.util.Collections;
import java.util.List;

/**
 * The AST node representing a split. A split is a series of things to execute in
 * parallel. Those things can themselves be individual flows or further splits. In DSL
 * form a split is expressed like this:
 *
 * <pre>
 * <tt><aa || bb></tt>
 * </pre>
 *
 * .
 *
 * @author Andy Clement
 */
public class SplitNode extends LabelledTaskNode {

	private List<LabelledTaskNode> parallelTaskApps;

	SplitNode(int startpos, int endpos, List<LabelledTaskNode> parallelSequences) {
		super(startpos, endpos);
		this.parallelTaskApps = Collections.unmodifiableList(parallelSequences);
	}

	@Override
	public String stringify(boolean includePositionInfo) {
		if (parallelTaskApps.size() == 1) {
			return parallelTaskApps.get(0).stringify(includePositionInfo);
		}
		else {
			StringBuilder s = new StringBuilder();
			s.append(TokenKind.LT.tokenChars);
			for (int i = 0; i < parallelTaskApps.size(); i++) {
				LabelledTaskNode jn = parallelTaskApps.get(i);
				if (i > 0) {
					s.append(" ").append(TokenKind.DOUBLEPIPE.tokenChars).append(" ");
				}
				s.append(jn.stringify(includePositionInfo));
			}
			s.append(TokenKind.GT.tokenChars);
			return s.toString();
		}
	}

	@Override
	public int getSeriesLength() {
		return parallelTaskApps.size();
	}

	@Override
	public LabelledTaskNode getSeriesElement(int index) {
		return parallelTaskApps.get(index);
	}

	@Override
	public List<LabelledTaskNode> getSeries() {
		return parallelTaskApps;
	}

	@Override
	public boolean isSplit() {
		return true;
	}

	@Override
	public String toString() {
		return "[Split:" + stringify(true) + "]";
	}

	@Override
	public void accept(TaskVisitor visitor) {
		boolean cont = visitor.preVisit(this);
		if (!cont) {
			return;
		}
		visitor.visit(this);
		for (LabelledTaskNode labelledTaskNode : parallelTaskApps) {
			labelledTaskNode.accept(visitor);
		}
		visitor.postVisit(this);
	}
}
