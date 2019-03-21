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

package org.springframework.cloud.dataflow.core.dsl.graph;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Represents a link in a {@link Graph} object that Flo will display as a link.
 *
 * @author Andy Clement
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public class Link {

	public final static String PROPERTY_TRANSITION_NAME = "transitionName";

	public String from;

	public String to;

	/**
	 * Properties on a link can capture the name of a potential transition that would lead
	 * to this link being taken when the 'from' job completes.
	 */
	public Map<String, String> properties = null;

	Link() {

	}

	public Link(String sourceId, String targetId) {
		this.from = sourceId;
		this.to = targetId;
	}

	public Link(String sourceId, String targetId, String transitionName) {
		this.from = sourceId;
		this.to = targetId;
		properties = new HashMap<>();
		properties.put(PROPERTY_TRANSITION_NAME, transitionName);
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("Link[from=").append(from).append(",to=").append(to);
		if (properties != null) {
			s.append(",properties=").append(properties);
		}
		s.append("]");
		return s.toString();
	}

	public boolean hasTransitionSet() {
		return properties != null && properties.containsKey(PROPERTY_TRANSITION_NAME);
	}

	@JsonIgnore
	public String getTransitionName() {
		return properties != null ? properties.get(PROPERTY_TRANSITION_NAME) : null;
	}

	public void updateFrom(String newFrom) {
		this.from = newFrom;
	}

	public void updateTo(String newTo) {
		this.to = newTo;
	}
}
