/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.kubernetes;

import org.springframework.cloud.dataflow.server.config.CloudProfileProvider;
import org.springframework.core.env.Environment;

/**
 * @author Mark Pollack
 */
public class KubernetesCloudProfileProvider implements CloudProfileProvider {

	public final static String PROFILE = "kubernetes";

	@Override
	public boolean isCloudPlatform(Environment environment) {
		return environment.containsProperty("kubernetes_service_host");
	}

	@Override
	public String getCloudProfile() {
		return PROFILE;
	}
}
