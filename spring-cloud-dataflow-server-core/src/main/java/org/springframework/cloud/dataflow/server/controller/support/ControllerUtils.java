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
package org.springframework.cloud.dataflow.server.controller.support;

import org.springframework.cloud.dataflow.rest.resource.DeploymentStateResource;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.util.Assert;

/**
 * @author Gunnar Hillert
 */
public class ControllerUtils {

	/**
	 * Maps the {@link DeploymentState} from the Deployer SPI to a
	 * {@link DeploymentStateResource} which is used by the REST API.
	 *
	 * @param state Must not be null
	 * @return a DeploymentStateResource, never null
	 */
	public static DeploymentStateResource mapState(DeploymentState state) {
		DeploymentStateResource result = DeploymentStateResource.fromKey(state.name());
		Assert.notNull(result, "Trying to display a DeploymentState that should not appear here: " + state);
		return result;
	}
}
