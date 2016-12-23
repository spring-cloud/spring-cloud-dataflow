/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.composed.task;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Glenn Renfro
 */
@ConfigurationProperties
public class ComposedTaskProperties {

	/**
	 * The maximum amount of time in millis that a individual step can run before
	 * the execution of the Composed task is failed.
	 */
	private int maxWaitTime = 30000;

	/**
	 * The amount of time in millis that the ComposedTaskStepExecutionListener
	 * will wait between checks of the database to see if a task has completed.
	 */
	private int intervalTimeBetweenChecks = 500;

	/**
	 * The URI for the dataflow server that will receive task launch requests.
	 */
	private String dataFlowUri = "http://localhost:9393";

	/**
	 * The DSL for the composed task directed graph.
	 */
	private String composedTaskDSL;

	/**
	 * The name of the job.
	 */
	private String jobNamePrefix = "ComposedTaskRunner";

	public int getMaxWaitTime() {
		return maxWaitTime;
	}

	public void setMaxWaitTime(int maxWaitTime) {
		this.maxWaitTime = maxWaitTime;
	}

	public int getIntervalTimeBetweenChecks() {
		return intervalTimeBetweenChecks;
	}

	public void setIntervalTimeBetweenChecks(int intervalTimeBetweenChecks) {
		this.intervalTimeBetweenChecks = intervalTimeBetweenChecks;
	}

	public String getDataFlowUri() {
		return dataFlowUri;
	}

	public void setDataFlowUri(String dataFlowUri) {
		this.dataFlowUri = dataFlowUri;
	}

	public String getComposedTaskDSL() {
		return composedTaskDSL;
	}

	public void setComposedTaskDSL(String composedTaskDSL) {
		this.composedTaskDSL = composedTaskDSL;
	}

	public String getJobNamePrefix() {
		return jobNamePrefix;
	}

	public void setJobNamePrefix(String jobNamePrefix) {
		this.jobNamePrefix = jobNamePrefix;
	}
}
