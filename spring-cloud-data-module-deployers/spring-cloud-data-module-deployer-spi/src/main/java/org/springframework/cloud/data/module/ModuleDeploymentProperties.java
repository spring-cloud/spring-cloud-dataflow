/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.module;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Deployment properties for a module.
 * @author Mark Fisher
 * @author Gary Russell
 */
public class ModuleDeploymentProperties implements Map<String, String> {

	/**
	 * Default instance of {@code ModuleDeploymentProperties}. This instance
	 * uses default values for all mandatory properties.
	 */
	public static final ModuleDeploymentProperties defaultInstance = new ModuleDeploymentProperties();

	/**
	 * Key for the {@code count} property. Value should be an integer.
	 */
	public static final String COUNT_KEY = "count";

	/**
	 * Key for the {@code criteria} property. Value should be a SpEL expression.
	 */
	public static final String CRITERIA_KEY = "criteria";

	/**
	 * Key for the {@code trackHistory} property. Value should be a boolean.
	 */
	public static final String TRACK_HISTORY_KEY = "trackHistory";

	/**
	 * The underlying map.
	 */
	private final Map<String, String> map = new HashMap<String, String>();

	/**
	 * Create a map to hold module deployment properties. The only initial value will be a count of 1.
	 */
	public ModuleDeploymentProperties() {
		map.put(COUNT_KEY, String.valueOf(1));
	}

	/**
	 * Return the number of container instances this module should be deployed to. A value of 0 indicates that this
	 * module should be deployed to all containers that match the provided target expression. If no target expression
	 * were provided and the count value is 0, this module should be deployed to all containers.
	 * @return number of container instances
	 */
	public int getCount() {
		return parseCount(get(COUNT_KEY));
	}

	/**
	 * Specify the number of container instances this module should be deployed to.
	 * A value of 0 indicates that this module should be deployed to all containers
	 * in the {@code group}. If {@code group} is null and the value is 0, this module
	 * should be deployed to all containers.
	 */
	public ModuleDeploymentProperties setCount(int count) {
		put(COUNT_KEY, String.valueOf(count));
		return this;
	}

	/**
	 * Return the criteria expression to evaluate against container attributes to determine deployment candidates for
	 * this module.
	 * @return criteria expression or {@code null} if no criteria expression was specified.
	 */
	public String getCriteria() {
		return get(CRITERIA_KEY);
	}

	/**
	 * Specify the criteria expression to evaluate against container attributes to determine deployment candidates for
	 * this module.
	 */
	public ModuleDeploymentProperties setCriteria(String criteria) {
		put(CRITERIA_KEY, criteria);
		return this;
	}

	/**
	 * Return the trackHistory property for this module.
	 * @return true if history should be tracked.
	 */
	public boolean getTrackHistory() {
		return Boolean.valueOf(get(TRACK_HISTORY_KEY));
	}

	/**
	 * Specify the trackHistory property for this module.
	 */
	public ModuleDeploymentProperties setTrackHistory(boolean trackHistory) {
		put(TRACK_HISTORY_KEY, String.valueOf(trackHistory));
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return map.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String get(Object key) {
		return map.get(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String put(String key, String value) {
		return map.put(key, value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String remove(Object key) {
		return map.remove(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void putAll(Map<? extends String, ? extends String> m) {
		map.putAll(m);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		this.map.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> keySet() {
		return Collections.unmodifiableSet(map.keySet());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<String> values() {
		return Collections.unmodifiableCollection(map.values());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<java.util.Map.Entry<String, String>> entrySet() {
		return Collections.unmodifiableSet(map.entrySet());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return map.toString();
	}

	/**
	 * Convert a String value to an int. Returns 1 if the String is null.
	 * @param s string to convert
	 * @return int value of String, or 1 if null
	 */
	private int parseCount(String s) {
		return s == null ? 1 : Integer.valueOf(s);
	}

}
