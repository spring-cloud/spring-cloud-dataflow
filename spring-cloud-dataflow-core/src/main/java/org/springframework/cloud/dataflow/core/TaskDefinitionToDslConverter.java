/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Reverse engineers a {@link TaskDefinition} into a semantically equivalent DSL text representation.
 *
 * @author Ilayaperumal Gopinathan
 */
public class TaskDefinitionToDslConverter {

	private final static List<String> dataFlowAddedProperties = Arrays.asList(TaskDefinition.SPRING_CLOUD_TASK_NAME);

	/**
	 * Reverse engineers a {@link TaskDefinition} into a semantically equivalent DSL text representation.
	 * @param taskDefinition task definition to be converted into DSL
	 * @return the textual DSL representation of the task
	 */
	public String toDsl(TaskDefinition taskDefinition) {

		if (StringUtils.hasText(taskDefinition.getDslText())) {
			TaskParser taskParser = new TaskParser("__dummy", taskDefinition.getDslText(), true, true);
			Assert.isTrue(!taskParser.parse().isComposed(),
					"The TaskDefinitionToDslConverter doesn't support Composed Tasks!");
		}

		StringBuilder dslBuilder = new StringBuilder();
		Map<String, String> properties = taskDefinition.getProperties();
		dslBuilder.append(taskDefinition.getRegisteredAppName());
		for (String propertyName : properties.keySet()) {
			if (!dataFlowAddedProperties.contains(propertyName)) {
				String propertyValue = StringEscapeUtils.unescapeHtml(properties.get(propertyName));
				dslBuilder.append(" --").append(propertyName).append("=").append(
						DefinitionUtils.escapeNewlines(DefinitionUtils.autoQuotes(propertyValue)));
			}
		}
		return dslBuilder.toString();
	}
}
