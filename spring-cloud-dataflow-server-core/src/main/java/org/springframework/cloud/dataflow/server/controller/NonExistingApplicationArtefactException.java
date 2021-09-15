/*
 * Copyright 2021-2021 the original author or authors.
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
package org.springframework.cloud.dataflow.server.controller;

import java.util.List;

/**
 * @author Christian Tzolov
 */
public class NonExistingApplicationArtefactException extends IllegalStateException {

	private static final long serialVersionUID = 1L;
	private final String uris;

	public NonExistingApplicationArtefactException(String...uris) {
		this.uris = String.join(",", uris);
	}

	public NonExistingApplicationArtefactException(List<String> uris) {
		this.uris = String.join(",", uris);
	}

	@Override
	public String getMessage() {
		return String.format("Non existing application artifacts: %s'", uris);
	}
}
