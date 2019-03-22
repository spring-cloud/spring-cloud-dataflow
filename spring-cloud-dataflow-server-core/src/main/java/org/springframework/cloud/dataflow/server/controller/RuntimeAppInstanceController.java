/*
 * Copyright 2018 the original author or authors.
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

import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.server.controller.support.ControllerUtils;
import org.springframework.cloud.dataflow.server.stream.StreamDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Mark Pollack
 */
@RestController
@RequestMapping("/runtime/apps/{appId}/instances")
@ExposesResourceFor(AppInstanceStatusResource.class)
public class RuntimeAppInstanceController {

	private static final Comparator<? super AppInstanceStatus> INSTANCE_SORTER = new Comparator<AppInstanceStatus>() {
		@Override
		public int compare(AppInstanceStatus i1, AppInstanceStatus i2) {
			return i1.getId().compareTo(i2.getId());
		}
	};

	private final StreamDeployer streamDeployer;

	/**
	 * Construct a new RuntimeAppInstanceController
	 * @param streamDeployer the stream deployer to use
	 */
	public RuntimeAppInstanceController(StreamDeployer streamDeployer) {
		this.streamDeployer = streamDeployer;
	}

	@RequestMapping
	public PagedResources<AppInstanceStatusResource> list(Pageable pageable, @PathVariable String appId,
			PagedResourcesAssembler<AppInstanceStatus> assembler) {
		AppStatus status = streamDeployer.getAppStatus(appId);
		if (status.getState().equals(DeploymentState.unknown)) {
			throw new NoSuchAppException(appId);
		}
		List<AppInstanceStatus> appInstanceStatuses = new ArrayList<>(status.getInstances().values());
		Collections.sort(appInstanceStatuses, RuntimeAppInstanceController.INSTANCE_SORTER);
		return assembler.toResource(new PageImpl<>(appInstanceStatuses, pageable,
				appInstanceStatuses.size()), new RuntimeAppInstanceController.InstanceAssembler(status));
	}

	@RequestMapping("/{instanceId}")
	public AppInstanceStatusResource display(@PathVariable String appId, @PathVariable String instanceId) {
		AppStatus status = streamDeployer.getAppStatus(appId);
		if (status.getState().equals(DeploymentState.unknown)) {
			throw new NoSuchAppException(appId);
		}
		AppInstanceStatus appInstanceStatus = status.getInstances().get(instanceId);
		if (appInstanceStatus == null) {
			throw new NoSuchAppInstanceException(instanceId);
		}
		return new RuntimeAppInstanceController.InstanceAssembler(status).toResource(appInstanceStatus);
	}

	static class InstanceAssembler
			extends ResourceAssemblerSupport<AppInstanceStatus, AppInstanceStatusResource> {

		private final AppStatus owningApp;

		InstanceAssembler(AppStatus owningApp) {
			super(RuntimeAppInstanceController.class, AppInstanceStatusResource.class);
			this.owningApp = owningApp;
		}

		@Override
		public AppInstanceStatusResource toResource(AppInstanceStatus entity) {
			return createResourceWithId("/" + entity.getId(), entity, owningApp.getDeploymentId());
		}

		@Override
		protected AppInstanceStatusResource instantiateResource(AppInstanceStatus entity) {
			return new AppInstanceStatusResource(entity.getId(), ControllerUtils.mapState(entity.getState()).getKey(),
					entity.getAttributes());
		}
	}
}
