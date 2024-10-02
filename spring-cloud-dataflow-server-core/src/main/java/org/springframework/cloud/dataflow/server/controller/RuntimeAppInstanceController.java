/*
 * Copyright 2018-2022 the original author or authors.
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.cloud.dataflow.server.controller.support.ControllerUtils;
import org.springframework.cloud.dataflow.server.stream.StreamDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.domain.ActuatorPostRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * @author Mark Pollack
 * @author Chris Bono
 */
@RestController
@RequestMapping("/runtime/apps/{appId}/instances")
@ExposesResourceFor(AppInstanceStatusResource.class)
public class RuntimeAppInstanceController {
	private final static Logger logger = LoggerFactory.getLogger(RuntimeAppInstanceController.class);

	private static final Comparator<? super AppInstanceStatus> INSTANCE_SORTER =
			(Comparator<AppInstanceStatus>) (i1, i2) -> i1.getId().compareTo(i2.getId());

	private final StreamDeployer streamDeployer;

	private final RestTemplate restTemplate;

	/**
	 * Construct a new RuntimeAppInstanceController
	 *
	 * @param streamDeployer the stream deployer to use
	 */
	public RuntimeAppInstanceController(StreamDeployer streamDeployer) {
		this.streamDeployer = streamDeployer;
		this.restTemplate = new RestTemplate();
	}

	@RequestMapping
	public PagedModel<AppInstanceStatusResource> list(Pageable pageable, @PathVariable String appId,
													  PagedResourcesAssembler<AppInstanceStatus> assembler) {
		AppStatus status = streamDeployer.getAppStatus(appId);
		if (status.getState().equals(DeploymentState.unknown)) {
			throw new NoSuchAppException(appId);
		}
		List<AppInstanceStatus> appInstanceStatuses = new ArrayList<>(status.getInstances().values());
		Collections.sort(appInstanceStatuses, RuntimeAppInstanceController.INSTANCE_SORTER);
		return assembler.toModel(new PageImpl<>(appInstanceStatuses, pageable,
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
		return new RuntimeAppInstanceController.InstanceAssembler(status).toModel(appInstanceStatus);
	}

	@GetMapping("/{instanceId}/actuator")
	public ResponseEntity<String> getFromActuator(
			@PathVariable String appId,
			@PathVariable String instanceId,
			@RequestParam String endpoint) {
		return ResponseEntity.ok(streamDeployer.getFromActuator(appId, instanceId, endpoint));
	}

	@PostMapping("/{instanceId}/actuator")
	public ResponseEntity<Void> postToActuator(
			@PathVariable String appId,
			@PathVariable String instanceId,
			@RequestBody ActuatorPostRequest actuatorPostRequest) {
		streamDeployer.postToActuator(appId, instanceId, actuatorPostRequest);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/{instanceId}/post")
	public ResponseEntity<String> postToUrl(
			@PathVariable String appId,
			@PathVariable String instanceId,
			@RequestBody String data,
			@RequestHeader HttpHeaders headers) {
		if (logger.isDebugEnabled()) {
			ArgumentSanitizer sanitizer = new ArgumentSanitizer();
			logger.debug("postToUrl:{}:{}:{}:{}", appId, instanceId, data, sanitizer.sanitizeHeaders(headers));
		}
		AppStatus status = streamDeployer.getAppStatus(appId);
		if (status.getState().equals(DeploymentState.unknown)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("appId not found:" + appId);
		}
		AppInstanceStatus appInstanceStatus = status.getInstances().get(instanceId);
		if (appInstanceStatus == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("instanceId not found:" + instanceId);
		}
		String port = appInstanceStatus.getAttributes().get("service.external.port");
		if(!StringUtils.hasText(port)) {
			port = "8080";
		}
		String url = String.format("http://%s:%s", appInstanceStatus.getAttributes().get("pod.ip"), port);
		if (!StringUtils.hasText(url)) {
			return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body("url not found on resource");
		}
		// TODO determine if some headers need to be removed or added
		HttpEntity<String> entity = new HttpEntity<>(data, headers);
		if (logger.isDebugEnabled()) {
			ArgumentSanitizer sanitizer = new ArgumentSanitizer();
			logger.debug("postToUrl:{}:{}:{}:{}:{}", appId, instanceId, url, data, sanitizer.sanitizeHeaders(headers));
		}
		waitForUrl(url, Duration.ofSeconds(30));
		ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
		return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
	}

	private void waitForUrl(String uri, Duration timeout) {
		// Check
		final long waitUntilMillis = System.currentTimeMillis() + timeout.toMillis();
		do {
			try {
				Set<HttpMethod> allowed = this.restTemplate.optionsForAllow(uri);
				if (!CollectionUtils.isEmpty(allowed)) {
					break;
				}
			} catch (Throwable x) {
				logger.trace("waitForUrl:exception:" + x);
				final String message = x.getMessage();
				if(message.contains("UnknownHostException")) {
					logger.trace("waitForUrl:retry:exception:" + x);
					continue;
				}
				if (message.contains("500")) {
					logger.trace("waitForUrl:accepted:exception:" + x);
					break;
				}
			}
			try {
				Thread.sleep(2000L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		} while (waitUntilMillis <= System.currentTimeMillis());
	}

	static class InstanceAssembler
			extends RepresentationModelAssemblerSupport<AppInstanceStatus, AppInstanceStatusResource> {

		private final AppStatus owningApp;

		InstanceAssembler(AppStatus owningApp) {
			super(RuntimeAppInstanceController.class, AppInstanceStatusResource.class);
			this.owningApp = owningApp;
		}

		@Override
		public AppInstanceStatusResource toModel(AppInstanceStatus entity) {
			AppInstanceStatusResource resource = createModelWithId("/" + entity.getId(), entity, owningApp.getDeploymentId());
			if (logger.isDebugEnabled()) {
				ArgumentSanitizer sanitizer = new ArgumentSanitizer();
				logger.debug("toModel:{}:{}", resource.getInstanceId(), sanitizer.sanitizeProperties(resource.getAttributes()));
			}
			if (resource.getAttributes() != null && resource.getAttributes().containsKey("url")) {
				resource.add(linkTo(
						methodOn(RuntimeAppInstanceController.class).postToUrl(
								owningApp.getDeploymentId(),
								resource.getInstanceId(),
								null,
								null)
				).withRel("post"));
				logger.debug("toModel:resource={}", resource.getLinks());
			}
			return resource;
		}

		@Override
		protected AppInstanceStatusResource instantiateModel(AppInstanceStatus entity) {
			return new AppInstanceStatusResource(entity.getId(), ControllerUtils.mapState(entity.getState()).getKey(),
					entity.getAttributes());
		}
	}
}
