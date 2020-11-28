/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.naming.OperationNotSupportedException;

import org.springframework.cloud.dataflow.rest.resource.StreamAppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.hateoas.PagedModel;

/**
 * Interface defining operations available against streams.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
public interface StreamOperations {

	/**
	 * @return the list streams known to the system.
	 */
	PagedModel<StreamDefinitionResource> list();

	/**
	 * @param name the name of the stream
	 * @return retrieve the stream info.
	 */
	StreamDeploymentResource info(String name);

	/**
	 * Create a new stream, optionally deploying it.
	 *
	 * @param name the name of the stream
	 * @param definition the stream definition DSL
	 * @param description the description of the stream
	 * @param deploy whether to deploy the stream after creating its definition
	 * @return the new stream definition
	 */
	StreamDefinitionResource createStream(String name, String definition, String description, boolean deploy);

	/**
	 * Deploy an already created stream.
	 *
	 * @param name the name of the stream
	 * @param properties the deployment properties
	 */
	void deploy(String name, Map<String, String> properties);

	/**
	 * Undeploy a deployed stream, retaining its definition.
	 *
	 * @param name the name of the stream
	 */
	void undeploy(String name);

	/**
	 * Undeploy all currently deployed streams.
	 */
	void undeployAll();

	/**
	 * Destroy an existing stream.
	 *
	 * @param name the name of the stream
	 */
	void destroy(String name);

	/**
	 * Destroy all streams known to the system.
	 */
	void destroyAll();

	/**
	 * Update the stream given its corresponding releaseName in Skipper using the specified
	 * package and updated yaml config.
	 * @param streamName the name of the stream to update
	 * @param releaseName the corresponding release name of the stream in skipper
	 * @param packageIdentifier the package that corresponds to this stream
	 * @param updateProperties a map of properties to use for updating the stream
	 * @param force boolean flag to indicate if the stream update is enforced irrespective of
	 * differences from the existing stream
	 * @param appNames app names to use for the stream update when update is enforced
	 */
	void updateStream(String streamName, String releaseName, PackageIdentifier packageIdentifier,
			Map<String, String> updateProperties, boolean force, List<String> appNames);

	/**
	 * Rollback the stream to the previous or a specific release version.
	 *
	 * @param streamName the name of the stream to rollback
	 * @param version the version to rollback to. If the version is 0, then rollback to the
	 * previous release. The version can not be less than zero.
	 */
	void rollbackStream(String streamName, int version);

	/**
	 * Queries the server for the stream definition.
	 * @param streamName the name of the stream to get status
	 * @return The current stream definition with updated status
	 */
	StreamDefinitionResource getStreamDefinition(String streamName);

	/**
	 * Get manifest for the given stream deployed via Skipper. Optionally, the version can be
	 * used to retrieve the version for a specific version of the stream.
	 * @param streamName the stream(release) name
	 * @param version the version of the release
	 * @return the manifest for the given stream and version
	 */
	String getManifest(String streamName, int version);

	/**
	 * Get the history of releases for the given stream deployed via Skipper.
	 * @param streamName the stream(release) name
	 * @return the history of releases for the stream
	 */
	Collection<Release> history(String streamName);

	/**
	 * @return the list of all Skipper platforms
	 */
	Collection<Deployer> listPlatforms();

	/**
	 * Return the validation status for the apps in an stream.
	 * @param streamDefinitionName The name of the stream definition to be validated.
	 * @return {@link StreamAppStatusResource} containing the stream app statuses.
	 * @throws OperationNotSupportedException if the server does not support stream validation
	 */
	StreamAppStatusResource validateStreamDefinition(String streamDefinitionName) throws OperationNotSupportedException;

	/**
	 * Scales number of application instances in a stream.
	 *
	 * @param streamName the stream(release) name.
	 * @param appName name of application in the stream to scale.
	 * @param count number of instances to scale to.
	 * @param properties scale deployment properties.
	 */
	void scaleApplicationInstances(String streamName, String appName, Integer count, Map<String, String> properties);

	/**
	 * Retrieves all the applications' logs for the given stream name
	 * @param streamName name of stream for which to get logs
	 * @return logs of said stream
	 */
	String streamExecutionLog(String streamName);

	/**
	 * Retrieve the logs of a specific application from the stream
	 * @param streamName name of stream for which to get logs
	 * @param appName app name for which to get logs
	 * @return logs for said application within said stream
	 */
	String streamExecutionLog(String streamName, String appName);
}
