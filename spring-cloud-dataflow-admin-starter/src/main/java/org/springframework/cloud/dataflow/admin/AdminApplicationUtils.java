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
package org.springframework.cloud.dataflow.admin;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.dataflow.admin.config.AdminProperties;
import org.springframework.util.StringUtils;

/**
 * @author Ilayaperumal Gopinathan
 */
public class AdminApplicationUtils {

	/**
	 * Get map of properties for all the admin properties that are set.
	 *
	 * @param adminProperties
	 * @return map of admin properties that are set.
	 */
	public static Map<String, String> getAdminProperties(AdminProperties adminProperties) {
		Map<String, String> properties = new HashMap<>();
		if (adminProperties.getLocalRepository() != null) {
			properties.put("localRepository", adminProperties.getLocalRepository());
		}
		if (adminProperties.getRemoteRepositories() != null) {
			properties.put("remoteRepositories", StringUtils.arrayToCommaDelimitedString(
					adminProperties.getRemoteRepositories()));
		}
		if (adminProperties.getOffline() != null) {
			properties.put("offline", adminProperties.getOffline());
		}
		return properties;
	}
}
