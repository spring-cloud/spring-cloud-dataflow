/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.skipper.server.deployer;

import org.springframework.util.Assert;

/**
 * @author Ilayaperumal Gopinathan
 */
public class ResourceUtils {

	static String getResourceLocation(String specResource, String specVersion) {
		Assert.hasText(specResource, "Spec resource must not be empty");
		if (specVersion != null) {
			if ((specResource.startsWith("maven") || specResource.startsWith("docker"))) {
				if (specResource.endsWith(":" + specVersion)) {
					// May still be consume 1.0 M1 based package artifacts
					return specResource;
				}
				else {
					return String.format("%s:%s", specResource, specVersion);
				}
			}
			// Assume the resource extension is JAR when it is neither maven nor docker.
			else {
				return String.format("%s-%s.jar", specResource, specVersion);
			}
		}
		else {
			return specResource;
		}
	}
}
