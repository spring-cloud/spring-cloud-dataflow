/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.skipper.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Payload in a scaling request.
 *
 * @author Janne Valkealahti
 */
public class ScaleRequest {

	private List<ScaleRequestItem> scale = new ArrayList<>();

	public ScaleRequest() {
	}

	public ScaleRequest(List<ScaleRequestItem> scale) {
		if (scale != null) {
			this.scale = scale;
		}
	}

	public List<ScaleRequestItem> getScale() {
		return scale;
	}

	public void setScale(List<ScaleRequestItem> scale) {
		this.scale = scale;
	}

	/**
	 * Create a {@code ScaleRequest} having one app with its count.
	 *
	 * @param name the app name
	 * @param count the app count
	 * @return the scale request
	 */
	public static ScaleRequest of(String name, Integer count) {
		return new ScaleRequest(Arrays.asList(new ScaleRequestItem(name, count, null)));
	}

	/**
	 * Create a {@code ScaleRequest} having one app with its count and properties.
	 *
	 * @param name the app name
	 * @param count the app count
	 * @param properties the app properties
	 * @return the scale request
	 */
	public static ScaleRequest of(String name, Integer count, Map<String, String> properties) {
		return new ScaleRequest(Arrays.asList(new ScaleRequestItem(name, count, properties)));
	}

	@Override
	public String toString() {
		return (this.scale != null) ? this.scale.toString() : super.toString();
	}

	/**
	 * As {@link ScaleRequest} can contain multiple requests for multiple app, this
	 * class represents one of those.
	 */
	public static class ScaleRequestItem {

		private String name;
		private Integer count;
		private Map<String, String> properties = new HashMap<>();

		public ScaleRequestItem() {
		}

		public ScaleRequestItem(String name, Integer count) {
			this(name, count, null);
		}

		public ScaleRequestItem(String name, Integer count, Map<String, String> properties) {
			this.name = name;
			this.count = count;
			if (properties != null) {
				this.properties = properties;
			}
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getCount() {
			return count;
		}

		public void setCount(Integer count) {
			this.count = count;
		}

		public Map<String, String> getProperties() {
			return properties;
		}

		public void setProperties(Map<String, String> properties) {
			this.properties = properties;
		}

		@Override
		public String toString() {
			return  "ScaleRequest [appName= " + this.name + ", count= " + this.count + ", properties= " +
					this.properties.toString() + "]";
		}
	}
}
