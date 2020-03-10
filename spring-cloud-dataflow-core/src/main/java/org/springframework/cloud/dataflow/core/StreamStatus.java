/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

import java.util.List;
import java.util.Map;

/**
 * Domain class that represents the runtime stream status
 *
 * @author Janne Valkealahti
 * @author Christian Tzolov
 */
public class StreamStatus {

	private String name;

	private String version;

	private List<Application> applications;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Application> getApplications() {
		return applications;
	}

	public void setApplications(List<Application> applications) {
		this.applications = applications;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public static class Application {

		private String name;

		private String id;

		private List<Instance> instances;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public List<Instance> getInstances() {
			return instances;
		}

		public void setInstances(List<Instance> instances) {
			this.instances = instances;
		}
	}

	public static class Instance {

		private String id;

		private String guid;

		private int index;

		private Map<String, Object> properties;

		private String state;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getGuid() {
			return guid;
		}

		public void setGuid(String guid) {
			this.guid = guid;
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public Map<String, Object> getProperties() {
			return properties;
		}

		public void setProperties(Map<String, Object> properties) {
			this.properties = properties;
		}

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}
	}
}
