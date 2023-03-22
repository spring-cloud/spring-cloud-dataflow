/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.skipper.support.yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * Property path representation in {@link YamlBuilder}.
 *
 * @author Kris De Volder
 * @author Janne Valkealahti
 *
 */
class YamlPath {

	public static final YamlPath EMPTY = new YamlPath();
	private final YamlPathSegment[] segments;

	/**
	 * Instantiates a new yaml path.
	 */
	YamlPath() {
		this.segments = new YamlPathSegment[0];
	}

	/**
	 * Instantiates a new yaml path.
	 *
	 * @param segments the segments
	 */
	YamlPath(List<YamlPathSegment> segments) {
		this.segments = segments.toArray(new YamlPathSegment[segments.size()]);
	}

	/**
	 * Instantiates a new yaml path.
	 *
	 * @param segments the segments
	 */
	YamlPath(YamlPathSegment... segments) {
		this.segments = segments;
	}

	/**
	 * Gets a property format of path.
	 *
	 * @return the string
	 */
	public String toPropString() {
		StringBuilder buf = new StringBuilder();
		boolean first = true;
		for (YamlPathSegment s : segments) {
			if (first) {
				buf.append(s.toPropString());
			}
			else {
				buf.append(s.toNavString());
			}
			first = false;
		}
		return buf.toString();
	}

	/**
	 * Gets a navigation format of path. This is similar to
	 * property format but will have leading dot.
	 *
	 * @return the string
	 */
	public String toNavString() {
		StringBuilder buf = new StringBuilder();
		for (YamlPathSegment s : segments) {
			buf.append(s.toNavString());
		}
		return buf.toString();
	}

	/**
	 * Gets the segments.
	 *
	 * @return the segments
	 */
	public YamlPathSegment[] getSegments() {
		return segments;
	}


	/**
	 * Parse a YamlPath from a dotted property name. The segments are obtained
	 * by spliting the name at each dot.
	 *
	 * @param propName the prop name
	 * @return the yaml path
	 */
	public static YamlPath fromProperty(String propName) {
		List<YamlPathSegment> segments = new ArrayList<>();
		String delim = ".[]";
		StringTokenizer tokens = new StringTokenizer(propName, delim, true);
		try {
			while (tokens.hasMoreTokens()) {
				String token = tokens.nextToken(delim);
				if (token.equals(".") || token.equals("]")) {
					// Skip it silently
				}
				else if (token.equals("[")) {
					String bracketed = tokens.nextToken("]");
					if (bracketed.equals("]")) {
						// empty string between []? Makes no sense, so ignore
						// that.
					}
					else {
						try {
							int index = Integer.parseInt(bracketed);
							segments.add(YamlPathSegment.valueAt(index));
						}
						catch (NumberFormatException e) {
							segments.add(YamlPathSegment.valueAt(bracketed));
						}
					}
				}
				else {
					segments.add(YamlPathSegment.valueAt(token));
				}
			}
		}
		catch (NoSuchElementException e) {
			// Ran out of tokens.
		}
		return new YamlPath(segments);
	}

	/**
	 * Create a YamlPath with a single segment (i.e. like 'fromProperty', but
	 * does not parse '.' as segment separators.
	 *
	 * @param name the name
	 * @return the yaml path
	 */
	public static YamlPath fromSimpleProperty(String name) {
		return new YamlPath(YamlPathSegment.valueAt(name));
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("YamlPath(");
		boolean first = true;
		for (YamlPathSegment s : segments) {
			if (!first) {
				buf.append(", ");
			}
			buf.append(s);
			first = false;
		}
		buf.append(")");
		return buf.toString();
	}

	/**
	 * Gets the number of segments.
	 *
	 * @return the number of segments
	 */
	public int size() {
		return segments.length;
	}

	/**
	 * Gets the segment.
	 *
	 * @param segment the segment
	 * @return the segment
	 */
	public YamlPathSegment getSegment(int segment) {
		if (segment >= 0 && segment < segments.length) {
			return segments[segment];
		}
		return null;
	}

	/**
	 * Prepend a segment.
	 *
	 * @param s the s
	 * @return the yaml path
	 */
	public YamlPath prepend(YamlPathSegment s) {
		YamlPathSegment[] newPath = new YamlPathSegment[segments.length + 1];
		newPath[0] = s;
		System.arraycopy(segments, 0, newPath, 1, segments.length);
		return new YamlPath(newPath);
	}

	/**
	 * Append a segment.
	 *
	 * @param s the s
	 * @return the yaml path
	 */
	public YamlPath append(YamlPathSegment s) {
		YamlPathSegment[] newPath = Arrays.copyOf(segments, segments.length + 1);
		newPath[segments.length] = s;
		return new YamlPath(newPath);
	}

	public YamlPath dropFirst(int dropCount) {
		if (dropCount >= size()) {
			return EMPTY;
		}
		if (dropCount == 0) {
			return this;
		}
		YamlPathSegment[] newPath = new YamlPathSegment[segments.length - dropCount];
		for (int i = 0; i < newPath.length; i++) {
			newPath[i] = segments[i + dropCount];
		}
		return new YamlPath(newPath);
	}

	/**
	 * Drop last segment.
	 *
	 * @return the yaml path
	 */
	public YamlPath dropLast() {
		return dropLast(1);
	}

	/**
	 * Drop last segment(s).
	 *
	 * @return the yaml path
	 */
	public YamlPath dropLast(int dropCount) {
		if (dropCount >= size()) {
			return EMPTY;
		}
		if (dropCount == 0) {
			return this;
		}
		YamlPathSegment[] newPath = new YamlPathSegment[segments.length - dropCount];
		for (int i = 0; i < newPath.length; i++) {
			newPath[i] = segments[i];
		}
		return new YamlPath(newPath);
	}

	/**
	 * Checks if has segments.
	 *
	 * @return true, if is empty
	 */
	public boolean isEmpty() {
		return segments.length == 0;
	}

	/**
	 * Returns path with first dropped segment.
	 *
	 * @return the yaml path
	 */
	public YamlPath tail() {
		return dropFirst(1);
	}

	/**
	 * Gets the last segment.
	 *
	 * @return the last segment
	 */
	public YamlPathSegment getLastSegment() {
		if (!isEmpty()) {
			return segments[segments.length - 1];
		}
		return null;
	}
}
