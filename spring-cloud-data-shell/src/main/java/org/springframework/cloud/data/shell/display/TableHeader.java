/*
 * Copyright 2009-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.shell.display;

/**
 * Defines table column headers used by {@link Table}.
 *
 * @see DisplayUtils
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
public class TableHeader {

	private String name;

	private int width = 0;

	private int maxWidth = -1;

	/**
	 * Constructor that initializes the table header with the provided header name and the width of the table header.
	 *
	 * @param name
	 * @param width
	 */
	public TableHeader(String name, int width) {
		super();
		this.width = width;
		this.name = name;
	}

	/**
	 * Constructor that initializes the table header with the provided header name. The width of the table header is
	 * calculated and assigned based on the provided header name.
	 *
	 * @param name
	 */
	public TableHeader(String name) {
		super();
		this.name = name;
		if (name == null) {
			this.width = 0;
		}
		else {
			this.width = name.length();
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getMaxWidth() {
		return maxWidth;
	}

	/**
	 * Defaults to -1 indicating to ignore the property.
	 *
	 * @param maxWidth If negative or zero this property will be ignored.
	 */
	public void setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
	}

	/**
	 * Updated the width for this particular column, but only if the value of the passed-in width is higher than the
	 * value of the pre-existing width.
	 *
	 * @param width
	 */
	public void updateWidth(int width) {
		if (this.width < width) {
			if (this.maxWidth > 0 && this.maxWidth < width) {
				this.width = this.maxWidth;
			}
			else {
				this.width = width;
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maxWidth;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + width;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TableHeader other = (TableHeader) obj;
		if (maxWidth != other.maxWidth) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		}
		else if (!name.equals(other.name)) {
			return false;
		}
		if (width != other.width) {
			return false;
		}
		return true;
	}
}
