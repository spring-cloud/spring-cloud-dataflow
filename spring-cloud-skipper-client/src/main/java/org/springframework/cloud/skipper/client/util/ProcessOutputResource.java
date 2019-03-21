/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.client.util;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.AbstractResource;

/**
 * {@link org.springframework.core.io.Resource} implementation to create an operating
 * system process process and capture its output.
 *
 * @author Mike Heath
 */
public class ProcessOutputResource extends AbstractResource {

	private final ProcessBuilder processBuilder;

	public ProcessOutputResource(String... command) {
		processBuilder = new ProcessBuilder(command);
	}

	@Override
	public String getDescription() {
		return processBuilder.toString();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return processBuilder.start().getInputStream();
	}

	@Override
	public String toString() {
		return getDescription();
	}
}
