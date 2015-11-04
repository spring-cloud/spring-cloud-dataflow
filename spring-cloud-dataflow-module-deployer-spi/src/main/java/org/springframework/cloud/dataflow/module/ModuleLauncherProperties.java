/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.cloud.dataflow.module;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Module Launcher/Resolver properties that can be passed to the module launcher from the deployer.
 *
 * @author Ilayaperumal Gopinathan
 */
@ConfigurationProperties
public class ModuleLauncherProperties {

	Map<String, String> launcherProperties;

	public void setLauncherProperties(Map<String, String> launcherProperties) {
		this.launcherProperties = launcherProperties;
	}

	public Map<String, String> getLauncherProperties() {
		return this.launcherProperties;
	}
}
