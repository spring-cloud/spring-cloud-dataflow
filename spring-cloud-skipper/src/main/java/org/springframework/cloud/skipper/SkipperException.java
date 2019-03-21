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
package org.springframework.cloud.skipper;

/**
 * Generic exception indicating a problem in components interacting with
 * {@code Skipper}.
 *
 * @author Mark Pollack
 * @author Janne Valkealahti
 */
@SuppressWarnings("serial")
public class SkipperException extends RuntimeException {

	/**
	 * Instantiates a new {@code SkipperException}.
	 *
	 * @param message the message
	 */
	public SkipperException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new {@code SkipperException}.
	 *
	 * @param message the message
	 * @param cause the cause
	 */
	public SkipperException(String message, Throwable cause) {
		super(message, cause);
	}
}
