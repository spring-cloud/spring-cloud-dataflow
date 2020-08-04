/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.cloud.dataflow.server.service.impl.diff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.builder.Diff;
import org.apache.commons.lang3.builder.DiffBuilder;
import org.apache.commons.lang3.builder.DiffResult;
import org.apache.commons.lang3.builder.ToStringStyle;

import org.springframework.util.StringUtils;

/**
 * {@code PropertiesDiff} is an implementation to come up with a difference
 * between two String based Maps. This difference gives answer to questions like
 * what has been added, removed and changed between left and right hand properties.
 *
 * @author Janne Valkealahti
 *
 */
public final class PropertiesDiff {

	private Map<String, String> removed = new HashMap<>();
	private Map<String, String> added = new HashMap<>();
	private Map<String, String> common = new HashMap<>();
	private Map<String, PropertyChange> changed = new HashMap<>();
	private Map<String, String> deleted = new HashMap<>();

	private PropertiesDiff(Map<String, String> removed, Map<String, String> added, Map<String, String> common,
			Map<String, PropertyChange> changed, Map<String, String> deleted) {
		this.removed = removed;
		this.added = added;
		this.common = common;
		this.changed = changed;
		this.deleted = deleted;
	}

	/**
	 * Gets the removed properties.
	 *
	 * @return the removed
	 */
	public Map<String, String> getRemoved() {
		return removed;
	}

	/**
	 * Gets the added properties.
	 *
	 * @return the removed
	 */
	public Map<String, String> getAdded() {
		return added;
	}

	/**
	 * Gets the common properties.
	 *
	 * @return the removed
	 */
	public Map<String, String> getCommon() {
		return common;
	}

	/**
	 * Gets the changed properties.
	 *
	 * @return the removed
	 */
	public Map<String, PropertyChange> getChanged() {
		return changed;
	}

	/**
	 * Gets the deleted properties.
	 *
	 * @return the deleted
	 */
	public Map<String, String> getDeleted() {
		return deleted;
	}

	/**
	 * Check if given left and right hand side properties are equal.
	 *
	 * @return true, if successful
	 */
	public boolean areEqual() {
		return removed.isEmpty() && added.isEmpty() && changed.isEmpty() && deleted.isEmpty();
	}

	@Override
	public String toString() {
		return "PropertiesDiff [added=" + added + ", removed=" + removed + ", changed=" + changed + ", common=" + common
				+ ", disappeared=" + deleted + "]";
	}

	/**
	 * Gets a {@link Builder} for {@code PropertiesDiff}.
	 *
	 * @return the builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Class representing a change in a property.
	 */
	public static class PropertyChange {

		private final String original;
		private final String replaced;

		/**
		 * Instantiates a new property change.
		 *
		 * @param original the original
		 * @param replaced the replaced
		 */
		public PropertyChange(String original, String replaced) {
			this.original = original;
			this.replaced = replaced;
		}

		/**
		 * Gets the original property value.
		 *
		 * @return the original property value
		 */
		public String getOriginal() {
			return original;
		}

		/**
		 * Gets the replaced property value.
		 *
		 * @return the replaced property value
		 */
		public String getReplaced() {
			return replaced;
		}

		@Override
		public String toString() {
			return "original=" + original + ", replaced=" + replaced;
		}
	}

	/**
	 * {@code Builder} for {@link PropertiesDiff}.
	 *
	 */
	public static class Builder {

		private final Map<String, String> deleted = new HashMap<>();
		private final Map<String, String> removed = new HashMap<>();
		private final Map<String, String> added = new HashMap<>();
		private final Map<String, String> common = new HashMap<>();
		private final Map<String, PropertyChange> changed = new HashMap<>();
		private final Map<String, String> leftMap = new HashMap<>();
		private final Map<String, String> rightMap = new HashMap<>();

		/**
		 * Adds a left hand side map.
		 *
		 * @param leftMap the left map
		 * @return the builder
		 */
		public Builder left(Map<String, String> leftMap) {
			if (leftMap != null) {
				this.leftMap.putAll(leftMap);
			}
			return this;
		}

		/**
		 * Adds a right hand side map.
		 *
		 * @param rightMap the right map
		 * @return the builder
		 */
		public Builder right(Map<String, String> rightMap) {
			if (rightMap != null) {
				this.rightMap.putAll(rightMap);
			}
			return this;
		}

		/**
		 * Builds the {@link PropertiesDiff}.
		 *
		 * @return the properties diff
		 */
		public PropertiesDiff build() {
			DiffBuilder<?> leftBuilder = new DiffBuilder<>(leftMap, rightMap, ToStringStyle.DEFAULT_STYLE);
			DiffBuilder<?> rightBuilder = new DiffBuilder<>(leftMap, rightMap, ToStringStyle.DEFAULT_STYLE);

			List<String> leftMapRemove = new ArrayList<>();
			for (Entry<String, String> entry : leftMap.entrySet()) {
				if (!rightMap.containsKey(entry.getKey())) {
					removed.put(entry.getKey(), entry.getValue());
					leftMapRemove.add(entry.getKey());
				}
				else {
					leftBuilder.append(entry.getKey(), entry.getValue(), rightMap.get(entry.getKey()));
				}
			}
			for (String keyToRemove : leftMapRemove) {
				leftMap.remove(keyToRemove);
			}
			for (Entry<String, String> entry : rightMap.entrySet()) {
				rightBuilder.append(entry.getKey(), entry.getValue(), leftMap.get(entry.getKey()));
			}

			DiffResult<?> leftResult = leftBuilder.build();
			for (Diff<?> diff : leftResult) {
				leftMap.remove(diff.getFieldName());
				if (diff.getRight() == null) {
					deleted.put(diff.getFieldName(), diff.getLeft().toString());
				}
				else if (!StringUtils.hasText(diff.getRight().toString())) {
					deleted.put(diff.getFieldName(), diff.getLeft().toString());
				}
				else {
					changed.put(diff.getFieldName(),
							new PropertyChange(diff.getLeft().toString(), diff.getRight().toString()));
				}
			}

			common.putAll(leftMap);

			DiffResult<?> rightResult = rightBuilder.build();
			for (Diff<?> diff : rightResult) {
				if (diff.getRight() == null) {
					added.put(diff.getFieldName(), diff.getLeft().toString());
				}
			}

			return new PropertiesDiff(removed, added, common, changed, deleted);
		}
	}
}
