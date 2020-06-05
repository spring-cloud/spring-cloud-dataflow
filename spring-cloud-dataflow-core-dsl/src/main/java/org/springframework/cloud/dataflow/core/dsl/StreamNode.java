/*
 * Copyright 2015 the original author or authors.
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
 * @author Andy Clement
 */
public class StreamNode extends AstNode {

	protected final String streamText;

	protected final String streamName;

	protected final List<AppNode> appNodes;

	protected SourceDestinationNode sourceDestinationNode;

	protected SinkDestinationNode sinkDestinationNode;

	public StreamNode(String streamText, String streamName, List<AppNode> appNodes,
			SourceDestinationNode sourceDestinationNode, SinkDestinationNode sinkDestinationNode) {
		super(appNodes.get(0).getStartPos(), appNodes.get(appNodes.size() - 1).getEndPos());
		this.streamText = streamText;
		this.streamName = streamName;
		this.appNodes = appNodes;
		this.sourceDestinationNode = sourceDestinationNode;
		this.sinkDestinationNode = sinkDestinationNode;
	}

	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		// s.append("Stream[").append(streamText).append("]");
		s.append("[");
		if (getStreamName() != null) {
			s.append(getStreamName()).append(" = ");
		}
		if (sourceDestinationNode != null) {
			s.append(sourceDestinationNode.stringify(includePositionalInfo));
		}
		for (AppNode appNode : appNodes) {
			s.append(appNode.stringify(includePositionalInfo));
		}
		if (sinkDestinationNode != null) {
			s.append(sinkDestinationNode.stringify(includePositionalInfo));
		}
		s.append("]");
		return s.toString();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if (getStreamName() != null) {
			s.append(getStreamName()).append(" = ");
		}
		if (sourceDestinationNode != null) {
			s.append(sourceDestinationNode.toString());
		}
		for (int m = 0; m < appNodes.size(); m++) {
			AppNode appNode = appNodes.get(m);
			s.append(appNode.toString());
			if (m + 1 < appNodes.size()) {
				if (appNode.isUnboundStreamApp()) {
					s.append(" || ");
				}
				else {
					s.append(" | ");
				}
			}
		}
		if (sinkDestinationNode != null) {
			s.append(sinkDestinationNode.toString());
		}
		return s.toString();
	}

	public List<AppNode> getAppNodes() {
		return appNodes;
	}

	public SourceDestinationNode getSourceDestinationNode() {
		return sourceDestinationNode;
	}

	public SinkDestinationNode getSinkDestinationNode() {
		return sinkDestinationNode;
	}

	public String getStreamName() {
		return streamName;
	}

	/**
	 * Find the first reference to the named app in the stream. If the same app is
	 * referred to multiple times the secondary references cannot be accessed via this
	 * method.
	 *
	 * @param  appName the name of the app
	 * @return the first occurrence of the named app in the stream
	 */
	public AppNode getApp(String appName) {
		for (AppNode appNode : appNodes) {
			if (appNode.getName().equals(appName)) {
				return appNode;
			}
		}
		return null;
	}

	public int getIndexOfLabel(String labelOrAppName) {
		for (int m = 0; m < appNodes.size(); m++) {
			AppNode appNode = appNodes.get(m);
			if (appNode.getLabelName().equals(labelOrAppName)) {
				return m;
			}
		}
		return -1;
	}

	public String getStreamData() {
		return toString();
	}

	public String getStreamText() {
		return this.streamText;
	}

	public String getName() {
		return this.streamName;
	}

}
