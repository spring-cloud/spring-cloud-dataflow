package org.springframework.cloud.dataflow.rest.client.dsl;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Vinicius Carvalho
 */
public class Stream {

	private String name;

	private List<StreamApplication> applications = new LinkedList<>();

	private String definition;

	private DataFlowOperations client;

	private Stream(String name, List<StreamApplication> applications, String definition,
			DataFlowOperations client) {
		this.name = name;
		this.applications = applications;
		this.definition = definition;
		this.client = client;
	}

	public static Builder builder(DataFlowOperations client) {
		return new Builder(client);
	}

	String getDefinition() {
		return this.definition;
	}

	public StreamBuilder undeploy() {
		client.streamOperations().undeploy(this.name);
		return new StreamBuilder(this.name, this.client, this.definition,
				this.applications);
	}

	public void destroy() {
		client.streamOperations().destroy(this.name);
	}

	public String getStatus() {
		StreamDefinitionResource resource = client.streamOperations()
				.getStreamDefinition(this.name);
		return resource.getStatus();
	}

	public static class Builder {

		private DataFlowOperations client;

		private Builder(DataFlowOperations client) {
			this.client = client;
		}

		public StreamNameBuilder name(String name) {
			return new StreamNameBuilder(name, client);
		}

	}

	public static class StreamNameBuilder {

		private String name;

		private List<StreamApplication> applications = new LinkedList<>();

		private DataFlowOperations client;

		private String definition;

		private StreamNameBuilder(String name, DataFlowOperations client) {
			this.client = client;
			Assert.hasLength(name, "Stream name can't be empty");
			this.name = name;
		}

		public SourceBuilder source(StreamApplication source) {
			Assert.notNull(source, "Source application can't be null");
			return new SourceBuilder(
					source.type(StreamApplication.ApplicationType.SOURCE), this);
		}

		public SourceBuilder source(String name) {
			Assert.isTrue(StringUtils.hasLength(name),
					"Source application can't be null");
			StreamApplication source = new StreamApplication(name);
			return new SourceBuilder(
					source.type(StreamApplication.ApplicationType.SOURCE), this);
		}

		/**
		 * Creates a Stream bypassing the fluent API and just using the provided
		 * definition
		 * @param definiton the Stream definition to use
		 * @return A {@link Stream} object
		 */
		public StreamDefinitionBuilder definition(String definiton) {
			Assert.hasLength(name, "Stream definition can't be empty");
			this.definition = definiton;
			return new StreamDefinitionBuilder(this.name, this.client, this.definition);
		}

		private StreamBuilder create() {
			return new StreamBuilder(this.name, this.client, this.definition,
					this.applications);
		}

		private void addApplication(StreamApplication application) {
			if (contains(application)) {
				throw new IllegalStateException(
						"There's already an application with the same definition in this stream");
			}
			this.applications.add(application);
		}

		private boolean contains(StreamApplication application) {
			for (StreamApplication app : this.applications) {
				if (app.getType().equals(application.getType())
						&& app.getIdentity().equals(application.getIdentity())) {
					return true;
				}
			}
			return false;
		}
	}

	public static class StreamDefinitionBuilder {

		private String name;

		private DataFlowOperations client;

		private String definition;

		private StreamDefinitionBuilder(String name, DataFlowOperations client,
				String definition) {
			this.name = name;
			this.client = client;
			this.definition = definition;
		}

		public StreamBuilder create() {
			return new StreamBuilder(this.name, this.client, this.definition,
					Collections.emptyList());
		}
	}

	public static class StreamBuilder {

		private String name;

		private DataFlowOperations client;

		private String definition;

		private List<StreamApplication> applications = new LinkedList<>();

		private StreamBuilder(String name, DataFlowOperations client, String definition,
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

		public void destroy() {
			this.client.streamOperations().destroy(this.name);
		}

		public Stream deploy(Map<String, String> deploymentProperties) {
			Map<String, String> resolvedProperties = resolveDeploymentProperties(
					deploymentProperties);
			client.streamOperations().deploy(this.name, resolvedProperties);
			return new Stream(this.name, this.applications, this.definition, this.client);
		}

		public Stream deploy() {
			return deploy(null);
		}

		private void createStreamDefinition() {
			StringBuilder buffer = new StringBuilder();
			this.definition = StringUtils.collectionToDelimitedString(applications,
					" | ");
		}

		/**
		 * Concatenates any deployment properties from the apps with a given map used
		 * during {@link StreamBuilder#deploy(Map)}
		 * @return
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

	public static class SourceBuilder extends BaseBuilder {

		private SourceBuilder(StreamApplication source, StreamNameBuilder parent) {
			super(source, parent);
		}

		public ProcessorBuilder processor(StreamApplication processor) {
			Assert.notNull(processor, "Processor application can't be null");
			return new ProcessorBuilder(
					processor.type(StreamApplication.ApplicationType.PROCESSOR),
					this.parent);
		}

		public ProcessorBuilder processor(String name) {
			Assert.hasLength(name, "Processor name can't be empty");
			StreamApplication processor = new StreamApplication(name);
			return new ProcessorBuilder(
					processor.type(StreamApplication.ApplicationType.PROCESSOR),
					this.parent);
		}

		public SinkBuilder sink(StreamApplication sink) {
			Assert.notNull(sink, "Sink application can't be null");
			return new SinkBuilder(sink.type(StreamApplication.ApplicationType.SINK),
					this.parent);
		}

		public SinkBuilder sink(String name) {
			Assert.hasLength(name, "Sink name can't be empty");
			StreamApplication sink = new StreamApplication(name);
			return new SinkBuilder(sink.type(StreamApplication.ApplicationType.SINK),
					this.parent);
		}

	}

	public static class ProcessorBuilder extends BaseBuilder {

		private ProcessorBuilder(StreamApplication application,
				StreamNameBuilder parent) {
			super(application, parent);
		}

		public ProcessorBuilder processor(String name) {
			Assert.hasLength(name, "Processor name can't be empty");
			StreamApplication processor = new StreamApplication(name);
			return new ProcessorBuilder(
					processor.type(StreamApplication.ApplicationType.PROCESSOR),
					this.parent);
		}

		public ProcessorBuilder processor(StreamApplication processor) {
			Assert.notNull(processor, "Processor application can't be null");
			return new ProcessorBuilder(
					processor.type(StreamApplication.ApplicationType.PROCESSOR),
					this.parent);
		}

		public SinkBuilder sink(StreamApplication sink) {
			Assert.notNull(sink, "Sink application can't be null");
			return new SinkBuilder(sink.type(StreamApplication.ApplicationType.SINK),
					this.parent);
		}

		public SinkBuilder sink(String name) {
			Assert.hasLength(name, "Sink name can't be empty");
			StreamApplication sink = new StreamApplication(name);
			return new SinkBuilder(sink.type(StreamApplication.ApplicationType.SINK),
					this.parent);
		}
	}

	public static class SinkBuilder extends BaseBuilder {

		private SinkBuilder(StreamApplication application, StreamNameBuilder parent) {
			super(application, parent);
		}

		public StreamBuilder create() {
			return this.parent.create();
		}

	}

	static abstract class BaseBuilder {

		protected StreamApplication application;

		protected StreamNameBuilder parent;

		public BaseBuilder(StreamApplication application, StreamNameBuilder parent) {
			this.application = application;
			this.parent = parent;
			this.parent.addApplication(application);
		}

	}

}
