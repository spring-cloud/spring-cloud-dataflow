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

import org.springframework.cloud.dataflow.server.stream.StreamDeployer;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Retrieves logs of deployed stream applications.
 *
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/streams/logs")
public class StreamLogsController {

	private final StreamDeployer streamDeployer;

	/**
	 * Construct Stream logs controller.
	 *
	 * @param streamDeployer the deployer this controller uses to get the logs of
	 * deployed stream apps
	 */
	public StreamLogsController(StreamDeployer streamDeployer) {
		Assert.notNull(streamDeployer, "StreamDeployer must not be null");
		this.streamDeployer = streamDeployer;
	}

	@RequestMapping("{streamName}")
	public String getLog(@PathVariable String streamName) {
		return this.streamDeployer.getLog(streamName);
	}

	@RequestMapping("{streamName}/{appName}")
	public String getLog(@PathVariable String streamName, @PathVariable String appName) {
		return this.streamDeployer.getLog(streamName, appName);
	}
}
