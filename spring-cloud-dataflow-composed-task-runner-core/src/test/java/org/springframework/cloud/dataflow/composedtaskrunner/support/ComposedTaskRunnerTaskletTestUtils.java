/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner.support;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.cloud.dataflow.composedtaskrunner.TaskLauncherTasklet;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

/**
 * @author Glenn Renfro
 */
public class ComposedTaskRunnerTaskletTestUtils {

	public static TaskLauncherTasklet getTaskletLauncherTasklet(ApplicationContext context, String beanName) {
		TaskletStep taskletStep = context.getBean(beanName, TaskletStep.class);
		return (TaskLauncherTasklet) taskletStep.getTasklet();
	}

	public static List<String> getTaskletArgumentsViaReflection(TaskLauncherTasklet taskLauncherTasklet) throws IllegalAccessException {
		final Field argumentsField = ReflectionUtils.findField(TaskLauncherTasklet.class, "arguments");
		ReflectionUtils.makeAccessible(argumentsField);
		return (List<String>)argumentsField.get(taskLauncherTasklet);
	}

	public static Map<String, String> getTaskletPropertiesViaReflection(TaskLauncherTasklet taskLauncherTasklet) throws IllegalAccessException {
		final Field propertiesField = ReflectionUtils.findField(TaskLauncherTasklet.class, "properties");
		ReflectionUtils.makeAccessible(propertiesField);
		return (Map<String, String>)propertiesField.get(taskLauncherTasklet);
	}
}
