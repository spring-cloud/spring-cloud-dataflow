/*
 * Copyright 2015-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.server.controller.support.ControllerUtils;
import org.springframework.cloud.dataflow.server.controller.support.StreamStatus;
import org.springframework.cloud.dataflow.server.stream.StreamDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
@RequestMapping("/runtime")
@ExposesResourceFor(AppStatusResource.class)
public class RuntimeAppsController {

	public static final String ATTRIBUTE_SKIPPER_APPLICATION_NAME = "skipper.application.name";
	public static final String ATTRIBUTE_SKIPPER_RELEASE_VERSION = "skipper.release.version";
	public static final String ATTRIBUTE_GUID = "guid";

	private static Log logger = LogFactory.getLog(RuntimeAppsController.class);

	private static final Comparator<? super AppInstanceStatus> INSTANCE_SORTER = Comparator.comparing(i -> i.getId());

	private final StreamDeployer streamDeployer;

	private final ResourceAssembler<AppStatus, AppStatusResource> statusAssembler = new Assembler();

	/**
	 * Construct a new runtime apps controller.
	 * @param streamDeployer the deployer this controller will use to get the status of
	 * deployed stream apps
	 */
	public RuntimeAppsController(StreamDeployer streamDeployer) {
		Assert.notNull(streamDeployer, "StreamDeployer must not be null");
		this.streamDeployer = streamDeployer;
	}

	@RequestMapping("/streams")
	public List<StreamStatus> streamStatus(@RequestParam("names") String... streamNames) {

		try {
			return Stream.of(streamNames).map(this::toStreamStatus).collect(Collectors.toList());
		}
		catch (Exception e) {
			logger.error("Failed to retrieve any metrics", e);
		}
		return Collections.emptyList();
	}

	private StreamStatus toStreamStatus(String streamName) {
		StreamStatus streamStatus = new StreamStatus();
		streamStatus.setName(streamName);
		streamStatus.setApplications(new ArrayList<>());

		List<AppStatus> appStatuses = this.streamDeployer.getStreamStatuses(streamName);

		if (!CollectionUtils.isEmpty(appStatuses)) {
			for (AppStatus appStatus : appStatuses) {
				try {
					StreamStatus.Application application = new StreamStatus.Application();
					streamStatus.getApplications().add(application);
					application.setInstances(new ArrayList<>());

					for (Map.Entry<String, AppInstanceStatus> instanceEntry : appStatus.getInstances().entrySet()) {
						AppInstanceStatus appInstanceStatus = instanceEntry.getValue();
						StreamStatus.Instance instance = new StreamStatus.Instance();
						application.getInstances().add(instance);

						instance.setGuid(getAppInstanceGuid(appInstanceStatus));
						instance.setState(appInstanceStatus.getState().name());
						instance.setProperties(Collections.emptyMap());

						application.setName(appInstanceStatus.getAttributes().get(ATTRIBUTE_SKIPPER_APPLICATION_NAME));
						streamStatus.setVersion(appInstanceStatus.getAttributes().get(ATTRIBUTE_SKIPPER_RELEASE_VERSION));
					}
				}
				catch (Throwable throwable) {
					logger.warn("Failed to retrieve runtime status for " + appStatus.getDeploymentId(), throwable);
				}
			}
		}
		return streamStatus;
	}

	private String getAppInstanceGuid(AppInstanceStatus instance) {
		return instance.getAttributes().containsKey(ATTRIBUTE_GUID) ?
				instance.getAttributes().get(ATTRIBUTE_GUID) : instance.getId();
	}

	@RequestMapping("/apps")
	public PagedResources<AppStatusResource> list(Pageable pageable, PagedResourcesAssembler<AppStatus> assembler)
			throws ExecutionException, InterruptedException {
		return assembler.toResource(streamDeployer.getAppStatuses(pageable), statusAssembler);
	}

	@RequestMapping("/apps/{id}")
	public AppStatusResource display(@PathVariable String id) {
		AppStatus status = streamDeployer.getAppStatus(id);
		if (status.getState().equals(DeploymentState.unknown)) {
			throw new NoSuchAppException(id);
		}
		return statusAssembler.toResource(status);
	}

	private static class Assembler extends ResourceAssemblerSupport<AppStatus, AppStatusResource> {

		public Assembler() {
			super(RuntimeAppsController.class, AppStatusResource.class);
		}

		@Override
		public AppStatusResource toResource(AppStatus entity) {
			return createResourceWithId(entity.getDeploymentId(), entity);
		}

		@Override
		protected AppStatusResource instantiateResource(AppStatus entity) {
			AppStatusResource resource = new AppStatusResource(entity.getDeploymentId(),
					ControllerUtils.mapState(entity.getState()).getKey());
			List<AppInstanceStatusResource> instanceStatusResources = new ArrayList<>();
			RuntimeAppInstanceController.InstanceAssembler instanceAssembler = new RuntimeAppInstanceController.InstanceAssembler(
					entity);
			List<AppInstanceStatus> instanceStatuses = new ArrayList<>(entity.getInstances().values());
			Collections.sort(instanceStatuses, INSTANCE_SORTER);
			for (AppInstanceStatus appInstanceStatus : instanceStatuses) {
				instanceStatusResources.add(instanceAssembler.toResource(appInstanceStatus));
			}
			resource.setInstances(new Resources<>(instanceStatusResources));
			return resource;
		}
	}

}
