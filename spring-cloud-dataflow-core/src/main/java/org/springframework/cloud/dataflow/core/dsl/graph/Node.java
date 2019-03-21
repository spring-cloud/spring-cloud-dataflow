/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.core.dsl.graph;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a node in a {@link Graph} object that Flo will display as a block.
 *
 * @author Andy Clement
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public class Node {

	public static final String METADATA_LABEL = "label";

	public String id;

	public String name;

	// Properties are those supported by the node
	public Map<String, String> properties;

	// Metadata is other information about the node (e.g. any label in use)
	public Map<String, String> metadata;

	Node() {
	}

	public Node(String id, String name) {
		this.id = id;
		this.name = name;
		this.properties = null;
	}

	public Node(String id, String name, Map<String, String> properties) {
		this.id = id;
		this.name = name;
		this.properties = properties;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("Node[id=").append(id).append(",name=").append(name);
		if (properties != null) {
			s.append(",properties=").append(properties);
		}
		s.append("]");
		return s.toString();
	}

	@JsonIgnore
	public boolean isStart() {
		return name.equals("START");
	}

	@JsonIgnore
	public boolean isEnd() {
		return name.equals("END");
	}

	@JsonIgnore
	public boolean isFail() {
		return name.equals("FAIL");
	}

	@JsonIgnore
	public boolean isSync() {
		return name.equals("SYNC");
	}

	@JsonIgnore
	public String getLabel() {
		if (metadata == null) {
			return null;
		}
		else {
			return metadata.get(METADATA_LABEL);
		}
	}

	@JsonIgnore
	public void setLabel(String label) {
		if (metadata == null) {
			metadata = new HashMap<>();
		}
		metadata.put(METADATA_LABEL, label);
	}
}
