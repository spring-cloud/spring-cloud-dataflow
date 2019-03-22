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

package org.springframework.cloud.dataflow.server.service.impl.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the tag's information returned from docker for a specific image.
 *
 * @author Glenn Renfro
 */
public class DockerResult {

	private int count;

	private String next;

	private List<DockerTag> results = new ArrayList<>();

	protected  DockerResult() {
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public List<DockerTag> getResults() {
		return results;
	}

	public void setResults(List<DockerTag> results) {
		this.results = results;
	}

	public String getNext() {
		return next;
	}

	public void setNext(String next) {
		this.next = next;
	}
}
