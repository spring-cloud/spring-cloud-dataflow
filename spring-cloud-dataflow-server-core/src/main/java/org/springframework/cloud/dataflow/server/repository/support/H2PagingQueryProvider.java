/*
 * Copyright 2016-2022 the original author or authors.
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

import org.springframework.data.domain.Pageable;

/**
 * H2 implementation of a {@link PagingQueryProvider} using database specific features.
 *
 * @author Glenn Renfro
 * @author Chris Bono
 */
public class H2PagingQueryProvider extends AbstractSqlPagingQueryProvider {

	@Override
	public String getPageQuery(Pageable pageable) {
		String limitClause = new StringBuilder().append("OFFSET ")
				.append(pageable.getOffset()).append(" ROWS FETCH NEXT ")
				.append(pageable.getPageSize()).append(" ROWS ONLY").toString();
		return SqlPagingQueryUtils.generateLimitJumpToQuery(this, limitClause);
	}
}
