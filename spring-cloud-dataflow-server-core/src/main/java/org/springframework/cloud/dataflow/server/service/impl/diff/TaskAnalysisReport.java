/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.service.impl.diff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.cloud.dataflow.server.service.impl.diff.PropertiesDiff.PropertyChange;

/**
 * Task analysis report contains differences for a task.
 *
 * @author Janne Valkealahti
 *
 */
public class TaskAnalysisReport {

	private TaskManifestDifference taskManifestDifference;

	public TaskAnalysisReport(TaskManifestDifference taskManifestDifference) {
		this.taskManifestDifference = taskManifestDifference;
	}

	public TaskManifestDifference getTaskManifestDifference() {
		return taskManifestDifference;
	}

	public Map<String, String> getMergedDeploymentProperties() {
		Map<String, String> props = new HashMap<>();
		props.putAll(getTaskManifestDifference().getDeploymentPropertiesDifference().getCommon());
		props.putAll(getTaskManifestDifference().getDeploymentPropertiesDifference().getAdded());
		props.putAll(getTaskManifestDifference().getDeploymentPropertiesDifference().getRemoved());
		for (Map.Entry<String, PropertyChange> entry : getTaskManifestDifference().getDeploymentPropertiesDifference()
				.getChanged().entrySet()) {
			props.put(entry.getKey(), entry.getValue().getReplaced());
		}
		return props;
	}

	public List<String> getMergedCommandLineArguments() {
		ArrayList<String> args = new ArrayList<>();
		for (Entry<String, String> entry : getTaskManifestDifference().getCommandLineArgumentPropertiesDifference()
				.getCommon().entrySet()) {
			args.add("--" + entry.getKey() + "=" + entry.getValue());
		}
		for (Entry<String, String> entry : getTaskManifestDifference().getCommandLineArgumentPropertiesDifference()
				.getAdded().entrySet()) {
			args.add("--" + entry.getKey() + "=" + entry.getValue());
		}
		for (Entry<String, String> entry : getTaskManifestDifference().getCommandLineArgumentPropertiesDifference()
				.getRemoved().entrySet()) {
			args.add("--" + entry.getKey() + "=" + entry.getValue());
		}
		for (Map.Entry<String, PropertyChange> entry : getTaskManifestDifference()
				.getCommandLineArgumentPropertiesDifference().getChanged().entrySet()) {
			args.add("--" + entry.getKey() + "=" + entry.getValue().getReplaced());
		}
		return args;
	}

	@Override
	public String toString() {
		return "TaskAnalysisReport [taskManifestDifference=" + taskManifestDifference + "]";
	}

}
