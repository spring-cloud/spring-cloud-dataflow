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

import java.util.Map;
import javax.sql.DataSource;

import org.springframework.data.domain.Pageable;

/**
 * Interface defining the functionality to be provided for generating paging queries.
 * @author Glenn Renfro
 */
public interface PagingQueryProvider {

	/**
	 * Initialize the query provider using the provided {@link DataSource} if necessary.
	 *
	 * @param dataSource DataSource to use for any initialization
	 */
	void init(DataSource dataSource) throws Exception;

	/**
	 * The number of parameters that are declared in the query
	 * @return number of parameters
	 */
	int getParameterCount();

	/**
	 * Indicate whether the generated queries use named parameter syntax.
	 *
	 * @return true if named parameter syntax is used
	 */
	boolean isUsingNamedParameters();

	/**
	 * The sort keys.  A Map of the columns that make up the key and a Boolean indicating ascending or descending
	 * (ascending = true).
	 *
	 * @return the sort keys used to order the query
	 */
	Map<String, Order> getSortKeys();

	/**
	 *
	 * Generate the query that will provide the jump to item query.
	 *
	 * @param pageable the coordinates to pull the next page from the datasource
	 * @return the generated query
	 */
	String getPageQuery(Pageable pageable);
}
