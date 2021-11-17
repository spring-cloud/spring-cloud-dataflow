/*
 * Copyright 2015-2019 the original author or authors.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.server.controller.support.ControllerUtils;
import org.springframework.cloud.dataflow.server.stream.StreamDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes runtime status of deployed apps.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Janne Valkealahti
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * @author Christian Tzolov
 */
@RestController
@RequestMapping("/runtime/apps")
@ExposesResourceFor(AppStatusResource.class)
public class RuntimeAppsController {

	private static final Logger logger = LoggerFactory.getLogger(RuntimeAppsController.class);

	private static final Comparator<? super AppInstanceStatus> INSTANCE_SORTER = Comparator.comparing(i -> i.getId());

	private final StreamDeployer streamDeployer;

	private final RepresentationModelAssembler<AppStatus, AppStatusResource> statusAssembler = new Assembler();

	/**
	 * Construct a new runtime apps controller.
	 * @param streamDeployer the deployer this controller will use to get the status of
	 * deployed stream apps
	 */
	public RuntimeAppsController(StreamDeployer streamDeployer) {
		Assert.notNull(streamDeployer, "StreamDeployer must not be null");
		this.streamDeployer = streamDeployer;
	}

	@RequestMapping
	public PagedModel<AppStatusResource> list(Pageable pageable, PagedResourcesAssembler<AppStatus> assembler) {
		return assembler.toModel(streamDeployer.getAppStatuses(pageable), statusAssembler);
	}

	@RequestMapping("/{appId}")
	public AppStatusResource display(@PathVariable String appId) {
		AppStatus status = streamDeployer.getAppStatus(appId);
		if (status.getState().equals(DeploymentState.unknown)) {
			throw new NoSuchAppException(appId);
		}
		return statusAssembler.toModel(status);
	}

	static class Assembler extends RepresentationModelAssemblerSupport<AppStatus, AppStatusResource> {

		public Assembler() {
			super(RuntimeAppsController.class, AppStatusResource.class);
		}

		@Override
		public AppStatusResource toModel(AppStatus entity) {
			return createModelWithId(entity.getDeploymentId(), entity);
		}

		@Override
		protected AppStatusResource instantiateModel(AppStatus entity) {
			AppStatusResource resource = new AppStatusResource(entity.getDeploymentId(),
					ControllerUtils.mapState(entity.getState()).getKey());
			List<AppInstanceStatusResource> instanceStatusResources = new ArrayList<>();
			RuntimeAppInstanceController.InstanceAssembler instanceAssembler = new RuntimeAppInstanceController.InstanceAssembler(
					entity);
			List<AppInstanceStatus> instanceStatuses = new ArrayList<>(entity.getInstances().values());
			Collections.sort(instanceStatuses, INSTANCE_SORTER);
			for (AppInstanceStatus appInstanceStatus : instanceStatuses) {
				instanceStatusResources.add(instanceAssembler.toModel(appInstanceStatus));
			}
			resource.setInstances(CollectionModel.of(instanceStatusResources));
			return resource;
		}
	}

}
