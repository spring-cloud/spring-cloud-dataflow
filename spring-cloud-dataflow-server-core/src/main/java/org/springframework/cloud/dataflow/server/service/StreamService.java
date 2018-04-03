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
package org.springframework.cloud.dataflow.server.service;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.deployer.spi.app.DeploymentState;

/**
 * Provide deploy, undeploy, info and state operations on the stream.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
public interface StreamService {

	/**
	 * Create a new stream.
	 *
	 * @param streamName stream name
	 * @param dsl DSL definition for stream
	 * @param deploy if {@code true}, the stream is deployed upon creation (default is
	 * {@code false})
	 * @return the created stream definition already exists
	 * @throws InvalidStreamDefinitionException if there errors in parsing the stream DSL,
	 * resolving the name, or type of applications in the stream
	 */
	StreamDefinition createStream(String streamName, String dsl, boolean deploy);

	/**
	 * Deploys the stream with the user provided deployment properties.
	 * Implementations are responsible for expanding deployment wildcard expressions.
	 * @param name the name of the stream
	 * @param deploymentProperties deployment properties to use as passed in from the client.
	 */
	void deployStream(String name, Map<String, String> deploymentProperties);


	/**
	 * Un-deploys the stream identified by the given stream name.
	 *
	 * @param name the name of the stream to un-deploy
	 */
	void undeployStream(String name);

	/**
	 * Retrieve the deployment state for list of stream definitions.
	 *
	 * @param streamDefinitions the list of Stream definitions to calculate the deployment states.
	 * @return the map containing the stream definitions and their deployment states.
	 */
	Map<StreamDefinition, DeploymentState> state(List<StreamDefinition> streamDefinitions);

	/**
	 * Get stream information including the deployment properties etc.
	 * @param streamName the name of the stream
	 * @return the stream deployment information
	 */
	StreamDeployment info(String streamName);
}
