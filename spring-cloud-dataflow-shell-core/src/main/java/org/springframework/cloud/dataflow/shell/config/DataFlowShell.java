/*
 * Copyright 2015-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.config;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.shell.TargetHolder;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.stereotype.Component;

/**
 * REST client component that holds all the available operations for
 * communicating with the Spring Cloud Data Flow REST server.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
@Component
public class DataFlowShell {

	//@Autowired
	private DataFlowOperations dataFlowOperations;

	//@Autowired
	private TargetHolder targetHolder;

	public DataFlowOperations getDataFlowOperations() {
		return dataFlowOperations;
	}

	public void setDataFlowOperations(DataFlowOperations dataFlowOperations) {
		this.dataFlowOperations = dataFlowOperations;
	}

	public boolean hasRole(RoleType create, OpsType opsType) {
		if (this.dataFlowOperations != null) {
			switch(opsType) {
				case AGGREGATE_COUNTER:
					return this.dataFlowOperations.aggregateCounterOperations() != null;
				case APP_REGISTRY:
					return this.dataFlowOperations.appRegistryOperations() != null;
				case COMPLETION:
					return this.dataFlowOperations.completionOperations() != null;
				case COUNTER:
					return this.dataFlowOperations.counterOperations() != null;
				case FIELD_VALUE_COUNTER:
					return this.dataFlowOperations.fieldValueCounterOperations() != null;
				case JOB:
					return this.dataFlowOperations.jobOperations() != null;
				case RUNTIME:
					return this.dataFlowOperations.runtimeOperations() != null;
				case STREAM:
					return this.dataFlowOperations.streamOperations() != null;
				case TASK:
					return this.dataFlowOperations.taskOperations() != null;
				default:
					throw new IllegalArgumentException("Unsupported OpsType");
			}
		}
		else {
			return false;
		}
	}
}
