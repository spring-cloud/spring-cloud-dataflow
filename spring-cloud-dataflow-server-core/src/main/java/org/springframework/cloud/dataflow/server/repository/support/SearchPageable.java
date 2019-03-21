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
package org.springframework.cloud.dataflow.server.repository.support;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

/**
 * Simple class that is composed of a {@link Pageable} and several properties to
 * encapsulate findByTaskNameContains queries.
 *
 * @author Gunnar Hillert
 */
public class SearchPageable {

	private final Pageable pageable;

	private final String searchQuery;

	private final LinkedHashSet<String> columns = new LinkedHashSet<>(0);

	/**
	 * Initialize a {@link SearchPageable}. Must provide a {@link Pageable} and a
	 * searchQuery. Don't forget to also provide the column names for the findByTaskNameContains using
	 * {@link #addColumns(String...)}.
	 *
	 * @param pageable Must not be null
	 * @param searchQuery Must not be empty
	 */
	public SearchPageable(Pageable pageable, String searchQuery) {
		super();
		Assert.notNull(pageable, "pageable must not be null");
		Assert.hasText(searchQuery, "searchQuery must not be empty");

		this.pageable = pageable;
		this.searchQuery = searchQuery;
	}

	/**
	 * @return Never null
	 */
	public Pageable getPageable() {
		return pageable;
	}

	/**
	 * @return SearchQuery will never be empty.
	 */
	public String getSearchQuery() {
		return searchQuery;
	}

	/**
	 * @return A set with the specified column names
	 */
	public LinkedHashSet<String> getColumns() {
		return columns;
	}

	/**
	 * Allows you to add additional columns.
	 *
	 * @param columns Must not be null or empty
	 */
	public void addColumns(String... columns) {
		Assert.notEmpty(columns, "You must specify at least 1 column.");

		for (String column : columns) {
			Assert.hasText(column, "Column names cannot be null or empty.");
		}

		this.columns.addAll(Arrays.asList(columns));
	}
}
