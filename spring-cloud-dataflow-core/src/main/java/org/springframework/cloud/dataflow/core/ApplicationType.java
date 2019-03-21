/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

/**
 * Enumeration of application types.
 *
 * @author Patrick Peralta
 * @author Eric Bottard
 * @author Mark Fisher
 */
public enum ApplicationType {

	/**
	 * A long lived application that is not part of a stream
	 */
	app,

	/**
	 * An application type that appears in a stream, at first position.
	 */
	source,

	/**
	 * An application type that appears in a stream, in middle position.
	 */
	processor,

	/**
	 * An application type that appears in a stream, in last position.
	 */
	sink,

	/**
	 * An application type to execute a short-lived process.
	 */
	task;

}
