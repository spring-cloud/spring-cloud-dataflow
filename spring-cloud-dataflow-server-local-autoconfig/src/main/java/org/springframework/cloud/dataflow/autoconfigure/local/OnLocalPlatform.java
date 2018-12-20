/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.autoconfigure.local;

import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.ConfigurationCondition;

/**
 * When Server in not deployed neither on CF nor on K8s it is considered to be on a Local platform.
 *
 * @author Christian Tzolov
 */
public class OnLocalPlatform extends NoneNestedConditions {

	public OnLocalPlatform() {
		super(ConfigurationCondition.ConfigurationPhase.PARSE_CONFIGURATION);
	}

	@ConditionalOnProperty(name = "kubernetes.service.host")
	static class OnKubernetesPlatform {
	}

	@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
	static class OnCloudFoundryPlatform {
	}
}
