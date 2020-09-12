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
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Common properties for applications deployed via Spring Cloud Data Flow.
 * Those properties are passed to all Stream or Task applications deployed via Spring Cloud Data Flow.
 * One can pass common Stream application properties directly using the stream properties:
 * <code>
 *  spring.cloud.dataflow.application-properties.stream.[key].[value]
 * </code>
 * Or by providing an external properties file using the spring.cloud.dataflow.application-properties.stream-resource
 * reference. By default the META-INF/application-stream-common-properties-defaults.yml is used.
 *
 * Similarly you can use the spring.cloud.dataflow.application-properties.task.[key].[value] or
 * spring.cloud.dataflow.application-properties.task-resource to define Task common properties. By default the
 * META-INF/application-task-common-properties-defaults.yml properties are applied.
 *
 * Note: the stream-resource and task-resource approach allows to pass property placeholders (e.g. ${}) to the deployed
 * applications.
 * The direct stream.* and task.* approach will resolve such placeholders before they are passed to the deployed
 * applications.
 *
 * @author Marius Bogoevici
 * @author Janne Valkealahti
 * @author Christian Tzolov
 */
@ConfigurationProperties(DataFlowPropertyKeys.PREFIX + "application-properties")
public class CommonApplicationProperties {

	private static final Logger logger = LoggerFactory.getLogger(CommonApplicationProperties.class);

	private Map<String, String> stream = new ConcurrentHashMap<>();
	private Map<String, String> task = new ConcurrentHashMap<>();

	private Resource streamResource = new ClassPathResource("META-INF/application-stream-common-properties-defaults.yml");
	private Resource taskResource = new ClassPathResource("META-INF/application-task-common-properties-defaults.yml");

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

	public Resource getStreamResource() {
		return streamResource;
	}

	public void setStreamResource(Resource streamResource) {
		this.streamResource = streamResource;
	}

	public Resource getTaskResource() {
		return taskResource;
	}

	public void setTaskResource(Resource taskResource) {
		this.taskResource = taskResource;
	}

	public Optional<Properties> getStreamResourceProperties() {
		return resourceToProperties(this.getStreamResource());
	}

	public Optional<Properties> getTaskResourceProperties() {
		return resourceToProperties(this.getTaskResource());
	}

	public static Optional<Properties> resourceToProperties(Resource resource) {
		Properties properties = null;
		if (resource != null && resource.exists()) {
			String extension = FilenameUtils.getExtension(resource.getFilename());
			if (StringUtils.hasText(extension) && (extension.equals("yaml") || extension.equals("yml"))) {
				YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
				yamlPropertiesFactoryBean.setResources(resource);
				yamlPropertiesFactoryBean.afterPropertiesSet();
				properties = yamlPropertiesFactoryBean.getObject();
			}
			else {
				try {
					PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
					propertiesFactoryBean.setLocation(resource);
					propertiesFactoryBean.afterPropertiesSet();
					properties = propertiesFactoryBean.getObject();
				}
				catch (Exception e) {
					logger.warn("Failed to load default app properties from " + resource, e);
				}
			}
		}
		return Optional.ofNullable(properties);
	}
}
