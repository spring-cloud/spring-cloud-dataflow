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
package org.springframework.cloud.dataflow.server.stream;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.deployer.spi.app.DeploymentState;

/**
 * SPI for deploying the apps in a stream and providing information about the status of
 * those deployed apps.
 * @author Mark Pollack
 */
public interface StreamDeployer {

	// TODO return 'stream handle' stream name for AppDeployer impl, release name for Skipper
	// impl.
	// TODO store releaseName in SCDF for stream name?
	void deployStream(StreamDeploymentRequest streamDeploymentRequest);

	String calculateStreamState(String streamName);

	Map<StreamDefinition, DeploymentState> state(List<StreamDefinition> content);

}
