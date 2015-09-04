/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.data.module.deployer.cloudfoundry;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Steve Powell
 */
abstract class AbstractPaginatedResponse<R> {

	private volatile int totalResults;

	private volatile int totalPages;

	private volatile String previousUrl;

	private volatile String nextUrl;

	private volatile List<R> resources;

	public final int getTotalResults() {
		return totalResults;
	}

	@JsonProperty("total_results")
	public final void setTotalResults(int totalResults) {
		this.totalResults = totalResults;
	}

	public final int getTotalPages() {
		return totalPages;
	}

	@JsonProperty("total_pages")
	public final void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}

	public final String getPreviousUrl() {
		return previousUrl;
	}

	@JsonProperty("prev_url")
	public final void setPreviousUrl(String previousUrl) {
		this.previousUrl = previousUrl;
	}

	public final String getNextUrl() {
		return nextUrl;
	}

	@JsonProperty("next_url")
	public final void setNextUrl(String nextUrl) {
		this.nextUrl = nextUrl;
	}

	public final List<R> getResources() {
		return resources;
	}

	public final void setResources(List<R> resources) {
		this.resources = resources;
	}

}
