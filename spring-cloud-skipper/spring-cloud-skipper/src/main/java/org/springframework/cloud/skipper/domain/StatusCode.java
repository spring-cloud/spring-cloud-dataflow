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
package org.springframework.cloud.skipper.domain;

/**
 * Release's status code definition
 *
 * @author Mark Pollack
 */

public enum StatusCode {

	// Status_UNKNOWN indicates that a release is in an uncertain state.
	UNKNOWN,

	// Status_DEPLOYED indicates that the release has been pushed to the platform.
	DEPLOYED,

	// Status_DELETED indicates that a release has been deleted from the platform.
	DELETED,

	// Status_FAILED indicates that the release was not successfully deployed.
	FAILED

}
