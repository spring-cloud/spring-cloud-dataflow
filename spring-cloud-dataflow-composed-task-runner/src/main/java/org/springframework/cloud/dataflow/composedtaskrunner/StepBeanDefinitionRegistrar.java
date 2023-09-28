/*
 * Copyright 2017-2021 the original author or authors.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
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

	private final static Logger log = LoggerFactory.getLogger(StepBeanDefinitionRegistrar.class);

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
		Map<String, TaskAppNodeHolder> taskSuffixMap = getTaskApps(taskParser);
		for (String taskName : taskSuffixMap.keySet()) {
			// handles the possibility that multiple instances of
			// task definition exist in a composed task
			for (int taskSuffix = 0; taskSuffixMap.get(taskName).count >= taskSuffix; taskSuffix++) {
				BeanDefinitionBuilder builder = BeanDefinitionBuilder
						.rootBeanDefinition(ComposedTaskRunnerStepFactory.class);
				builder.addConstructorArgValue(properties);
				builder.addConstructorArgValue(String.format("%s_%s",
						taskName, taskSuffix));
				builder.addConstructorArgValue(taskName.replaceFirst(ctrName + "-", ""));
				builder.addPropertyValue("taskSpecificProps",
						getPropertiesForTask(taskName, properties, taskSuffixMap.get(taskName)));
				String args = getCommandLineArgsForTask(properties.getComposedTaskArguments(), taskName, taskSuffixMap, ctrName);
				builder.addPropertyValue("arguments", args);
				registry.registerBeanDefinition(String.format("%s_%s",
						taskName, taskSuffix), builder.getBeanDefinition());
			}
		}
	}


	private String getCommandLineArgsForTask(String arguments, String taskName, Map<String, TaskAppNodeHolder> taskSuffixMap, String ctrName ) {
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

	private Map<String, String> getPropertiesForTask(String taskName, ComposedTaskProperties properties, TaskAppNodeHolder holder) {
		Map<String, String> taskDeploymentProperties =
				DeploymentPropertiesUtils.parse(properties.getComposedTaskProperties());
		Map<String, String> deploymentProperties = new HashMap<>();
		updateDeploymentProperties(String.format("app.%s.", taskName), taskDeploymentProperties, deploymentProperties);
		updateDeploymentProperties(String.format("deployer.%s.", taskName), taskDeploymentProperties, deploymentProperties);
		String subTaskName = taskName.substring(taskName.indexOf('-') + 1);
		updateVersionDeploymentProperties(taskName, subTaskName, taskDeploymentProperties, deploymentProperties);
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

	private void updateVersionDeploymentProperties(String taskName,String subTaskName, Map<String, String> taskDeploymentProperties,
			Map<String, String> deploymentProperties) {
		String prefix = String.format("version.%s", taskName);
		String key = String.format("version.%s", subTaskName);
		for (Map.Entry<String, String> entry : taskDeploymentProperties.entrySet()) {
			if (entry.getKey().startsWith(prefix)) {
				String realkey = String.format("version%s", entry.getKey().replaceFirst("^" + prefix, ""));
				log.debug("updateVersionDeploymentProperties {} {} {}", key, entry.getValue(), realkey);
				deploymentProperties.put(realkey, entry.getValue());
			}
		}
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.env = environment;
	}

	private ComposedTaskProperties composedTaskProperties() {
		ComposedTaskProperties properties = new ComposedTaskProperties();

		String skipTlsCertificateVerification = getPropertyValue("skip-tls-certificate-verification");
		if (skipTlsCertificateVerification != null) {
			properties.setSkipTlsCertificateVerification(Boolean.parseBoolean(skipTlsCertificateVerification));
		}
		String dataFlowUriString = getPropertyValue("dataflow-server-uri");
		if (dataFlowUriString != null) {
			properties.setDataflowServerUri(URI.create(dataFlowUriString));
		}
		String maxStartWaitTime = getPropertyValue("max-start-wait-time");
		if (maxStartWaitTime != null) {
			properties.setMaxStartWaitTime(Integer.parseInt(maxStartWaitTime));
		}
		String maxWaitTime = getPropertyValue("max-wait-time");
		if (maxWaitTime != null) {
			properties.setMaxWaitTime(Integer.parseInt(maxWaitTime));
		}
		String intervalTimeBetweenChecks = getPropertyValue("interval-time-between-checks");
		if (intervalTimeBetweenChecks != null) {
			properties.setIntervalTimeBetweenChecks(Integer.parseInt(intervalTimeBetweenChecks));
		}
		properties.setGraph(getPropertyValue("graph"));
		properties.setComposedTaskArguments(getPropertyValue("composed-task-arguments"));
		properties.setPlatformName(getPropertyValue("platform-name"));
		properties.setComposedTaskProperties(getPropertyValue("composed-task-properties"));
		properties.setDataflowServerAccessToken(getPropertyValue("dataflow-server-access-token"));
		properties.setDataflowServerPassword(getPropertyValue("dataflow-server-password"));
		properties.setDataflowServerUsername(getPropertyValue("dataflow-server-username"));
		properties.setOauth2ClientCredentialsClientId(getPropertyValue("oauth2-client-credentials-client-id"));
		properties.setOauth2ClientCredentialsClientSecret(getPropertyValue("oauth2-client-credential-client-secret"));
		
		String oauth2ClientCredentialsClientAuthenticationMethodAsString = getPropertyValue("oauth2-client-credential-client-authentication-method");
		if (oauth2ClientCredentialsClientAuthenticationMethodAsString != null) {
			properties.setOauth2ClientCredentialsClientAuthenticationMethod(new ClientAuthenticationMethod(oauth2ClientCredentialsClientAuthenticationMethodAsString));
		}
		
		properties.setOauth2ClientCredentialsScopes(StringUtils.commaDelimitedListToSet(getPropertyValue("oauth2-client-credentials-scopes")));
		return properties;
	}

	/**
	 * @return a {@link Map} of task app name as the key and the number of times it occurs
	 * as the value.
	 */
	private Map<String, TaskAppNodeHolder> getTaskApps(TaskParser taskParser) {
		TaskAppsMapCollector collector = new TaskAppsMapCollector();
		taskParser.parse().accept(collector);
		return collector.getTaskApps();
	}

	private String getPropertyValue(String key) {
		RelaxedNames relaxedNames = new RelaxedNames(key);
		String result = null;
		Iterator<String> iter = relaxedNames.iterator();
		while(iter.hasNext()) {
			String relaxedName = iter.next();
			if (this.env.containsProperty(relaxedName)) {
				result = this.env.getProperty(relaxedName);
				break;
			}
		}

		return result;
	}
	/**
	 * Simple visitor that discovers all the tasks in use in the composed
	 * task definition.
	 */
	static class TaskAppsMapCollector extends TaskVisitor {

		Map<String, TaskAppNodeHolder> taskApps = new HashMap<>();

		@Override
		public void visit(TaskAppNode taskApp) {
			if (taskApps.containsKey(taskApp.getName())) {
				int updatedCount = taskApps.get(taskApp.getName()).count + 1;
				taskApps.put(taskApp.getName(), new TaskAppNodeHolder(taskApp, updatedCount));
			}
			else {
				taskApps.put(taskApp.getName(), new TaskAppNodeHolder(taskApp, 0));
			}
		}

		@Override
		public void visit(TransitionNode transition) {
			if (transition.isTargetApp()) {
				if (taskApps.containsKey(transition.getTargetApp().getName())) {
					int updatedCount = taskApps.get(transition.getTargetApp().getName()).count + 1;
					taskApps.put(transition.getTargetApp().getName(), new TaskAppNodeHolder(transition.getTargetApp(), updatedCount));
				}
				else {
					taskApps.put(transition.getTargetApp().getName(), new TaskAppNodeHolder(transition.getTargetApp(), 0));
				}
			}
		}

		public Map<String, TaskAppNodeHolder> getTaskApps() {
			return taskApps;
		}

	}

	static class TaskAppNodeHolder {
		TaskAppNode taskAppNode;
		int count;
		TaskAppNodeHolder(TaskAppNode taskAppNode, int count) {
			this.taskAppNode = taskAppNode;
			this.count = count;
		}
	}
}
