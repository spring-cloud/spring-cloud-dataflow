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
package org.springframework.cloud.dataflow.server.stream;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
	 * @throws ExecutionException if the computation threw an exception
	 * @throws InterruptedException if the current thread was interrupted while waiting
	 */
	Page<AppStatus> getAppStatuses(Pageable pageable) throws ExecutionException, InterruptedException;

	/**
	 * Gets runtime application status
	 * @param appDeploymentId the id of the application instance running in the target runtime environment
	 * @return app status
	 */
	AppStatus getAppStatus(String appDeploymentId);

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
}
