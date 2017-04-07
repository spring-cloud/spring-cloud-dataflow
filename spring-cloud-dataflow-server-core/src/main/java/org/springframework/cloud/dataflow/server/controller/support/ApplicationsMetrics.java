/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.server.controller.support;

import java.util.List;
import java.util.Map;

/**
 * Support domain class to map metrics response from a collector.
 *
 * @author Janne Valkealahti
 *
 */
public class ApplicationsMetrics {

	private String name;
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

	public static class Application {

		private String name;
		private List<Instance> instances;
		private double incomingRate;
		private double outgoingRate;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Instance> getInstances() {
			return instances;
		}

		public void setInstances(List<Instance> instances) {
			this.instances = instances;
		}

		public double getIncomingRate() {
			return incomingRate;
		}

		public void setIncomingRate(double incomingRate) {
			this.incomingRate = incomingRate;
		}

		public double getOutgoingRate() {
			return outgoingRate;
		}

		public void setOutgoingRate(double outgoingRate) {
			this.outgoingRate = outgoingRate;
		}
	}

	public static class Instance {

		private String guid;
		private int index;
		private Map<String, Object> properties;
		private double incomingRate;
		private double outgoingRate;

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

		public double getIncomingRate() {
			return incomingRate;
		}

		public void setIncomingRate(double incomingRate) {
			this.incomingRate = incomingRate;
		}

		public double getOutgoingRate() {
			return outgoingRate;
		}

		public void setOutgoingRate(double outgoingRate) {
			this.outgoingRate = outgoingRate;
		}
	}
}
