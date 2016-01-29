/*
 * Copyright 2015-2016 the original author or authors.
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
 * @author Andy Clement
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
public class ChannelNode extends AstNode {

	private String nameComponent;

	private List<String> labelComponents;

	public ChannelNode(int startPos, int endPos, String nameComponent, List<String> labelComponents) {
		super(startPos, endPos);
		this.nameComponent = nameComponent;
		this.labelComponents = labelComponents;
	}

	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append("(");
		produceStringRepresentation(s);
		if (includePositionalInfo) {
			s.append(":");
			s.append(getStartPos()).append(">").append(getEndPos());
		}
		s.append(")");
		return s.toString();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		produceStringRepresentation(s);
		return s.toString();
	}

	private void produceStringRepresentation(StringBuilder s) {
		int t = 0;
		s.append(nameComponent);
		if (labelComponents.size() != 0) {
			for (int t2 = 0, max = labelComponents.size(); t2 < max; t2++) {
				s.append(".");
				s.append(labelComponents.get(t2));
			}
		}
	}

	String getChannelName() {
		StringBuilder s = new StringBuilder();
		s.append(this.nameComponent);
		s.append(getLabelComponents());
		return s.toString();
	}

	private String getLabelComponents() {
		StringBuilder s = new StringBuilder();
		for (int t = 0, max = labelComponents.size(); t < max; t++) {
			s.append(".");
			s.append(labelComponents.get(t));
		}
		return s.toString();
	}

	public ChannelNode copyOf() {
		return new ChannelNode(super.startPos, super.endPos, nameComponent, labelComponents);
	}
}
