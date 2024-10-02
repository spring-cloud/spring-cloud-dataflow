/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.core.StreamRuntimePropertyKeys;
import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.StreamStatusResource;
import org.springframework.cloud.dataflow.server.stream.StreamDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/runtime/streams")
@ExposesResourceFor(StreamStatusResource.class)
public class RuntimeStreamsController {

	private static Log logger = LogFactory.getLog(RuntimeStreamsController.class);

	private final StreamDeployer streamDeployer;

	private final RepresentationModelAssembler<Pair<String, List<AppStatus>>, StreamStatusResource> statusAssembler
			= new RuntimeStreamsController.Assembler();

	/**
	 * Construct a new runtime apps controller.
	 * @param streamDeployer the deployer this controller will use to get the status of
	 * deployed stream apps
	 */
	public RuntimeStreamsController(StreamDeployer streamDeployer) {
		Assert.notNull(streamDeployer, "StreamDeployer must not be null");
		this.streamDeployer = streamDeployer;
	}

	/**
	 * @param names The names of streams to include in result.
	 * @param pageable the page
	 * @param assembler the resource assembler
	 *
	 * @return a paged model for stream statuses
	 */
	@GetMapping
	public PagedModel<StreamStatusResource> status(
			@RequestParam(required = false) String[] names,
			Pageable pageable,
			PagedResourcesAssembler<Pair<String, List<AppStatus>>> assembler
	) {
		List<String> streamNames = (names!= null) ? Arrays.asList(names): new ArrayList<>();
		if (streamNames.isEmpty()) {
			streamNames = this.streamDeployer.getStreams();
		}
		return assembler.toModel(new PageImpl<>(getStreamStatusList(getPagedStreamNames(pageable, streamNames)),
				pageable, streamNames.size()), statusAssembler);
	}


	private String[] getPagedStreamNames(Pageable pageable, List<String> streamNames) {
		PageRequest page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
		int start = (int) page.getOffset();
		List<String> streamsSubList = new ArrayList<>();
		if ((streamNames.size() > start)) {
			int end = (start + page.getPageSize()) > streamNames.size() ? streamNames.size() : (start + page.getPageSize());
			streamsSubList = streamNames.subList(start, end);
		}
		return new PageImpl<>(streamsSubList,
				PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()), streamNames.size())
				.getContent().toArray(new String[0]);
	}


	private List<Pair<String, List<AppStatus>>> getStreamStatusList(String[] streamNames) {
		Map<String, List<AppStatus>> streamStatuses = this.streamDeployer.getStreamStatuses(streamNames);
		List<Pair<String, List<AppStatus>>> streamStatusList = new ArrayList<>();
		streamStatuses.entrySet().forEach(entry -> {
			streamStatusList.add(Pair.of(entry.getKey(), entry.getValue()));
		});
		return streamStatusList;
	}

	/**
	 * @param streamNames comma separated list of streams to retrieve the statuses for
	 * @param pageable Pageable required on subsequent calls.
	 * @param assembler The resource assembler for the results.
	 * @return paged results.
	 */
	@GetMapping("/{streamNames}")
	public PagedModel<StreamStatusResource> streamStatus(@PathVariable String[] streamNames, Pageable pageable,
			PagedResourcesAssembler<Pair<String, List<AppStatus>>> assembler) {
		return assembler.toModel(new PageImpl<>(getStreamStatusList(getPagedStreamNames(pageable, Arrays.asList(streamNames))),
				pageable, streamNames.length), statusAssembler);
	}

	private String getAppInstanceGuid(AppInstanceStatus instance) {
		return instance.getAttributes().containsKey(StreamRuntimePropertyKeys.ATTRIBUTE_GUID) ?
				instance.getAttributes().get(StreamRuntimePropertyKeys.ATTRIBUTE_GUID) : instance.getId();
	}

	private static class Assembler extends RepresentationModelAssemblerSupport<Pair<String, List<AppStatus>>, StreamStatusResource> {

		public Assembler() {
			super(RuntimeStreamsController.class, StreamStatusResource.class);
		}

		@Override
		protected StreamStatusResource instantiateModel(Pair<String, List<AppStatus>> entity) {
			return this.toStreamStatus(entity.getFirst(), entity.getSecond());
		}

		private StreamStatusResource toStreamStatus(String streamName, List<AppStatus> appStatuses) {
			StreamStatusResource streamStatusResource = new StreamStatusResource();
			streamStatusResource.setName(streamName);

			List<AppStatusResource> appStatusResources = new ArrayList<>();

			if (!CollectionUtils.isEmpty(appStatuses)) {
				for (AppStatus appStatus : appStatuses) {
					try {
						appStatusResources.add(new RuntimeAppsController.Assembler().toModel(appStatus));
					}
					catch (Throwable throwable) {
						logger.warn("Failed to retrieve runtime status for " + appStatus.getDeploymentId(), throwable);
					}
				}
			}
			streamStatusResource.setApplications(CollectionModel.of(appStatusResources));
			return streamStatusResource;
		}

		@Override
		public StreamStatusResource toModel(Pair<String, List<AppStatus>> entity) {
			return createModelWithId(entity.getFirst(), entity);
		}
	}
}
