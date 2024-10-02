/*
 * Copyright 2015-2016 the original author or authors.
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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * Representation of a defined stream. A stream consists of an ordered list of apps used
 * to process data. Each app may contain configuration options provided at the time of
 * stream creation.
 * <p>
 * This stream definition does not include any deployment or runtime configuration for a
 * stream.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @see StreamAppDefinition
 */
@Entity
@Table(name = "STREAM_DEFINITIONS")
public class StreamDefinition {

	/**
	 * Name of stream.
	 */
	@Id
	@Column(name = "DEFINITION_NAME")
	private String name;

	/**
	 * DSL definition for stream.
	 */
	@Column(name = "DEFINITION")
	@Lob
	private String dslText;

	/**
	 * Original DSL definition for stream.
	 */
	@Column(name = "ORIGINAL_DEFINITION")
	@Lob
	private String originalDslText;

	/**
	 * Custom description of the stream definition. (Optional)
	 */
	@Column(name = "DESCRIPTION")
	private String description;

	public StreamDefinition() {
	}

	/**
	 * Construct a {@code StreamDefinition}.
	 *
	 * @param name name of stream
	 * @param dslText DSL definition for stream
	 */
	public StreamDefinition(String name, String dslText) {
		Assert.hasText(name, "name is required");
		Assert.hasText(dslText, "dslText is required");
		this.name = name;
		this.dslText = dslText;
		this.originalDslText = dslText;
	}

	/**
	 * Construct a {@code StreamDefinition}.
	 *
	 * @param name name of stream
	 * @param dslText DSL definition for stream
	 * @param originalDslText the original DSL definition for stream
	 */
	public StreamDefinition(String name, String dslText, String originalDslText) {
		this(name, dslText);
		this.originalDslText = originalDslText;
	}

	public StreamDefinition(String name, String dslText, String originalDslText, String description) {
		this(name, dslText);
		this.originalDslText = originalDslText;
		this.description = description;
	}

	/**
	 * Return the name of this stream.
	 *
	 * @return stream name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the DSL definition for this stream.
	 *
	 * @return the stream definition DSL
	 */
	public String getDslText() {
		return dslText;
	}

	/**
	 * Return the Original DSL definition for this stream.
	 *
	 * @return the original stream definition DSL
	 */
	public String getOriginalDslText() {
		return this.originalDslText;
	}

	/**
	 * Return the custom definition of the stream definition.
	 *
	 * @return stream definition description string
	 */
	public String getDescription() {
		return description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dslText == null) ? 0 : dslText.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StreamDefinition other = (StreamDefinition) obj;
		if (dslText == null) {
			if (other.dslText != null)
				return false;
		}
		else if (!dslText.equals(other.dslText))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("name", this.name).append("definition", this.dslText).toString();
	}

}
