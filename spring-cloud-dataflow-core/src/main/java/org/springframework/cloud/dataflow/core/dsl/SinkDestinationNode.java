/*
 * Copyright 2015-2016 the original author or authors.
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

/**
 * @author Andy Clement
 */
public class SinkDestinationNode extends AstNode {

	private final DestinationNode destinationNode;

	public SinkDestinationNode(DestinationNode destinationNode, int startPos) {
		super(startPos, destinationNode.endPos);
		this.destinationNode = destinationNode;
	}

	@Override
	public String stringify(boolean includePositionalInfo) {
		return ">" + destinationNode.stringify(includePositionalInfo);
	}

	@Override
	public String toString() {
		return " > " + destinationNode.toString();
	}

	public DestinationNode getDestinationNode() {
		return destinationNode;
	}

	public String getDestinationName() {
		return destinationNode.getDestinationName();
	}

	public SinkDestinationNode copyOf() {
		return new SinkDestinationNode(destinationNode.copyOf(), super.startPos);
	}

}
