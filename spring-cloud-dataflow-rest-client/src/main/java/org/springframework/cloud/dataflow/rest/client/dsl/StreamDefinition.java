package org.springframework.cloud.dataflow.rest.client.dsl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.util.StringUtils;

/**
 * Represents a stream that has been created but not yet deployed.
 * 
 * <pre>
 * {
 * 	&#64;code
 * 	StreamDefinition streamBuilder = Stream.builder(dataFlowOperations).name("ticktock").definition("time | log")
 * 			.create();
 * }
 * </pre>
 *
 * @author Vinicius Carvalho
 */
public class StreamDefinition {

	private String name;

	private DataFlowOperations client;

	private String definition;

	private List<StreamApplication> applications = new LinkedList<>();

	public StreamDefinition(String name, DataFlowOperations client, String definition,
			List<StreamApplication> applications) {
		this.name = name;
		this.client = client;
		this.definition = definition;
		this.applications = applications;
		if (StringUtils.isEmpty(definition)) {
			createStreamDefinition();
		}
		this.client.streamOperations().createStream(this.name, this.definition,
				false);
	}

	/**
	 * Destroy the stream from the server. This method invokes the remote server
	 */
	public void destroy() {
		this.client.streamOperations().destroy(this.name);
	}

	/**
	 * Deploy the current stream using the deploymentProperties. This method invokes the
	 * remote server
	 * @param deploymentProperties Map of properties to be used during deployment
	 * @return A deployed {@link Stream}
	 */
	public Stream deploy(Map<String, String> deploymentProperties) {
		Map<String, String> resolvedProperties = resolveDeploymentProperties(
				deploymentProperties);
		client.streamOperations().deploy(this.name, resolvedProperties);
		return new Stream(this.name, this.applications, this.definition, this.client);
	}

	/**
	 * Deploy the current stream without any extra properties
	 * @return A deployed {@link Stream}
	 */
	public Stream deploy() {
		return deploy(null);
	}

	private void createStreamDefinition() {
		StringBuilder buffer = new StringBuilder();
		this.definition = StringUtils.collectionToDelimitedString(applications,
				" | ");
	}

	/**
	 * Concatenates any deployment properties from the apps with a given map used during
	 * {@link StreamDefinition#deploy(Map)}
	 * @return A concatenated map containing all applications deployment properties
	 */
	private Map<String, String> resolveDeploymentProperties(
			Map<String, String> deploymentProperties) {
		Map<String, String> properties = new HashMap<>();
		if (deploymentProperties != null) {
			properties.putAll(deploymentProperties);
		}
		for (StreamApplication app : this.applications) {
			for (Map.Entry<String, Object> entry : app.getDeploymentProperties()
					.entrySet()) {
				properties.put(entry.getKey(), entry.getValue().toString());
			}
		}
		return properties;
	}
}
