/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config.apps;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;

/**
 * Common properties for applications deployed via Spring Cloud Data Flow.
 *
 * @author Marius Bogoevici
 * @author Janne Valkealahti
 */
@ConfigurationProperties(DataFlowPropertyKeys.PREFIX + "application-properties")
public class CommonApplicationProperties {

	private Map<String, String> stream = new ConcurrentHashMap<>();
	private Map<String, String> task = new ConcurrentHashMap<>();

	public Map<String, String> getStream() {
		return this.stream;
	}

	public void setStream(Map<String, String> stream) {
		this.stream = stream;
	}

	public Map<String, String> getTask() {
		return this.task;
	}

	public void setTask(Map<String, String> task) {
		this.task = task;
	}
}
