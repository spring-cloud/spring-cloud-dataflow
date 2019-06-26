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
package org.springframework.cloud.dataflow.server.stream;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * SPI for handling deployment related operations on the apps in a stream.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
public interface StreamDeployer {

	/**
	 * Get the deployment state of a stream. Stream state is computed from the deployment states of its apps.
	 * @param streamName stream name
	 * @return the deployment status
	 */
	DeploymentState streamState(String streamName);

	/**
	 * Get the deployment states for a list of stream definitions
	 * @param streamDefinitions list of stream definitions
	 * @return map of stream definition and its corresponding deployment state
	 */
	Map<StreamDefinition, DeploymentState> streamsStates(List<StreamDefinition> streamDefinitions);

	/**
	 * Returns application statuses of all deployed applications
	 * @param pageable Pagination information
	 * @return pagable list of all app statuses
	 */
	Page<AppStatus> getAppStatuses(Pageable pageable);

	/**
	 * Gets runtime application status
	 * @param appDeploymentId the id of the application instance running in the target runtime environment
	 * @return app status
	 */
	AppStatus getAppStatus(String appDeploymentId);

	/**
	 * Returns the deployed application statuses part for the streamName stream.
	 * @param streamName Stream name to retrieve the runtime application statues for
	 * @return List of runtime application statues, part of the requested streamName.
	 */
	List<AppStatus> getStreamStatuses(String streamName);

	/**
	 * @return the runtime environment info for deploying streams.
	 */
	RuntimeEnvironmentInfo environmentInfo();

	/**
	 * Get stream information (including the deployment properties) for the given stream name.
	 * @param streamName the name of the stream
	 * @return stream deployment information
	 */
	StreamDeployment getStreamInfo(String streamName);

	/**
	 * Returns the logs of all the applications of the stream identified by the stream name.
	 *
	 * @param streamName the stream name
	 * @return the logs content
	 */
	String getLog(String streamName);

	/**
	 * Returns the logs of a specific application in the stream identified by the stream name.
	 *
	 * @param streamName the stream name
	 * @param appName specific application name inside the stream
	 * @return the logs content
	 */
	String getLog(String streamName, String appName);
}
