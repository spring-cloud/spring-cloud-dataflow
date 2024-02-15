/*
 * Copyright 2024 the original author or authors.
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


//TODO: Boot3x followup

import org.springframework.batch.item.database.support.AbstractSqlPagingQueryProvider;
import org.springframework.cloud.dataflow.server.repository.support.PagingQueryProvider;

/**
 * This class provides the implementation for methods removed by Spring Batch but are still
 * needed by SCDF.  This comment will be need to be updated prior to release to
 * discuss that it implements extra features needed beyond the {@code SqlPagingQueryProviderFactoryBean}.
 */
public  class DataflowSqlPagingQueryProvider implements DataflowPagingQueryProvider {
	public String generateJumpToItemQuery(int start, int count) {
		throw new UnsupportedOperationException("This method is not yet supported by SCDF.");
	}
}
