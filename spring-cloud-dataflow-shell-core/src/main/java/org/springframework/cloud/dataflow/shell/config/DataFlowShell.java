/*
 * Copyright 2015-2017 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.shell.Target;
import org.springframework.cloud.dataflow.shell.TargetCredentials;
import org.springframework.cloud.dataflow.shell.TargetHolder;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.stereotype.Component;

/**
 * REST client component that holds all the available operations for communicating with
 * the Spring Cloud Data Flow REST server.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
@Component
public class DataFlowShell {

	private DataFlowOperations dataFlowOperations;

	private TargetHolder targetHolder;

	public DataFlowOperations getDataFlowOperations() {
		return dataFlowOperations;
	}

	public void setDataFlowOperations(DataFlowOperations dataFlowOperations) {
		this.dataFlowOperations = dataFlowOperations;
	}

	@Autowired
	public void setTargetHolder(TargetHolder targetHolder) {
		this.targetHolder = targetHolder;
	}

	/**
	 * This method performs various access checks and only if {@code true} is returned,
	 * the shell should show the associated commands. 2 checks are performed:
	 * <p>
	 * <ul>
	 * <li>Does the desired operation indicated by the {@link OpsType} exist?
	 * <li>If the operation exist, does the user have the necessary credentials?
	 * </ul>
	 * <p>
	 * <b>What are valid credentials?</b>
	 * <p>
	 * The way the passed-in {@link RoleType}s are treated depends on the servers security
	 * settings:
	 * <p>
	 * <ul>
	 * <li>{@link Target#isAuthenticationEnabled()} Is authentication enabled?
	 * <li>{@link Target#isAuthorizationEnabled()} If authentication is enabled, is
	 * authorization enabled?
	 * <li>{@link Target#isAuthenticated()} Is the user authenticated?
	 * <li>Only if {@link Target#isAuthorizationEnabled()} and
	 * {@link Target#isAuthenticated()} is {@code true} will the {@link RoleType} checked
	 * against {@link TargetCredentials#getRoles()}.
	 * </ul>
	 *
	 * @param role Can be null.
	 * @param opsType Must not be null.
	 * @return If true the indicated operation is accessible and the user has all
	 * credentials to execute the operation.
	 */
	public boolean hasAccess(RoleType role, OpsType opsType) {
		if (this.dataFlowOperations != null) {
			boolean operationsAvailable = false;
			switch (opsType) {
			case AGGREGATE_COUNTER:
				operationsAvailable = this.dataFlowOperations.aggregateCounterOperations() != null;
				break;
			case APP_REGISTRY:
				operationsAvailable = this.dataFlowOperations.appRegistryOperations() != null;
				break;
			case COMPLETION:
				operationsAvailable = this.dataFlowOperations.completionOperations() != null;
				break;
			case COUNTER:
				operationsAvailable = this.dataFlowOperations.counterOperations() != null;
				break;
			case FIELD_VALUE_COUNTER:
				operationsAvailable = this.dataFlowOperations.fieldValueCounterOperations() != null;
				break;
			case JOB:
				operationsAvailable = this.dataFlowOperations.jobOperations() != null;
				break;
			case RUNTIME:
				operationsAvailable = this.dataFlowOperations.runtimeOperations() != null;
				break;
			case STREAM:
				operationsAvailable = this.dataFlowOperations.streamOperations() != null;
				break;
			case TASK:
				operationsAvailable = this.dataFlowOperations.taskOperations() != null;
				break;
			default:
				throw new IllegalArgumentException("Unsupported OpsType " + opsType);
			}

			boolean passesSecurityChecks = false;
			final Target target = targetHolder.getTarget();

			if (target.isAuthenticationEnabled()) {
				if (target.isAuthenticated()) {
					if (target.isAuthorizationEnabled() && role != null) {
						passesSecurityChecks = target.getTargetCredentials().getRoles().contains(role);
					}
					else {
						passesSecurityChecks = true;
					}
				}
				else {
					passesSecurityChecks = false;
				}
			}
			else {
				passesSecurityChecks = true;
			}
			return operationsAvailable && passesSecurityChecks;
		}
		else {
			return false;
		}
	}
}
