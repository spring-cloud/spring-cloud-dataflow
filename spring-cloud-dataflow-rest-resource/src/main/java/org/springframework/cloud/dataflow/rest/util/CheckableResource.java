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
package org.springframework.cloud.dataflow.rest.util;

import java.io.IOException;

import org.springframework.core.io.Resource;

/**
 * A CheckableResource is a {@link org.springframework.core.io.Resource} which can be checked for validity.
 * If the check method throws an exception, any information obtained from the Resource should be discarded.
 *
 * @author Glyn Normington
 */
public interface CheckableResource extends Resource {

	// Check the resource for validity and throw an exception unless the resource is valid.
	void check() throws IOException;
}
