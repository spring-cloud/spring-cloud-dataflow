/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.config;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.stereotype.Component;

/**
 * REST client component that holds all the available operations for
 * communicating with the Spring Cloud Data Flow REST server.
 *
 * @author Ilayaperumal Gopinathan
 */
@Component
public class DataFlowShell {

	private DataFlowOperations dataFlowOperations;

	public DataFlowOperations getDataFlowOperations() {
		return dataFlowOperations;
	}

	public void setDataFlowOperations(DataFlowOperations dataFlowOperations) {
		this.dataFlowOperations = dataFlowOperations;
	}
}
