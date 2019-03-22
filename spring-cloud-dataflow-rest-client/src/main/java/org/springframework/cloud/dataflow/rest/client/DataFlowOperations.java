/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

/**
 * Interface the REST clients implement to interact with spring-cloud-dataflow REST API.
 *
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Eric Bottard
 */
public interface DataFlowOperations {

	/**
	 * @return Stream related operations.
	 */
	StreamOperations streamOperations();

	/**
	 * @return Task related operations.
	 */
	TaskOperations taskOperations();

	/**
	 * @return Job related operations.
	 */
	JobOperations jobOperations();

	/**
	 * @return Application registry related operations.
	 */
	AppRegistryOperations appRegistryOperations();

	/**
	 * @return DSL Completion related operations.
	 */
	CompletionOperations completionOperations();

	/**
	 * @return Runtime related operations.
	 */
	RuntimeOperations runtimeOperations();

	/**
	 * @return "About" related operations.
	 */
	AboutOperations aboutOperation();

	/**
	 * @return Scheduler related operations.
	 */
	SchedulerOperations schedulerOperations();
}
