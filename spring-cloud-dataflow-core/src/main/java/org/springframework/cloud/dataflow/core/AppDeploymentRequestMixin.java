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
package org.springframework.cloud.dataflow.core;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.core.io.Resource;

/**
 * Jackson Mixin used to create a new {@code AppDeploymentRequest} during deserialization
 *
 * @author Michael Minella
 * @since 2.3
 */
public abstract class AppDeploymentRequestMixin {

	AppDeploymentRequestMixin(@JsonProperty("definition")AppDefinition definition,
			@JsonProperty("resource") Resource resource,
			@JsonProperty("deploymentProperties") Map<String, String> deploymentProperties){}
}
