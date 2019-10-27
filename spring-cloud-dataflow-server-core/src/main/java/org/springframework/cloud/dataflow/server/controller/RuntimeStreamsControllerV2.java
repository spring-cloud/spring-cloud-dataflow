/*
 * Copyright 2019 the original author or authors.
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.StreamStatusResource;
import org.springframework.cloud.dataflow.server.stream.StreamDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.data.domain.PageImpl;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Christian Tzolov
 */
@RestController
@RequestMapping("/runtime/streams")
@ExposesResourceFor(StreamStatusResource.class)
public class RuntimeStreamsControllerV2 {

	private static Log logger = LogFactory.getLog(RuntimeStreamsControllerV2.class);

	private final StreamDeployer streamDeployer;

	private final RepresentationModelAssembler<Pair<String, List<AppStatus>>, StreamStatusResource> statusAssembler
			= new RuntimeStreamsControllerV2.Assembler();

	/**
	 * Construct a new runtime apps controller.
	 * @param streamDeployer the deployer this controller will use to get the status of
	 * deployed stream apps
	 */
	public RuntimeStreamsControllerV2(StreamDeployer streamDeployer) {
		Assert.notNull(streamDeployer, "StreamDeployer must not be null");
		this.streamDeployer = streamDeployer;
	}

	/**
	 * @param streamNames comma separated list of streams to retrieve the statuses for
	 */
	@RequestMapping(value = "/{streamNames}", method = RequestMethod.GET)
	public PagedModel<StreamStatusResource> streamStatus(@PathVariable("streamNames") String[] streamNames, Pageable pageable,
			PagedResourcesAssembler<Pair<String, List<AppStatus>>> assembler) {

		List<Pair<String, List<AppStatus>>> streamStatusList = Stream.of(streamNames)
				.map(streamName -> Pair.of(streamName, this.streamDeployer.getStreamStatuses(streamName)))
				.collect(Collectors.toList());

		return assembler.toModel(new PageImpl<>(streamStatusList, pageable, streamStatusList.size()), statusAssembler);
	}

	private static class Assembler extends RepresentationModelAssemblerSupport<Pair<String, List<AppStatus>>, StreamStatusResource> {

		public Assembler() {
			super(RuntimeStreamsControllerV2.class, StreamStatusResource.class);
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
			streamStatusResource.setApplications(new CollectionModel<>(appStatusResources));
			return streamStatusResource;
		}

		@Override
		public StreamStatusResource toModel(Pair<String, List<AppStatus>> entity) {
			return createModelWithId(entity.getFirst(), entity);
		}
	}
}
