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
package org.springframework.cloud.skipper.domain;

import java.util.Map;

import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;

/**
 * Implementation for {@link AppInstanceStatus} that has deployment state and attributes.
 *
 * @author Mark Pollack
 */
public class AppInstanceStatusImpl implements AppInstanceStatus {

	private String id;

	private DeploymentState state;

	private Map<String, String> attributes;

	public AppInstanceStatusImpl() {
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public DeploymentState getState() {
		return state;
	}

	@Override
	public Map<String, String> getAttributes() {
		return attributes;
	}
}
