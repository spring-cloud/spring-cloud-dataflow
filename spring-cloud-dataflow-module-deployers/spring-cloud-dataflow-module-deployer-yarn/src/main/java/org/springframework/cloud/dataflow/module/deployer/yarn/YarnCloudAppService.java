/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.module.deployer.yarn;

import java.util.Collection;
import java.util.Map;

/**
 * Interface used to glue a state machine and yarn application logic together.
 *
 * @author Janne Valkealahti
 */
public interface YarnCloudAppService {

	/**
	 * Get applications pushed to hdfs.
	 *
	 * @return the applications
	 */
	Collection<CloudAppInfo> getApplications();

	/**
	 * Get running application instances.
	 *
	 * @return the instances
	 */
	Collection<CloudAppInstanceInfo> getInstances();

	/**
	 * Push new application into hdfs. Push operation is copying needed
	 * files into hdfs without trying to start a new application instance.
	 *
	 * @param appVersion the app version
	 */
	void pushApplication(String appVersion);

	/**
	 * Submit new application instance into yarn. Prior to calling a new
	 * submit operation, application has to exist in hdfs which can be done
	 * i.e. using {@link #pushApplication(String)} method.
	 *
	 * @param appVersion the app version
	 * @return the application id
	 */
	String submitApplication(String appVersion);

	/**
	 * Creates the container cluster.
	 *
	 * @param yarnApplicationId the yarn application id
	 * @param clusterId the cluster id
	 * @param count the count
	 * @param module the module
	 * @param definitionParameters the definition parameters
	 */
	void createCluster(String yarnApplicationId, String clusterId, int count, String module,
			Map<String, String> definitionParameters);

	/**
	 * Start a container cluster.
	 *
	 * @param yarnApplicationId the yarn application id
	 * @param clusterId the cluster id
	 */
	void startCluster(String yarnApplicationId, String clusterId);

	/**
	 * Stop a container cluster.
	 *
	 * @param yarnApplicationId the yarn application id
	 * @param clusterId the cluster id
	 */
	void stopCluster(String yarnApplicationId, String clusterId);

	/**
	 * Gets the clusters states. Returned map has a mapping between
	 * yarn container cluster id and its state known by application master
	 * using container clusters.
	 *
	 * @return the clusters states
	 */
	Map<String, String> getClustersStates();

	/**
	 * Gets the clusters.
	 *
	 * @param yarnApplicationId the yarn application id
	 * @return the clusters
	 */
	Collection<String> getClusters(String yarnApplicationId);

	/**
	 * Destroy cluster.
	 *
	 * @param yarnApplicationId the yarn application id
	 * @param clusterId the cluster id
	 */
	void destroyCluster(String yarnApplicationId, String clusterId);

	/**
	 * Wrapping info about application pushed into hdfs.
	 */
	public class CloudAppInfo {

		private final String name;

		public CloudAppInfo(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

	}

	/**
	 * Wrapping info about running application.
	 */
	public class CloudAppInstanceInfo {

		private final String applicationId;
		private final String name;
		private final String address;

		public CloudAppInstanceInfo(String applicationId, String name, String address) {
			this.applicationId = applicationId;
			this.name = name;
			this.address = address;
		}

		public String getApplicationId() {
			return applicationId;
		}

		public String getName() {
			return name;
		}

		public String getAddress() {
			return address;
		}

	}

}
