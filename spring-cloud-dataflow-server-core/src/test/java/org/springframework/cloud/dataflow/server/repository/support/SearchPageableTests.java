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

import org.junit.jupiter.api.Test;

import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
class SearchPageableTests {

	@Test
	void initializeSearchPageableWithNullPageable() throws Exception {
		try {
			new SearchPageable(null, null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("pageable must not be null");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	void initializeSearchPageableWithNullSearchQuery() throws Exception {
		final PageRequest pageable = PageRequest.of(1, 5);
		try {
			new SearchPageable(pageable, null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("searchQuery must not be empty");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	void initializeSearchPageableWithEmptySearchQuery() throws Exception {
		final PageRequest pageable = PageRequest.of(1, 5);
		try {
			new SearchPageable(pageable, "  ");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("searchQuery must not be empty");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	void addNullColumn() throws Exception {
		final PageRequest pageable = PageRequest.of(1, 5);
		final SearchPageable searchPageable = new SearchPageable(pageable, "findByTaskNameContains query");

		try {
			searchPageable.addColumns(new String[] {});
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("You must specify at least 1 column.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	void addNullColumn2() throws Exception {
		final PageRequest pageable = PageRequest.of(1, 5);
		final SearchPageable searchPageable = new SearchPageable(pageable, "findByTaskNameContains query");

		try {
			searchPageable.addColumns("c1", null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Column names cannot be null or empty.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	void addWhitespaceColumn() throws Exception {
		final PageRequest pageable = PageRequest.of(1, 5);
		final SearchPageable searchPageable = new SearchPageable(pageable, "findByTaskNameContains query");

		try {
			searchPageable.addColumns("     ");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Column names cannot be null or empty.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	void searchPageableGetters() throws Exception {
		final PageRequest pageable = PageRequest.of(1, 5);
		final SearchPageable searchPageable = new SearchPageable(pageable, "findByTaskNameContains query");

		assertThat(searchPageable.getColumns()).isEmpty();
		assertThat(searchPageable.getPageable()).isNotNull();
		assertThat(searchPageable.getSearchQuery()).isEqualTo("findByTaskNameContains query");

		searchPageable.addColumns("c1", "c2");

		assertThat(searchPageable.getColumns()).hasSize(2);
		assertThat(searchPageable.getColumns()).containsExactly("c1", "c2");

	}
}
