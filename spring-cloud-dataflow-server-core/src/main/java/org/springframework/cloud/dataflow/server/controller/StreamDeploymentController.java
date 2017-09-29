/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamPropertyKeys;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentKey;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.impl.DefaultStreamService;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for deployment operations on {@link StreamDefinition}.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 * @author Janne Valkealahti
 */
@RestController
@RequestMapping("/streams/deployments")
@ExposesResourceFor(StreamDeploymentResource.class)
public class StreamDeploymentController {

	/**
	 * This is the spring boot property key that Spring Cloud Stream uses to filter the
	 * metrics to import when the specific Spring Cloud Stream "applicaiton" trigger is fired
	 * for metrics export.
	 */
	private static final String METRICS_TRIGGER_INCLUDES = "spring.metrics.export.triggers.application.includes";
	private static Log logger = LogFactory.getLog(StreamDeploymentController.class);
	private final DefaultStreamService defaultStreamService;

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	private final StreamDefinitionRepository repository;

	/**
	 * The repository this controller will use for deployment IDs.
	 */
	private final DeploymentIdRepository deploymentIdRepository;

	/**
	 * The app registry this controller will use to lookup apps.
	 */
	private final AppRegistry registry;

	/**
	 * The deployer this controller will use to deploy stream apps.
	 */
	private final AppDeployer deployer;

	private final WhitelistProperties whitelistProperties;

	/**
	 * General properties to be applied to applications on deployment.
	 */
	private final CommonApplicationProperties commonApplicationProperties;

	/**
	 * Create a {@code StreamDeploymentController} that delegates
	 * <ul>
	 * <li>CRUD operations to the provided {@link StreamDefinitionRepository}</li>
	 * <li>app retrieval to the provided {@link AppRegistry}</li>
	 * <li>deployment operations to the provided {@link AppDeployer}</li>
	 * </ul>
	 *
	 * @param repository the repository this controller will use for stream CRUD operations
	 * @param deploymentIdRepository the repository this controller will use for deployment
	 * IDs
	 * @param registry the registry this controller will use to lookup apps
	 * @param deployer the deployer this controller will use to deploy stream apps
	 * @param metadataResolver the application metadata resolver
	 * @param commonProperties common set of application properties
	 */
	public StreamDeploymentController(StreamDefinitionRepository repository,
									  DeploymentIdRepository deploymentIdRepository, AppRegistry registry, AppDeployer deployer,
									  ApplicationConfigurationMetadataResolver metadataResolver, CommonApplicationProperties commonProperties,
									  DefaultStreamService defaultStreamService) {
		Assert.notNull(repository, "StreamDefinitionRepository must not be null");
		Assert.notNull(deploymentIdRepository, "DeploymentIdRepository must not be null");
		Assert.notNull(registry, "AppRegistry must not be null");
		Assert.notNull(deployer, "AppDeployer must not be null");
		Assert.notNull(metadataResolver, "MetadataResolver must not be null");
		Assert.notNull(commonProperties, "CommonApplicationProperties must not be null");
		Assert.notNull(defaultStreamService, "StreamDeploymentService must not be null");
		this.repository = repository;
		this.deploymentIdRepository = deploymentIdRepository;
		this.registry = registry;
		this.deployer = deployer;
		this.whitelistProperties = new WhitelistProperties(metadataResolver);
		this.commonApplicationProperties = commonProperties;
		this.defaultStreamService = defaultStreamService;
	}

	/**
	 * Request un-deployment of an existing stream.
	 *
	 * @param name the name of an existing stream (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name") String name) {
		StreamDefinition stream = this.repository.findOne(name);
		if (stream == null) {
			throw new NoSuchStreamDefinitionException(name);
		}
		undeployStream(stream);
	}

	/**
	 * Request un-deployment of all streams.
	 */
	@RequestMapping(value = "", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void undeployAll() {
		for (StreamDefinition stream : this.repository.findAll()) {
			this.undeployStream(stream);
		}
	}

	/**
	 * Request deployment of an existing stream definition.
	 *
	 * @param name the name of an existing stream definition (required)
	 * @param properties the deployment properties for the stream as a comma-delimited list of
	 * key=value pairs
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void deploy(@PathVariable("name") String name,
					   @RequestBody(required = false) Map<String, String> properties) {
		defaultStreamService.deployStream(name, properties);
	}

	/**
	 * Return a new app definition where definition-time and deploy-time properties have been
	 * merged and short form parameters have been expanded to their long form (amongst the
	 * whitelisted supported properties of the app) if applicable.
	 */
	/* default */ AppDefinition mergeAndExpandAppProperties(StreamAppDefinition original, Resource metadataResource,
			Map<String, String> appDeployTimeProperties) {
		Map<String, String> merged = new HashMap<>(original.getProperties());
		merged.putAll(appDeployTimeProperties);
		merged = whitelistProperties.qualifyProperties(merged, metadataResource);

		merged.putIfAbsent(StreamPropertyKeys.METRICS_PROPERTIES, "spring.application.name,spring.application.index,"
				+ "spring.cloud.application.*,spring.cloud.dataflow.*");
		merged.putIfAbsent(METRICS_TRIGGER_INCLUDES, "integration**");

		return new AppDefinition(original.getName(), merged);
	}

	/**
	 * Undeploy the given stream.
	 *
	 * @param stream stream to undeploy
	 */
	private void undeployStream(StreamDefinition stream) {
		for (StreamAppDefinition appDefinition : stream.getAppDefinitions()) {
			String key = DeploymentKey.forStreamAppDefinition(appDefinition);
			String id = this.deploymentIdRepository.findOne(key);
			// if id is null, assume nothing is deployed
			if (id != null) {
				AppStatus status = this.deployer.status(id);
				if (!EnumSet.of(DeploymentState.unknown, DeploymentState.undeployed).contains(status.getState())) {
					this.deployer.undeploy(id);
				}
				this.deploymentIdRepository.delete(key);
			}
		}
	}

}
