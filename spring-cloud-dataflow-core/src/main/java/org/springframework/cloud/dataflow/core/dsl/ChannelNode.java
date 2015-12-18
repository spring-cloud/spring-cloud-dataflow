/*
 * Copyright 2015 the original author or authors.
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
 */
public class ChannelNode extends AstNode {

	private ChannelType channelType;

	private List<String> nameComponents;

	private List<String> indexingElements;

	public ChannelNode(ChannelType channelType, int startPos, int endPos, List<String> nameElements,
			List<String> indexingElements) {
		super(startPos, endPos);
		this.channelType = channelType;
		this.nameComponents = nameElements;
		this.indexingElements = indexingElements;
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
		if (channelType.isTap()) {
			s.append(channelType.getStringRepresentation());
		}
		if (nameComponents.size() > 0 && channelType.isTap() &&
				nameComponents.get(0).equalsIgnoreCase(channelType.tapSource().name())) {
			t = 1;
		}
		for (int max = nameComponents.size(); t < max; t++) {
			s.append(nameComponents.get(t));
			if (t < nameComponents.size() - 1) {
				s.append(":");
			}
		}
		if (indexingElements.size() != 0) {
			for (int t2 = 0, max = indexingElements.size(); t2 < max; t2++) {
				s.append(".");
				s.append(indexingElements.get(t2));
			}
		}
	}

	String getChannelName() {
		StringBuilder s = new StringBuilder();
		if (channelType.isTap()) {
			s.append("tap:");
		}
		s.append(getNameComponents());
		s.append(getIndexingComponents());
		return s.toString();
	}

	private String getNameComponents() {
		StringBuilder s = new StringBuilder();
		for (int t = 0, max = nameComponents.size(); t < max; t++) {
			if (t > 0) {
				s.append(":");
			}
			s.append(nameComponents.get(t));
		}
		return s.toString();
	}

	private String getIndexingComponents() {
		StringBuilder s = new StringBuilder();
		for (int t = 0, max = indexingElements.size(); t < max; t++) {
			s.append(".");
			s.append(indexingElements.get(t));
		}
		return s.toString();
	}

	ChannelType getChannelType() {
		return this.channelType;
	}

	public ChannelNode copyOf() {
		return new ChannelNode(this.channelType, super.startPos, super.endPos, nameComponents, indexingElements);
	}
}
