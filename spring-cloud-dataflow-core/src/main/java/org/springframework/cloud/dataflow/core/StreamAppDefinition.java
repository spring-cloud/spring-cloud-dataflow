/*
 * Copyright 2016 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.util.Assert;

/**
 * Representation of an application, including properties provided via the DSL definition.
 * This does not include any information required at deployment time (such as the number
 * of app instances).
 *
 * @author Mark Fisher
 */
public class StreamAppDefinition extends DataFlowAppDefinition {

	/**
	 * Name of stream unit this app instance belongs to.
	 */
	private final String streamName;

	/**
	 * Construct a {@code StreamAppDefinition}. This constructor is private; use
	 * {@link StreamAppDefinition.Builder} to create a new instance.
	 *
	 * @param registeredAppName name of app in the registry
	 * @param label label used for app in stream definition
	 * @param streamName name of the stream this app belongs to
	 * @param properties app properties; may be {@code null}
	 */
	private StreamAppDefinition(String registeredAppName, String label, String streamName,
			Map<String, String> properties) {
		super(registeredAppName, label, properties);
		Assert.notNull(streamName, "stream name must not be null");
		this.streamName = streamName;
	}

	/**
	 * Return name of the stream this app instance belongs to.
	 *
	 * @return stream name
	 */
	public String getStreamName() {
		return streamName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((streamName == null) ? 0 : streamName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		StreamAppDefinition other = (StreamAppDefinition) obj;
		if (streamName == null) {
			if (other.streamName != null)
				return false;
		}
		else if (!streamName.equals(other.streamName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "StreamAppDefinition [streamName=" + streamName + ", name=" + this.appDefinition.getName()
				+ ", registeredAppName=" + getRegisteredAppName() + ", properties=" + this.appDefinition.getProperties()
				+ "]";
	}

	/**
	 * Builder object for {@code StreamAppDefinition}. This object is mutable to allow for
	 * flexibility in specifying application fields/properties during parsing.
	 */
	public static class Builder {

		/**
		 * @see AppDefinition#getProperties()
		 */
		private final Map<String, String> properties = new HashMap<String, String>();

		/**
		 * @see StreamAppDefinition#streamName
		 */
		private String streamName;

		/**
		 * @see DataFlowAppDefinition#registeredAppName
		 */
		private String registeredAppName;

		/**
		 * @see AppDefinition#getName()
		 */
		private String label;

		/**
		 * Create a new builder that is initialized with properties of the given
		 * definition. Useful for "mutating" a definition by building a slightly different
		 * copy.
		 *
		 * @param definition the StreamAppDefinition to create a new Builder instance with
		 * stream name, registeredAppName, label, and properties populated
		 * @return a StreamAppDefinition builder
		 */
		public static Builder from(StreamAppDefinition definition) {
			Builder builder = new Builder();
			builder.setStreamName(definition.getStreamName()).setRegisteredAppName(definition.getRegisteredAppName())
					.setLabel(definition.getName()).addProperties(definition.getProperties());
			return builder;
		}

		/**
		 * Return name of stream this app instance belongs to.
		 *
		 * @return stream name
		 */
		public String getStreamName() {
			return streamName;
		}

		/**
		 * Set the stream name this app belongs to.
		 *
		 * @param streamName name
		 * @return this builder object
		 * @see StreamAppDefinition#streamName
		 */
		public Builder setStreamName(String streamName) {
			this.streamName = streamName;
			return this;
		}

		/**
		 * Set the name of the app in the registry.
		 *
		 * @param registeredAppName name of app in registry
		 * @return this builder object
		 * @see DataFlowAppDefinition#registeredAppName
		 */
		public Builder setRegisteredAppName(String registeredAppName) {
			this.registeredAppName = registeredAppName;
			return this;
		}

		/**
		 * Set the app label.
		 *
		 * @param label name of app label
		 * @return this builder object
		 */
		public Builder setLabel(String label) {
			this.label = label;
			return this;
		}

		/**
		 * Set an app property.
		 *
		 * @param name property name
		 * @param value property value
		 * @return this builder object
		 * @see AppDefinition#getProperties()
		 */
		public Builder setProperty(String name, String value) {
			this.properties.put(name, value);
			return this;
		}

		/**
		 * Sets app properties.
		 *
		 * @param properties app properties
		 * @return this builder object
		 * @see AppDefinition#getProperties()
		 */
		public Builder setProperties(Map<String, String> properties) {
			this.properties.clear();
			this.addProperties(properties);
			return this;
		}

		/**
		 * Add the contents of the provided map to the map of app properties.
		 *
		 * @param properties app properties
		 * @return this builder object
		 * @see AppDefinition#getProperties()
		 */
		public Builder addProperties(Map<String, String> properties) {
			this.properties.putAll(properties);
			return this;
		}

		/**
		 * Return a new instance of {@link StreamAppDefinition}.
		 *
		 * @param streamName the name of the stream
		 * @return new instance of {@code StreamAppDefinition}
		 */
		public StreamAppDefinition build(String streamName) {
			if (this.label == null) {
				this.setLabel(this.registeredAppName);
			}
			return new StreamAppDefinition(this.registeredAppName, this.label, streamName, this.properties);
		}
	}

}
