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
package org.springframework.cloud.dataflow.server.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.dsl.ParseException;
import org.springframework.cloud.dataflow.core.dsl.StreamNode;
import org.springframework.cloud.dataflow.core.dsl.StreamParser;
import org.springframework.cloud.dataflow.registry.AppRegistryCommon;
import org.springframework.cloud.dataflow.server.DataFlowServerUtil;
import org.springframework.cloud.dataflow.server.controller.StreamAlreadyDeployedException;
import org.springframework.cloud.dataflow.server.controller.StreamAlreadyDeployingException;
import org.springframework.cloud.dataflow.server.controller.support.InvalidStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.support.SearchPageable;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.stream.StreamDeploymentRequest;
import org.springframework.cloud.dataflow.server.support.CannotDetermineApplicationTypeException;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Performs manipulation on application and deployment properties, expanding shorthand
 * application property values, resolving wildcard deployment properties, and creates a
 * {@link StreamDeploymentRequest}.
 * </p>
 * The {@link AbstractStreamService} deployer is agnostic. For deploying streams on
 * Skipper use the {@link DefaultSkipperStreamService} and for the AppDeploy stream
 * deployment use the {@link AppDeployerStreamService}.
 * </p>
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
@Transactional
public abstract class AbstractStreamService implements StreamService {

	private static Log logger = LogFactory.getLog(AbstractStreamService.class);

	/**
	 * The repository this controller will use for stream CRUD operations.
	 */
	protected final StreamDefinitionRepository streamDefinitionRepository;

	/**
	 * The app registry this controller will use to lookup apps.
	 */
	private final AppRegistryCommon appRegistry;

	/**
	 * Constructor for implementations of the {@link StreamService}.
	 * @param streamDefinitionRepository the stream definition repository to use
	 * @param appRegistry the application registry to use
	 */
	public AbstractStreamService(StreamDefinitionRepository streamDefinitionRepository, AppRegistryCommon appRegistry) {
		Assert.notNull(streamDefinitionRepository, "StreamDefinitionRepository must not be null");
		Assert.notNull(appRegistry, "AppRegistryCommon must not be null");
		this.streamDefinitionRepository = streamDefinitionRepository;
		this.appRegistry = appRegistry;
	}

	public StreamDefinition createStream(String streamName, String dsl, boolean deploy) {
		StreamDefinition streamDefinition = createStreamDefinition(streamName, dsl);

		List<String> errorMessages = new ArrayList<>();

		for (StreamAppDefinition streamAppDefinition : streamDefinition.getAppDefinitions()) {
			final String appName = streamAppDefinition.getRegisteredAppName();
			try {
				final ApplicationType appType = DataFlowServerUtil.determineApplicationType(streamAppDefinition);
				if (!appRegistry.appExist(appName, appType)) {
					errorMessages.add(
							String.format("Application name '%s' with type '%s' does not exist in the app registry.",
									appName, appType));
				}
			}
			catch (CannotDetermineApplicationTypeException e) {
				errorMessages.add(String.format("Cannot determine application type for application '%s': %s",
						appName, e.getMessage()));
				continue;
			}
		}

		if (!errorMessages.isEmpty()) {
			throw new InvalidStreamDefinitionException(
					StringUtils.collectionToDelimitedString(errorMessages, "\n"));
		}

		this.streamDefinitionRepository.save(streamDefinition);
		if (deploy) {
			this.deployStream(streamName, new HashMap<>());
		}

		return streamDefinition;
	}

	private StreamDefinition createStreamDefinition(String streamName, String dsl) {
		try {
			return new StreamDefinition(streamName, dsl);
		}
		catch (ParseException ex) {
			throw new InvalidStreamDefinitionException(ex.getMessage());
		}
	}

	@Override
	public void deployStream(String name, Map<String, String> deploymentProperties) {
		if (deploymentProperties == null) {
			deploymentProperties = new HashMap<>();
		}
		StreamDefinition streamDefinition = this.streamDefinitionRepository.findOne(name);
		if (streamDefinition == null) {
			throw new NoSuchStreamDefinitionException(name);
		}

		DeploymentState status = this.doCalculateStreamState(name);

		if (DeploymentState.deployed == status) {
			throw new StreamAlreadyDeployedException(name);
		}
		else if (DeploymentState.deploying == status) {
			throw new StreamAlreadyDeployingException(name);
		}
		doDeployStream(streamDefinition, deploymentProperties);
	}

	protected abstract void doDeployStream(StreamDefinition streamDefinition, Map<String, String> deploymentProperties);

	protected abstract DeploymentState doCalculateStreamState(String name);

	@Override
	public void deleteStream(String name) {
		if (this.streamDefinitionRepository.findOne(name) == null) {
			throw new NoSuchStreamDefinitionException(name);
		}
		this.undeployStream(name);
		this.streamDefinitionRepository.delete(name);
	}

	@Override
	public void deleteAll() {
		for (StreamDefinition streamDefinition : this.streamDefinitionRepository.findAll()) {
			this.undeployStream(streamDefinition.getName());
		}
		this.streamDefinitionRepository.deleteAll();
	}

	@Override
	public List<StreamDefinition> findRelatedStreams(String name, boolean nested) {
		Set<StreamDefinition> relatedDefinitions = new LinkedHashSet<>();
		StreamDefinition currentStreamDefinition = streamDefinitionRepository.findOne(name);
		if (currentStreamDefinition == null) {
			throw new NoSuchStreamDefinitionException(name);
		}
		Iterable<StreamDefinition> definitions = streamDefinitionRepository.findAll();
		List<StreamDefinition> result = new ArrayList<>(findRelatedDefinitions(currentStreamDefinition, definitions,
				relatedDefinitions, nested));
		return result;
	}

	private Set<StreamDefinition> findRelatedDefinitions(StreamDefinition currentStreamDefinition,
			Iterable<StreamDefinition> definitions,
			Set<StreamDefinition> relatedDefinitions,
			boolean nested) {
		relatedDefinitions.add(currentStreamDefinition);
		String currentStreamName = currentStreamDefinition.getName();
		String indexedStreamName = currentStreamName + ".";
		for (StreamDefinition definition : definitions) {
			StreamNode sn = new StreamParser(definition.getName(), definition.getDslText()).parse();
			if (sn.getSourceDestinationNode() != null) {
				String nameComponent = sn.getSourceDestinationNode().getDestinationName();
				if (nameComponent.equals(currentStreamName) || nameComponent.startsWith(indexedStreamName)) {
					boolean isNewEntry = relatedDefinitions.add(definition);
					if (nested && isNewEntry) {
						findRelatedDefinitions(definition, definitions, relatedDefinitions, true);
					}
				}
			}
		}
		return relatedDefinitions;
	}

	@Override
	public Page<StreamDefinition> findDefinitionByNameLike(Pageable pageable, String searchName) {
		Page<StreamDefinition> streamDefinitions;
		if (searchName != null) {
			final SearchPageable searchPageable = new SearchPageable(pageable, searchName);
			searchPageable.addColumns("DEFINITION_NAME", "DEFINITION");
			streamDefinitions = streamDefinitionRepository.findByNameLike(searchPageable);
			long count = streamDefinitions.getContent().size();
			long to = Math.min(count, pageable.getOffset() + pageable.getPageSize());
			streamDefinitions = new PageImpl<>(streamDefinitions.getContent(), pageable,
					streamDefinitions.getTotalElements());
		}
		else {
			streamDefinitions = streamDefinitionRepository.findAll(pageable);
		}
		return streamDefinitions;
	}

	@Override
	public StreamDefinition findOne(String streamDefinitionName) {
		StreamDefinition definition = streamDefinitionRepository.findOne(streamDefinitionName);
		if (definition == null) {
			throw new NoSuchStreamDefinitionException(streamDefinitionName);
		}
		return definition;
	}
}
