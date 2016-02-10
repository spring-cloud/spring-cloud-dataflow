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
 * @author Mark Fisher
 */
public class ChannelNode extends AstNode {

	private List<String> nameComponents;

	public ChannelNode(int startPos, int endPos, List<String> nameComponents) {
		super(startPos, endPos);
		this.nameComponents = nameComponents;
	}

	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append("(");
		s.append(getChannelName());
		if (includePositionalInfo) {
			s.append(":");
			s.append(getStartPos()).append(">").append(getEndPos());
		}
		s.append(")");
		return s.toString();
	}

	@Override
	public String toString() {
		return getChannelName();
	}

	String getChannelName() {
		StringBuilder s = new StringBuilder();
		for (int t = 0, max = nameComponents.size(); t < max; t++) {
			if (t != 0) {
				s.append(".");
			}
			s.append(nameComponents.get(t));
		}
		return s.toString();
	}

	public ChannelNode copyOf() {
		return new ChannelNode(super.startPos, super.endPos, nameComponents);
	}
}
