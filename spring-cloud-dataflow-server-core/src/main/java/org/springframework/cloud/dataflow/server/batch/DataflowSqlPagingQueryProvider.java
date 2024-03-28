/*
 * Copyright 2006-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.server.batch;

import org.springframework.batch.item.database.PagingQueryProvider;

/**
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Corneil du Plessis
 */
public interface DataflowSqlPagingQueryProvider extends PagingQueryProvider {

	/**
	 *
	 * Generate the query that will provide the jump to item query.  The itemIndex provided could be in the middle of
	 * the page and together with the page size it will be used to calculate the last index of the preceding page
	 * to be able to retrieve the sort key for this row.
	 *
	 * @param itemIndex the index for the next item to be read
	 * @param pageSize number of rows to read for each page
	 * @return the generated query
	 */
	String generateJumpToItemQuery(int itemIndex, int pageSize);

}
