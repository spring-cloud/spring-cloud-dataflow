/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.core.dsl;

/**
 * Represents a label attached to a module.
 *
 * @author Andy Clement
 */
public class LabelNode extends AstNode {

	private final String label;

	public LabelNode(String label, int startpos, int endpos) {
		super(startpos, endpos);
		this.label = label;
	}

	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append("(").append("Label:").append(label);
		if (includePositionalInfo) {
			s.append(":");
			s.append(getStartPos()).append(">").append(getEndPos());
		}
		s.append(")");
		return s.toString();
	}

	public String toString() {
		return label + ":";
	}

	public String getLabelName() {
		return label;
	}

}
