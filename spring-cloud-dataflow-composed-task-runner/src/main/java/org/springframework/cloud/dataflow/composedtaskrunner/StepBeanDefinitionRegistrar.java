/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.util.cli.CommandLineUtils;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.core.dsl.TaskAppNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.core.dsl.TaskVisitor;
import org.springframework.cloud.dataflow.core.dsl.TransitionNode;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

/**
 * Creates the Steps necessary to execute the directed graph of a Composed
 * Task.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
public class StepBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar,
		EnvironmentAware {

	private Environment env;

	private boolean firstAdd;

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			BeanDefinitionRegistry registry) {
		ComposedTaskProperties properties = composedTaskProperties();
		String ctrName = this.env.getProperty("spring.cloud.task.name");
		if(ctrName == null) {
			throw  new IllegalStateException("spring.cloud.task.name property must have a value.");
		}
		TaskParser taskParser = new TaskParser("bean-registration",
				properties.getGraph(), false, true);
		Map<String, Integer> taskSuffixMap = getTaskApps(taskParser);
		for (String taskName : taskSuffixMap.keySet()) {
			//handles the possibility that multiple instances of
			// task definition exist in a composed task
			for (int taskSuffix = 0; taskSuffixMap.get(taskName) >= taskSuffix; taskSuffix++) {
				BeanDefinitionBuilder builder = BeanDefinitionBuilder
						.rootBeanDefinition(ComposedTaskRunnerStepFactory.class);
				builder.addConstructorArgValue(properties);
				builder.addConstructorArgValue(String.format("%s_%s",
						taskName, taskSuffix));
				builder.addPropertyValue("taskSpecificProps",
						getPropertiesForTask(taskName, properties));
				String args = getCommandLineArgsForTask(properties.getComposedTaskArguments(), taskName, taskSuffixMap, ctrName);
				builder.addPropertyValue("arguments", args);
				registry.registerBeanDefinition(String.format("%s_%s",
						taskName, taskSuffix), builder.getBeanDefinition());
			}
		}
	}
	private String getCommandLineArgsForTask(String arguments, String taskName, Map<String, Integer> taskSuffixMap, String ctrName ) {
		String result = "";
		if(!StringUtils.hasText(arguments)) {
			return arguments;
		}
		if(arguments.startsWith("\"") && arguments.endsWith("\"")) {
			arguments = arguments.substring(1, arguments.length() - 1);
		}
		arguments = arguments.replace('\n', ' ').replace('\t', ' ');
		this.firstAdd = true;
		try {
			String[] args = CommandLineUtils.translateCommandline(arguments);
			String taskNamePrefix = taskName + ".";
			String taskNameNonIdentify = "--" + taskNamePrefix;
			for(String commandLineArg : Arrays.asList(args)) {
				String userPrefix = getPrefix(commandLineArg);
				String commandLineArgPrefix = ctrName + "-" + userPrefix;
				String commandLineArgToken = commandLineArgPrefix + ".";
				if(commandLineArgToken.equals(taskNameNonIdentify) || commandLineArgToken.equals(taskNamePrefix)) {
					result = addBlankToCommandLineArgs(result);
					if(commandLineArg.startsWith(userPrefix)) {
						result = result.concat(commandLineArg.substring(userPrefix.length() + 1));
					}
					else {
						result = result + "--" + commandLineArg.substring(userPrefix.length() + 3);
					}
					continue;
				}
				if(!taskSuffixMap.containsKey(commandLineArgPrefix)) {
					result = addBlankToCommandLineArgs(result);
					if(commandLineArg.contains(" ")) {
						commandLineArg = commandLineArg.substring(0, commandLineArg.indexOf("=")) +
								"=\"" + commandLineArg.substring(commandLineArg.indexOf("=") + 1 )+ "\"";
					}
					result = result.concat(commandLineArg);
				}
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to extract command line args for task " + taskName, e);
		}

		return result;
	}

	private String addBlankToCommandLineArgs(String commandArgs) {
		String result = commandArgs;
		if(firstAdd) {
			this.firstAdd = false;
		}
		else {
			result = result.concat(" ");
		}
		return result;
	}

	private String getPrefix(String commandLineArg) {
		String commandLineArgPrefix = (!commandLineArg.contains("="))? commandLineArg : commandLineArg.substring(0, commandLineArg.indexOf("="));
		int indexOfSeparator = commandLineArgPrefix.indexOf(".");
		if(indexOfSeparator > -1) {
			commandLineArgPrefix = commandLineArg.substring(0, indexOfSeparator);
		}
		if(commandLineArgPrefix.startsWith("--")) {
			commandLineArgPrefix = commandLineArgPrefix.substring(2);
		}
		return commandLineArgPrefix;
	}

	private Map<String, String> getPropertiesForTask(String taskName, ComposedTaskProperties properties) {
		Map<String, String> taskDeploymentProperties =
				DeploymentPropertiesUtils.parse(properties.getComposedTaskProperties());
		Map<String, String> deploymentProperties = new HashMap<>();
		updateDeploymentProperties(String.format("app.%s.", taskName), taskDeploymentProperties, deploymentProperties);
		updateDeploymentProperties(String.format("deployer.%s.", taskName), taskDeploymentProperties, deploymentProperties);
		return deploymentProperties;
	}

	private void updateDeploymentProperties(String prefix, Map<String, String> taskDeploymentProperties,
			Map<String, String> deploymentProperties) {
		for (Map.Entry<String, String> entry : taskDeploymentProperties.entrySet()) {
			if (entry.getKey().startsWith(prefix)) {
				deploymentProperties.put(entry.getKey()
						.substring(prefix.length()), entry.getValue());
			}
		}
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.env = environment;
	}

	private ComposedTaskProperties composedTaskProperties() {
		ComposedTaskProperties properties = new ComposedTaskProperties();
		String dataFlowUriString = this.env.getProperty("dataflow-server-uri");
		String maxWaitTime = this.env.getProperty("max-wait-time");
		String intervalTimeBetweenChecks =
				this.env.getProperty("interval-time-between-checks");
		properties.setGraph(this.env.getProperty("graph"));
		properties.setComposedTaskArguments(
				this.env.getProperty("composed-task-arguments"));
		properties.setPlatformName(this.env.getProperty("platform-name"));
		properties.setComposedTaskProperties(this.env.getProperty("composed-task-properties"));

		if (maxWaitTime != null) {
			properties.setMaxWaitTime(Integer.valueOf(maxWaitTime));
		}
		if (intervalTimeBetweenChecks != null) {
			properties.setIntervalTimeBetweenChecks(Integer.valueOf(
					intervalTimeBetweenChecks));
		}
		if (dataFlowUriString != null) {
			try {
				properties.setDataflowServerUri(new URI(dataFlowUriString));
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException("Invalid Data Flow URI");
			}
		}
		return properties;
	}

	/**
	 * @return a {@link Map} of task app name as the key and the number of times it occurs
	 * as the value.
	 */
	private Map<String, Integer> getTaskApps(TaskParser taskParser) {
		TaskAppsMapCollector collector = new TaskAppsMapCollector();
		taskParser.parse().accept(collector);
		return collector.getTaskApps();
	}

	/**
	 * Simple visitor that discovers all the tasks in use in the composed
	 * task definition.
	 */
	static class TaskAppsMapCollector extends TaskVisitor {

		Map<String, Integer> taskApps = new HashMap<>();

		@Override
		public void visit(TaskAppNode taskApp) {
			if (taskApps.containsKey(taskApp.getName())) {
				Integer updatedCount = taskApps.get(taskApp.getName()) + 1;
				taskApps.put(taskApp.getName(), updatedCount);
			}
			else {
				taskApps.put(taskApp.getName(), 0);
			}
		}

		@Override
		public void visit(TransitionNode transition) {
			if (transition.isTargetApp()) {
				if (taskApps.containsKey(transition.getTargetApp())) {
					Integer updatedCount = taskApps.get(transition.getTargetApp()) + 1;
					taskApps.put(transition.getTargetApp().getName(), updatedCount);
				}
				else {
					taskApps.put(transition.getTargetApp().getName(), 0);
				}
			}
		}

		public Map<String, Integer> getTaskApps() {
			return taskApps;
		}

	}

}
