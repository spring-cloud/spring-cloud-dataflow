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

import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.tasklet.CallableTaskletAdapter;
import org.springframework.batch.core.step.tasklet.MethodInvokingTaskletAdapter;
import org.springframework.batch.core.step.tasklet.SystemCommandTasklet;
import org.springframework.util.StringUtils;

/**
 * Types of {@link org.springframework.batch.core.step.tasklet.Tasklet} implementations
 * known by Spring Task.  These
 * include tasklets provided by Spring Batch and Spring for Apache Hadoop.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @since 1.0
 */
public enum TaskletType {

	/**
	 * {@link org.springframework.batch.core.step.item.ChunkOrientedTasklet}
	 */
	CHUNK_ORIENTED_TASKLET(ChunkOrientedTasklet.class.getName(), "Chunk Oriented Step"),
	/**
	 * {@link org.springframework.batch.core.step.tasklet.SystemCommandTasklet}
	 */
	SYSTEM_COMMAND_TASKLET(SystemCommandTasklet.class.getName(), "System Command Step"),
	/**
	 * {@link org.springframework.batch.core.step.tasklet.CallableTaskletAdapter}
	 */
	CALLABLE_TASKLET_ADAPTER(CallableTaskletAdapter.class.getName(), "Callable Tasklet Adapter Step"),
	/**
	 * {@link org.springframework.batch.core.step.tasklet.MethodInvokingTaskletAdapter}
	 */
	METHOD_INVOKING_TASKLET_ADAPTER(MethodInvokingTaskletAdapter.class.getName(), "Method Invoking Tasklet Adapter Step"),
	/**
	 * Used when the type of tasklet is unknown to the system
	 */
	UNKNOWN("", "");

	//TODO: Add Hadoop Types

	private final String className;
	private final String displayName;

	private TaskletType(String className, String displayName) {
		this.className = className;
		this.displayName = displayName;
	}

	/**
	 * @param className the fully qualified name of the {@link org.springframework.batch.core.step.tasklet.Tasklet}
	 *                     implementation
	 * @return the type if known, otherwise {@link #UNKNOWN}
	 */
	public static TaskletType fromClassName(String className) {
		TaskletType type = UNKNOWN;

		if(StringUtils.hasText(className)) {
			String name = className.trim();

			for (TaskletType curType : values()) {
				if(curType.className.equals(name)) {
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
