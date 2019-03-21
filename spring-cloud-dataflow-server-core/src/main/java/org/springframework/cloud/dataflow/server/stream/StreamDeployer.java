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
 */
public interface StreamDeployer {

	DeploymentState calculateStreamState(String streamName);

	Map<StreamDefinition, DeploymentState> state(List<StreamDefinition> content);

	/**
	 * Undeploys the stream identified by the given stream name.
	 *
	 * @param name the name of the stream to un-deploy
	 */
	void undeployStream(String name);

	Page<AppStatus> getAppStatuses(Pageable pageable) throws ExecutionException, InterruptedException;

	AppStatus getAppStatus(String id);

	RuntimeEnvironmentInfo environmentInfo();

	/**
	 * Get stream information (including the deployment properties) for the given stream name.
	 * @param streamName the name of the stream
	 * @return stream deployment information
	 */
	StreamDeployment getStreamInfo(String streamName);
}
