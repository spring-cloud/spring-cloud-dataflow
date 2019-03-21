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
package org.springframework.cloud.dataflow.rest.client.dsl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Represents an individual Stream Application, encapsulating name, {@link ApplicationType},
 * label, application and deployment properties.
 * @author Vinicius Carvalho
 */
public class StreamApplication {

	/**
	 * Construct a new StreamApplication given the application name.
	 * @param name The name of the application.
	 */
	public StreamApplication(String name) {
		Assert.hasLength(name, "Application name can't be empty");
		this.name = name;
	}

	private final String deployerPrefix = "deployer.%s.";

	private final String name;

	private String label;

	private Map<String, Object> properties = new HashMap<>();

	private Map<String, Object> deploymentProperties = new HashMap<>();

	private ApplicationType type;

	public String getName() {
		return name;
	}

	public StreamApplication label(String label){
		Assert.hasLength(label, "Label can't be empty");
		this.label = label;
		return this;
	}

	public StreamApplication addProperty(String key, Object value){
		this.properties.put(key, value);
		return this;
	}

	public StreamApplication addDeploymentProperty(String key, Object value){
		this.deploymentProperties.put(key, value);
		return this;
	}

	public StreamApplication addProperties(Map<String, Object> properties){
		this.properties.putAll(properties);
		return this;
	}

	public String getLabel() {
		return label;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public StreamApplication type(ApplicationType type){
		this.type = type;
		return this;
	}

	public Map<String, Object> getDeploymentProperties(){
		Map<String, Object> formattedProperties = new HashMap<>();
		String id = StringUtils.isEmpty(label) ? name : label;
		for(Map.Entry<String, Object> entry : deploymentProperties.entrySet()){
			formattedProperties.put(String.format(deployerPrefix, id)+entry.getKey(), entry.getValue());
		}
		return formattedProperties;
	}

	/**
	 * @return Returns the unique identity of an application in a Stream.
	 * This could be name or label: name
	 *
	 */
	public String getIdentity() {
		if(!StringUtils.isEmpty(label)){
			return label+": "+name;
		}else{
			return name;
		}
	}

	public String getDefinition(){
		StringBuilder buffer = new StringBuilder();

		buffer.append(getIdentity());
		for(Map.Entry<String, Object> entry : properties.entrySet()){
			buffer.append(" --"+entry.getKey()+"="+entry.getValue());
		}
		return buffer.toString();
	}

	public ApplicationType getType() {
		return type;
	}

	@Override
	public String toString() {
		return getDefinition();
	}

	public enum ApplicationType {
		SOURCE, PROCESSOR, SINK;
	}
}
