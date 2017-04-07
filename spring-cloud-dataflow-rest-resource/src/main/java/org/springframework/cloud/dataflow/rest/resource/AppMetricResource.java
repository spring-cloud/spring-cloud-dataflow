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

package org.springframework.cloud.dataflow.rest.resource;

import java.util.HashMap;
import java.util.Map;

import org.springframework.hateoas.ResourceSupport;

/**
 * Rest resources for runtime metrics response.
 *
 * @author Janne Valkealahti
 *
 */
public class AppMetricResource extends ResourceSupport {

	private MetricsHolder entity;

	/**
	 * Instantiates a new app metric resource.
	 *
	 * @param entity the entity
	 */
	public AppMetricResource(MetricsHolder entity) {
		this.entity = entity;
	}

	/**
	 * Gets the guid.
	 *
	 * @return the guid
	 */
	public String getGuid() {
		return entity.getGuid();
	}

	/**
	 * Gets the deployment id.
	 *
	 * @return the deployment id
	 */
	public String getDeploymentId() {
		return entity.getDeploymentId();
	}

	/**
	 * Gets the instance id.
	 *
	 * @return the instance id
	 */
	public String getInstanceId() {
		return entity.getInstanceId();
	}

	/**
	 * Gets the metrics.
	 *
	 * @return the metrics
	 */
	public Map<String, Number> getMetrics() {
		return entity.getMetrics();
	}

	/**
	 * Entity class for metrics.
	 */
	public static class MetricsHolder {

		private String guid;
		private String deploymentId;
		private String instanceId;
		private final Map<String, Number> metrics = new HashMap<>();

		/**
		 * Instantiates a new metrics holder.
		 *
		 * @param guid the guid
		 * @param deploymentId the deployment id
		 * @param instanceId the instance id
		 */
		public MetricsHolder(String guid, String deploymentId, String instanceId) {
			this.guid = guid;
			this.deploymentId = deploymentId;
			this.instanceId = instanceId;
		}

		/**
		 * Gets the guid.
		 *
		 * @return the guid
		 */
		public String getGuid() {
			return guid;
		}

		/**
		 * Gets the deployment id.
		 *
		 * @return the deployment id
		 */
		public String getDeploymentId() {
			return deploymentId;
		}

		/**
		 * Gets the instance id.
		 *
		 * @return the instance id
		 */
		public String getInstanceId() {
			return instanceId;
		}

		/**
		 * Gets the metrics.
		 *
		 * @return the metrics
		 */
		public Map<String, Number> getMetrics() {
			return metrics;
		}
	}
}
