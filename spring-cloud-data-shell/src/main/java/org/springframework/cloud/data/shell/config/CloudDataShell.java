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

package org.springframework.cloud.data.shell.config;

import org.springframework.cloud.data.rest.client.CloudDataOperations;
import org.springframework.stereotype.Component;

/**
 * REST client component that holds all the available operations for
 * communicating with the spring cloud data REST server.
 *
 * @author Ilayaperumal Gopinathan
 */
@Component
public class CloudDataShell {

	private CloudDataOperations CloudDataOperations;

	public CloudDataOperations getCloudDataOperations() {
		return CloudDataOperations;
	}

	public void setCloudDataOperations(CloudDataOperations cloudDataOperations) {
		this.CloudDataOperations = cloudDataOperations;
	}
}
