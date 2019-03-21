/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.job.support;

import org.springframework.batch.core.job.flow.FlowStep;
import org.springframework.batch.core.partition.support.PartitionStep;
import org.springframework.batch.core.step.job.JobStep;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.util.StringUtils;

/**
 * The types of Spring Batch {@link org.springframework.batch.core.Step} implementations that are known to the system.
 *
 * @author Michael Minella
 * @since 1.0
 */
public enum StepType {
	/**
	 * {@link org.springframework.batch.core.step.tasklet.TaskletStep}
	 */
	TASKLET_STEP(TaskletStep.class.getName(), "Tasklet Step"),

	/**
	 * {@link org.springframework.batch.core.job.flow.FlowStep}
	 */
	FLOW_STEP(FlowStep.class.getName(), "Flow Step"),

	/**
	 * {@link org.springframework.batch.core.step.job.JobStep}
	 */
	JOB_STEP(JobStep.class.getName(), "Job Step"),

	/**
	 * {@link org.springframework.batch.core.partition.support.PartitionStep}
	 */
	PARTITION_STEP(PartitionStep.class.getName(), "Partition Step"),

	/**
	 * Used when the type of step is unknown to the system.
	 */
	UNKNOWN("", "");

	private final String className;
	private final String displayName;

	private StepType(String className, String displayName) {
		this.className = className;
		this.displayName = displayName;
	}

	/**
	 * @param className the fully qualified name of the {@link org.springframework.batch.core.Step} implementation
	 * @return the type if known, otherwise {@link #UNKNOWN}
	 */
	public static StepType fromClassName(String className) {
		StepType type = UNKNOWN;

		if (StringUtils.hasText(className)) {
			String name = className.trim();

			for (StepType curType : values()) {
				if (curType.className.equals(name)) {
					type = curType;
					break;
				}
			}
		}

		return type;
	}

	/**
	 * @return the name of the class the current value represents
	 */
	public String getClassName() {
		return this.className;
	}

	/**
	 * @return the value to display in the UI or return via the REST API
	 */
	public String getDisplayName() {
		return this.displayName;
	}
}
