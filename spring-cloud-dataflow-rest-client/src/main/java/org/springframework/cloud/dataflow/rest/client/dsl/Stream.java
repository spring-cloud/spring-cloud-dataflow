/*
 * Copyright 2017-2019 the original author or authors.
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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.rest.resource.StreamStatusResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Represents a Stream deployed on DataFlow server. Instances of this class are created using a fluent style builder
 * pattern.  For for instance:
 * <pre>
 *     {@code
 *     Stream stream = Stream.builder(dataflowOperations).definition("time | log").create().deploy();
 *     }
 * </pre>
 *
 * A fluent style that separates source, processor and sink parts can also be used via
 * <pre>
 *     {@code
 *     Stream stream = Stream.builder(dataflowOperations).source("time").sink("log").create().deploy();
 *     }
 * </pre>
 * @author Vinicius Carvalho
 * @author Christian Tzolov
 *
 */
public class Stream implements AutoCloseable {

	private String name;

	private List<StreamApplication> applications;

	private String definition;

	private String description;

	private DataFlowOperations client;

	Stream(String name, List<StreamApplication> applications, String definition, String description,
			DataFlowOperations client) {
		this.name = name;
		this.applications = applications;
		this.definition = definition;
		this.client = client;
		this.description = description;
	}

	/**
	 * @return Stream name
	 */
	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Fluent API method to create a {@link StreamBuilder}.
	 * @param client {@link DataFlowOperations} client instance
	 * @return A fluent style builder to create streams
	 */
	public static StreamBuilder builder(DataFlowOperations client) {
		return new StreamBuilder(client);
	}

	String getDefinition() {
		return this.definition;
	}

	/**
	 * Unforced Stream Update with properties string definition
	 *
	 * @param properties application properties to update.
	 */
	public void update(String properties) {
		try {
			this.update(DeploymentPropertiesUtils.parseDeploymentProperties(properties, null, 0));
		}
		catch (IOException e) {
			throw new RuntimeException("Could not update Stream with property string = " + properties, e);
		}
	}

	/**
	 * Scale up or down the number of application instances.
	 * @param application App in the stream to scale.
	 * @param count Number of instance to scale to.
	 * @param properties optional scale properties.
	 */
	public void scaleApplicationInstances(StreamApplication application, int count, Map<String, String> properties) {
		this.client.streamOperations().scaleApplicationInstances(this.name,
				getAppLabelOrName(application), count, properties);
	}

	private String getAppLabelOrName(StreamApplication application) {
		return StringUtils.hasText(application.getLabel()) ? application.getLabel() : application.getName();
	}

	/**
	 * Unforced Stream Update with properties map
	 *
	 * @param propertiesToUse application properties to update.
	 */
	public void update(Map<String, String> propertiesToUse) {
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(this.name);
		this.client.streamOperations().updateStream(this.name, this.name, packageIdentifier, propertiesToUse, false, null);

		StreamDeploymentResource info = this.client.streamOperations().info(this.name);
		this.name = info.getStreamName();
		this.definition = info.getDslText();
	}

	/**
	 * Rollback the stream to the previous or a specific release version.
	 *
	 * @param streamVersion the version to rollback to. If the version is 0, then rollback to the previous release.
	 *                         The version can not be less than zero.
	 */
	public void rollback(int streamVersion) {
		this.client.streamOperations().rollbackStream(this.name, streamVersion);
		StreamDeploymentResource info = this.client.streamOperations().info(this.name);
		this.name = info.getStreamName();
		this.definition = info.getDslText();
	}

	/**
	 * Undeploy the current {@link Stream}. This method invokes the remote server
	 */
	public void undeploy() {
		this.client.streamOperations().undeploy(this.name);
	}

	/**
	 * Destroy the stream from the server. This method invokes the remote server
	 */
	public void destroy() {
		this.client.streamOperations().destroy(this.name);
	}

	/**
	 * @return list of stream versions and their statuses.
	 */
	public Map<Integer, String> history() {
		Collection<Release> history = this.client.streamOperations().history(this.name);
		return history.stream().collect(Collectors.toMap(
				Release::getVersion,
				r -> r.getInfo().getStatus().getStatusCode().toString().toLowerCase(Locale.ROOT)));
	}

	/**
	 * Get manifest for the given stream deployed via Skipper. Optionally, the version can be
	 * used to retrieve the version for a specific version of the stream.
	 * @param streamVersion the version of the release
	 * @return the manifest for the given stream and version
	 */
	public String manifest(int streamVersion) {
		return this.client.streamOperations().getManifest(this.name, streamVersion);
	}

	/**
	 * @return Status of the deployed stream
	 */
	public String getStatus() {
		StreamDefinitionResource resource = client.streamOperations().getStreamDefinition(this.name);
		return resource.getStatus();
	}

	/**
	* Gets all the applications' logs for this stream
	* @return the log for said stream
	*/
	public String logs(){
		return this.client.streamOperations().streamExecutionLog(this.name);
	}

	/**
	* Get the logs of a specific application from the stream
	* @param app specific application within stream
	* @return the log for said application within said stream
	*/
	public String logs(StreamApplication app) {
		String appDeploymentId = this.appDeploymentId(app.getName());
        return this.client.streamOperations().streamExecutionLog(this.name, appDeploymentId);
    }

	/**
	 * @return Returns a map of the stream applications, associating every application with its applications instances
	 * and their current runtime states: {@code (App -> (AppInstanceId -> AppInstanceState))}.
	 */
	public Map<StreamApplication, Map<String, String>> runtimeApps() {

		StreamStatusResource streamStatus = client.runtimeOperations()
				.streamStatus(this.name).getContent().iterator().next();;
		Map<StreamApplication, Map<String, String>> applications = new HashMap<>();
		for (AppStatusResource appStatusResource : streamStatus.getApplications().getContent()) {
			StreamApplication app = new StreamApplication(appStatusResource.getName());
			app.addProperty("state", appStatusResource.getState());

			Map<String, String> appInstances = new HashMap<>();
			for (AppInstanceStatusResource inst : appStatusResource.getInstances().getContent()) {
				appInstances.put(inst.getInstanceId(), inst.getState());
			}
			applications.put(app, appInstances);
		}

		return applications;
	}

	private String appDeploymentId(String appName) {

		StreamStatusResource streamStatus = client.runtimeOperations()
				.streamStatus(this.name).getContent().iterator().next();

		return streamStatus.getApplications().getContent().stream()
				.filter(asr -> asr.getName().startsWith(appName))
				.map(AppStatusResource::getDeploymentId)
				.findFirst()
				.orElse("none");
	}

	@Override
	public void close() {
		this.destroy();
	}

	public static class StreamNameBuilder extends PropertyBuilder {

		private String name;

		private List<StreamApplication> applications = new LinkedList<>();

		private DataFlowOperations client;

		private String definition;

		private String description;

		StreamNameBuilder(String name, String description, DataFlowOperations client) {
			this.client = client;
			Assert.hasLength(name, "Stream name can't be empty");
			this.name = name;
			this.description = description;
		}

		/**
		 * Appends a {@link StreamApplication} as a source for this stream
		 * @param source - The {@link StreamApplication} being added
		 * @return a {@link SourceBuilder} to continue the building of the Stream
		 */
		public SourceBuilder source(StreamApplication source) {
			Assert.notNull(source, "Source application can't be null");
			return new SourceBuilder(
					source.type(StreamApplication.ApplicationType.SOURCE), this);
		}

		/**
		 * Creates a Stream bypassing the fluent API and just using the provided
		 * definition
		 * @param definition the Stream definition to use
		 * @return A {@link Stream} object
		 */
		public StreamDefinitionBuilder definition(String definition) {
			Assert.hasLength(name, "Stream definition can't be empty");
			this.definition = definition;
			return new StreamDefinitionBuilder(this.name, this.client, this.description, this.definition);
		}

		/**
		 * Sets the description of the stream.
		 * @param description the description text
		 * @return A {@link StreamDescriptionBuilder} object
		 */
		public StreamDescriptionBuilder description(String description) {
			this.description = description;
			return new StreamDescriptionBuilder(this.name, this.description, this.client);
		}

		/**
		 * Creates the Stream. This method will invoke the remote server and create a stream
		 * @return StreamDefinition to allow deploying operations on the created Stream
		 */
		protected StreamDefinition create() {
			return new StreamDefinition(this.name, this.client, this.definition, this.description,
					this.applications);
		}

		protected void addApplication(StreamApplication application) {
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

	public static class StreamDescriptionBuilder extends PropertyBuilder {

		private String name;

		private List<StreamApplication> applications = new LinkedList<>();

		private DataFlowOperations client;

		private String definition;

		private String description;

		StreamDescriptionBuilder(String name, String description, DataFlowOperations client) {
			this.client = client;
			Assert.hasLength(name, "Stream name can't be empty");
			this.name = name;
			this.description = description;
		}

		/**
		 * Appends a {@link StreamApplication} as a source for this stream
		 * @param source - The {@link StreamApplication} being added
		 * @return a {@link SourceBuilder} to continue the building of the Stream
		 */
		public SourceBuilder source(StreamApplication source) {
			Assert.notNull(source, "Source application can't be null");
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
			return new StreamDefinitionBuilder(this.name, this.client, this.description, this.definition);
		}

		/**
		 * Creates the Stream. This method will invoke the remote server and create a stream
		 * @return StreamDefinition to allow deploying operations on the created Stream
		 */
		protected StreamDefinition create() {
			return new StreamDefinition(this.name, this.client, this.definition, this.description,
					this.applications);
		}

		protected void addApplication(StreamApplication application) {
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

		private String description;

		private StreamDefinitionBuilder(String name, DataFlowOperations client, String description,
				String definition) {
			this.name = name;
			this.client = client;
			this.definition = definition;
			this.description = description;
		}

		/**
		 * Creates the Stream. This method will invoke the remote server and create a stream
		 * @return StreamDefinition to allow deploying operations on the created Stream
		 */
		public StreamDefinition create() {
			return new StreamDefinition(this.name, this.client, this.definition, this.description,
					Collections.emptyList());
		}
	}

	public static class SourceBuilder extends BaseBuilder {

		private SourceBuilder(StreamApplication source, PropertyBuilder parent) {
			super(source, parent);
		}

		/**
		 * Appends a {@link StreamApplication} as a processor for this stream
		 * @param processor - The {@link StreamApplication} being added
		 * @return a {@link ProcessorBuilder} to continue the building of the Stream
		 */
		public ProcessorBuilder processor(StreamApplication processor) {
			Assert.notNull(processor, "Processor application can't be null");
			return new ProcessorBuilder(
					processor.type(StreamApplication.ApplicationType.PROCESSOR),
					this.parent);
		}

		/**
		 * Appends a {@link StreamApplication} as a sink for this stream
		 * @param sink - The {@link StreamApplication} being added
		 * @return a {@link SinkBuilder} to continue the building of the Stream
		 */
		public SinkBuilder sink(StreamApplication sink) {
			Assert.notNull(sink, "Sink application can't be null");
			return new SinkBuilder(sink.type(StreamApplication.ApplicationType.SINK),
					this.parent);
		}
	}

	public static class ProcessorBuilder extends BaseBuilder {

		private ProcessorBuilder(StreamApplication application,
				PropertyBuilder parent) {
			super(application, parent);
		}

		/**
		 * Appends a {@link StreamApplication} as a processor for this stream
		 * @param processor - The {@link StreamApplication} being added
		 * @return a {@link ProcessorBuilder} to continue the building of the Stream
		 */
		public ProcessorBuilder processor(StreamApplication processor) {
			Assert.notNull(processor, "Processor application can't be null");
			return new ProcessorBuilder(
					processor.type(StreamApplication.ApplicationType.PROCESSOR),
					this.parent);
		}

		/**
		 * Appends a {@link StreamApplication} as a sink for this stream
		 * @param sink - The {@link StreamApplication} being added
		 * @return a {@link SinkBuilder} to continue the building of the Stream
		 */
		public SinkBuilder sink(StreamApplication sink) {
			Assert.notNull(sink, "Sink application can't be null");
			return new SinkBuilder(sink.type(StreamApplication.ApplicationType.SINK),
					this.parent);
		}

	}

	public static class SinkBuilder extends BaseBuilder {

		private SinkBuilder(StreamApplication application, PropertyBuilder parent) {
			super(application, parent);
		}

		public StreamDefinition create() {
			return this.parent.create();
		}

	}

	static abstract class PropertyBuilder {
		protected abstract StreamDefinition create();

		protected abstract void addApplication(StreamApplication application);
	}

	static abstract class BaseBuilder {

		protected StreamApplication application;

		protected PropertyBuilder parent;

		public BaseBuilder(StreamApplication application, PropertyBuilder parent) {
			this.application = application;
			this.parent = parent;
			this.parent.addApplication(application);
		}
	}
}
