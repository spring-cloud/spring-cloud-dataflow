/*
 * Copyright 2019 the original author or authors.
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

/**
 * Thrown when a user attempts to launch a non composed task while specifying a composed task runner name.
 *
 * @author Glenn Renfro
 */
public class InvalidCTRLaunchRequestException extends RuntimeException {

	public InvalidCTRLaunchRequestException(String taskName) {
		super(String.format("Can not specify a Composed Task Runner Name when launching a non composed task definition (%s)", taskName));
	}
}
