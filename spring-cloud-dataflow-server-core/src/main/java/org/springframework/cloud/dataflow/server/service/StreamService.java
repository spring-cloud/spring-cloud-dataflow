/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.dataflow.server.controller.support.InvalidStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Provide deploy, undeploy, info and state operations on the stream.
 * Uses the support for operations provided by Skipper.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
public interface StreamService {

	/**
	 * Scale application instances in a deployed stream.
	 * @param streamName the name of an existing stream definition (required)
	 * @param appName in stream application name to scale (required)
	 * @param count number of instances for the selected stream application (required)
	 * @param properties scale deployment specific properties (optional)
	 */
	void scaleApplicationInstances(String streamName, String appName, String count, Map<String, String> properties);

	/**
	 * Update the stream using the UpdateStreamRequest.
	 *
	 * @param streamName the name of the stream to update
	 * @param updateStreamRequest the UpdateStreamRequest to use during the update
	 */
	void updateStream(String streamName, UpdateStreamRequest updateStreamRequest);

	/**
	 * Rollback the stream to the previous or a specific version of the stream.
	 *
	 * @param streamName the name of the stream to rollback
	 * @param releaseVersion the version to rollback to (if not specified, rollback to the previous deleted/deployed
	 * release version of the stream.
	 */
	void rollbackStream(String streamName, int releaseVersion);

	/**
	 * Return a manifest info of a release version. For packages with dependencies, the
	 * manifest includes the contents of those dependencies.
	 *
	 * @param releaseName the release name
	 * @param releaseVersion the release version
	 * @return the manifest info of a release
	 */
	String manifest(String releaseName, int releaseVersion);

	/**
	 * Get stream's deployment history
	 * @param releaseName Stream release name
	 * @return List or Releases for this release name
	 */
	Collection<Release> history(String releaseName);

	/**
	 * @return list of supported deployment platforms
	 */
	Collection<Deployer> platformList();

	/**
	 * Create a new stream.
	 *
	 * @param streamName stream name
	 * @param dsl DSL definition for stream
	 * @param description description of the stream definition
	 * @param deploy if {@code true}, the stream is deployed upon creation (default is
	 * {@code false})
	 * @return the created stream definition already exists
	 * @throws InvalidStreamDefinitionException if there are errors in parsing the stream DSL,
	 * resolving the name, or type of applications in the stream
	 */
	StreamDefinition createStream(String streamName, String dsl, String description, boolean deploy);

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
	 * Delete the stream, including undeloying.
	 * @param name the name of the stream to delete
	 */
	void deleteStream(String name);

	/**
	 * Delete all streams, including undeploying.
	 */
	void deleteAll();

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

	/**
	 * Find streams related to the given stream name.
	 * @param name name of the stream
	 * @param nested if should recursively findByTaskNameContains for related stream definitions
	 * @return a list of related stream definitions
	 */
	List<StreamDefinition> findRelatedStreams(String name, boolean nested);

	/**
	 * Find stream definitions where the findByTaskNameContains parameter
	 * @param pageable Pagination information
	 * @param search the findByTaskNameContains parameter to use
	 * @return Page of stream definitions
	 */
	Page<StreamDefinition> findDefinitionByNameContains(Pageable pageable, String search);

	/**
	 * Find a stream definition by name.
	 * @param streamDefinitionName the name of the stream definition
	 * @return the stream definition
	 * @throws NoSuchStreamDefinitionException if the definition can not be found.
	 */
	StreamDefinition findOne(String streamDefinitionName);

	/**
	 * Verifies that all apps in the stream are valid.
	 * @param name the name of the definition
	 * @return  {@link ValidationStatus} for a stream.
	 */
	ValidationStatus validateStream(String name);
}
